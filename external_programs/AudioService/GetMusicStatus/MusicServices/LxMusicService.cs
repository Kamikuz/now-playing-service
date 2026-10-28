using System;
using System.Collections.Generic;
using CSCore.CoreAudioAPI;

public class LxMusicService : MusicService
{
    public override string GetMusicStatus(AudioSessionManager2 sessionManager)
    {
        double volume = 0;
        bool musicAppRunning = false;
        string windowTitle = "";

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

                if (processName.StartsWith("lx-music-desktop"))
                {
                    musicAppRunning = true;
                    meter = ownedSession.Own(session.QueryInterface<AudioMeterInformation>());
                    volume += meter.PeakValue;
                    // 洛雪音乐的窗口标题需另行获取
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

        // 获取洛雪音乐的窗口标题
        try
        {
            List<string> allTitles = WindowDetector.GetWindowTitles("lx-music-desktop");
            foreach (string title in allTitles)
            {
                if (title.Contains(" - ") && !title.Contains("LX Music"))
                {
                    windowTitle = FixTitleLx(title);
                    break;
                }
            }
        }
        catch (Exception)
        {
            return "None";
        }

        // 如果窗口标题为空（说明没成功获取到），则返回 None
        if (string.IsNullOrEmpty(windowTitle))
        {
            return "None";
        }

        // 输出结果
        string status = volume > 0.00001 ? "Playing" : "Paused";
        return $"{status}\r\n{windowTitle}";
    }

    /*
        修正洛雪音乐标题
    */
    private string FixTitleLx(string windowTitle)
    {
        windowTitle = windowTitle.Replace("、", " / ");

        return windowTitle;
    }
}
