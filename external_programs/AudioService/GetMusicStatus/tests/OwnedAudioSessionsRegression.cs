using System;
using System.Collections;
using System.Collections.Generic;
using System.Diagnostics;
using CSCore.CoreAudioAPI;
class OwnedAudioSessionsRegression
{
    static void Check(bool condition) { if (!condition) throw new Exception("Resource parity failed"); }
    static void Main()
    {
        for (int round = 0; round < 10000; round++)
        {
            var session = new AudioSessionControl();
            var control = new AudioSessionControl2();
            var meter = new Counting();
            var enumerator = new AudioSessionEnumerator(session);
            try
            {
                foreach (var lease in OwnedAudioSessions.Enumerate(enumerator))
                {
                    lease.Own(control);
                    Check(ReferenceEquals(lease.GetProcess(control), lease.GetProcess(control)));
                    lease.Own(meter);
                    if (round % 3 == 0) continue;
                    if (round % 3 == 1) throw new InvalidOperationException("query failed");
                    break;
                }
            }
            catch (InvalidOperationException) { }
            Check(session.Count == 1 && control.Count == 1 && meter.Count == 1 && enumerator.Count == 1);
            Check(control.ProcessQueries == 1);
        }
        var resources = new AudioSessionEnumerator(new AudioSessionControl());
        foreach (var lease in OwnedAudioSessions.Enumerate(resources))
        {
            lease.Own(new Counting { Throws = true });
            lease.Dispose(); lease.Dispose();
        }
        Check(resources.Count == 1);
        for (int i = 0; i < 1000; i++)
        {
            var service = new TestMusicService();
            service.Init(); service.Dispose(); service.Dispose();
            Check(service.Cleaned == 1);
        }
        Console.WriteLine("PASS: 10000 success/continue/break/error iterations release all resources; process queried once; disposal failure isolated/idempotent.");
        Console.WriteLine("PASS: 1000 music service worker cancellation, cleanup and repeated disposal cycles.");
    }
}
class Counting : IDisposable { public int Count; public bool Throws; public virtual void Dispose() { Count++; if (Throws) throw new Exception("release failed"); } }
class TestMusicService : MusicService
{
    public int Cleaned;
    public override void Init()
    {
        OwnCleanup(() => Cleaned++);
        StartWorker(async () => await System.Threading.Tasks.Task.Delay(-1, StopToken));
    }
    public override string GetMusicStatus(AudioSessionManager2 manager) { return "None"; }
}
namespace CSCore.CoreAudioAPI
{
    public class AudioSessionManager2 { }
    class AudioSessionControl : Counting { }
    class AudioSessionControl2 : Counting
    {
        public int ProcessQueries;
        public Process Process { get { ProcessQueries++; return Process.GetCurrentProcess(); } }
    }
    class AudioSessionEnumerator : Counting, IEnumerable<AudioSessionControl>
    {
        readonly AudioSessionControl session;
        public AudioSessionEnumerator(AudioSessionControl session) { this.session=session; }
        public IEnumerator<AudioSessionControl> GetEnumerator() { yield return session; }
        IEnumerator IEnumerable.GetEnumerator() { return GetEnumerator(); }
    }
}
