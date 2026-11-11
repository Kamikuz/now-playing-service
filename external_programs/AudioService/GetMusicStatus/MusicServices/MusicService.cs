using CSCore.CoreAudioAPI;

public abstract class MusicService : System.IDisposable
{
    private readonly System.Collections.Generic.List<System.Action> cleanup = new System.Collections.Generic.List<System.Action>();
    private readonly System.Threading.CancellationTokenSource stopping = new System.Threading.CancellationTokenSource();
    private readonly System.Collections.Generic.List<System.Threading.Tasks.Task> workers = new System.Collections.Generic.List<System.Threading.Tasks.Task>();
    private int disposed;
    protected System.Threading.CancellationToken StopToken => stopping.Token;
    protected void StartWorker(System.Func<System.Threading.Tasks.Task> action)
    {
        workers.Add(System.Threading.Tasks.Task.Run(async () => {
            try { await action(); } catch (System.OperationCanceledException) { }
            catch (System.Exception error) { System.Console.Error.WriteLine("Music worker failed: " + error.GetType().Name); }
        }));
    }
    protected void OwnCleanup(System.Action release) { cleanup.Add(release); }
    public virtual void Dispose()
    {
        if (System.Threading.Interlocked.Exchange(ref disposed, 1) != 0) return;
        stopping.Cancel();
        for (var i = cleanup.Count - 1; i >= 0; i--)
            try { cleanup[i](); } catch (System.Exception) { }
        cleanup.Clear();
        var completion = System.Threading.Tasks.Task.WhenAll(workers);
        workers.Clear();
        if (!completion.Wait(500))
        {
            System.Console.Error.WriteLine("Music worker stop timed out; process exit will release remaining resources.");
            completion.ContinueWith(_ => stopping.Dispose(), System.Threading.Tasks.TaskScheduler.Default);
        }
        else stopping.Dispose();
    }
    public virtual void Init() {}

    public abstract string GetMusicStatus(AudioSessionManager2 sessionManager);
}
