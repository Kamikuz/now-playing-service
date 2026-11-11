using System;
using System.Net.WebSockets;
using System.Threading;
using System.Threading.Tasks;
internal sealed class SocketHeartbeat : IDisposable
{
    private readonly CancellationTokenSource stopping;
    private readonly Task worker;
    internal SocketHeartbeat(ClientWebSocket socket, byte[] payload, CancellationToken token)
    {
        stopping = CancellationTokenSource.CreateLinkedTokenSource(token);
        worker = Run(socket, payload);
    }
    private async Task Run(ClientWebSocket socket, byte[] payload)
    {
        try
        {
            while (socket.State == WebSocketState.Open && !stopping.IsCancellationRequested)
            {
                await Task.Delay(30000, stopping.Token);
                if (socket.State != WebSocketState.Open) break;
                await socket.SendAsync(new ArraySegment<byte>(payload), WebSocketMessageType.Text, true, stopping.Token);
            }
        }
        catch (OperationCanceledException) { }
        catch (Exception) { } // Owning receive loop handles reconnection; never async-void.
    }
    public void Dispose()
    {
        stopping.Cancel();
        if (worker.IsCompleted) stopping.Dispose();
        else worker.ContinueWith(_ => stopping.Dispose(), TaskScheduler.Default);
    }
}
