using System;
using System.Collections.Generic;
using System.Diagnostics;
using System.Threading;
using CSCore.CoreAudioAPI;

// Enumerating owns the enumerator and one session lease at a time. The iterator
// finally paths also run for continue, break, return and exceptions in callers.
internal static class OwnedAudioSessions
{
    private static long lastError = -10000;
    private static readonly Stopwatch clock = Stopwatch.StartNew();
    internal static IEnumerable<Lease> Enumerate(AudioSessionEnumerator sessions)
    {
        try
        {
            foreach (AudioSessionControl session in sessions)
                using (var lease = new Lease(session)) yield return lease;
        }
        finally { Release(sessions); }
    }
    internal static void Release(IDisposable resource)
    {
        try { resource?.Dispose(); }
        catch (Exception error)
        {
            var now = clock.ElapsedMilliseconds;
            var previous = Interlocked.Read(ref lastError);
            if (now - previous >= 10000 && Interlocked.CompareExchange(ref lastError, now, previous) == previous)
                Console.Error.WriteLine("Audio resource release failed: " + error.GetType().Name);
        }
    }
    internal sealed class Lease : IDisposable
    {
        private readonly List<IDisposable> resources = new List<IDisposable>(4);
        private Process process;
        private bool processQueried;
        internal AudioSessionControl Session { get; }
        internal Lease(AudioSessionControl session) { Session = Own(session); }
        internal T Own<T>(T resource) where T : class, IDisposable
        {
            if (resource != null) resources.Add(resource);
            return resource;
        }
        internal Process GetProcess(AudioSessionControl2 control)
        {
            if (!processQueried) { processQueried = true; process = Own(control?.Process); }
            return process;
        }
        public void Dispose()
        {
            for (var index = resources.Count - 1; index >= 0; index--) Release(resources[index]);
            resources.Clear();
            process = null;
        }
    }
}
