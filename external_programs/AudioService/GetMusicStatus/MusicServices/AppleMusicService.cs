using System;
using System.IO;
using System.Text;
using CSCore.CoreAudioAPI;

/*
    Apple Music 没有窗口标题，此处通过魔改后的 Cider v1.6.3（一个能听 Apple Music 的客户端）来获取歌曲信息
*/
public class AppleMusicService : MusicService
{
    public override string GetMusicStatus(AudioSessionManager2 sessionManager)
    {
        double volume = 0;
        bool musicAppRunning = false;

        AudioSessionEnumerator sessionEnumerator = null;

        try
        {
            sessionEnumerator = sessionManager.GetSessionEnumerator();

            // 遍历所有会话，寻找匹配的进程
            foreach (var ownedSession in OwnedAudioSessions.Enumerate(sessionEnumerator))
            {
                var session = ownedSession.Session;
                if (session == null)
                {
                    continue;
                }

                AudioSessionControl2 sessionControl = ownedSession.Own(session.QueryInterface<AudioSessionControl2>());
                if (sessionControl == null || ownedSession.GetProcess(sessionControl) == null)
                {
                    continue;
                }

                string processName = ownedSession.GetProcess(sessionControl).ProcessName;
                AudioMeterInformation meter = null;

                if (processName.StartsWith("Cider"))
                {
                    musicAppRunning = true;
                    meter = ownedSession.Own(session.QueryInterface<AudioMeterInformation>());
                    volume += meter.PeakValue;
                }

                // 释放对象
            }
        }
        catch (Exception)
        {
            return "None";
        }
        finally
        {
            // 释放对象
        }

        // 未检测到音乐软件进程
        if (!musicAppRunning)
        {
            return "None";
        }

        string title = "";
        string appDataPath = Environment.GetFolderPath(Environment.SpecialFolder.ApplicationData);
        string filePath = Path.Combine(appDataPath, "Cider", "Plugins", "title.txt");

        if (File.Exists(filePath))
        {
            title = File.ReadAllText(filePath, Encoding.UTF8).Trim();
        }

        if (string.IsNullOrEmpty(title))
        {
            return "None";
        }

        // 输出结果
        string status = volume > 0.00001 ? "Playing" : "Paused";
        return $"{status}\r\n{title}";
    }
}
