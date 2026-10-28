package com.widdit.nowplaying.service.lilith;

import com.alibaba.fastjson.JSON;
import com.widdit.nowplaying.entity.SettingsGeneral;
import com.widdit.nowplaying.event.SettingsGeneralChangedEvent;
import com.widdit.nowplaying.service.NowPlayingService;
import com.widdit.nowplaying.service.SettingsService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.websocket.ClientEndpointConfig;
import javax.websocket.CloseReason;
import javax.websocket.ContainerProvider;
import javax.websocket.Endpoint;
import javax.websocket.EndpointConfig;
import javax.websocket.MessageHandler;
import javax.websocket.Session;
import javax.websocket.WebSocketContainer;
import java.net.URI;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

@Service
@Slf4j
public class LilithWebSocketClient {

    private static final String PLATFORM = "lilith";
    private static final String DEFAULT_URL = "ws://127.0.0.1:18988/now-playing";
    private static final long RECONNECT_DELAY_SECONDS = 3L;

    @Autowired
    private SettingsService settingsService;
    @Autowired
    private NowPlayingService nowPlayingService;

    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor(runnable -> {
        Thread thread = new Thread(runnable, "lilith-now-playing-client");
        thread.setDaemon(true);
        return thread;
    });
    private final AtomicLong generation = new AtomicLong();
    private volatile Session session;
    private volatile boolean reconnectScheduled;

    @PostConstruct
    public void init() {
        refreshConnection();
    }

    @PreDestroy
    public void destroy() {
        generation.incrementAndGet();
        closeSession();
        executor.shutdownNow();
    }

    @EventListener
    public void handleSettingsGeneralChanged(SettingsGeneralChangedEvent event) {
        refreshConnection();
    }

    private void refreshConnection() {
        long currentGeneration = generation.incrementAndGet();
        closeSession();
        reconnectScheduled = false;
        if (isEnabled()) {
            executor.execute(() -> connect(currentGeneration));
        }
    }

    private void connect(long expectedGeneration) {
        if (expectedGeneration != generation.get() || !isEnabled() || isOpen()) {
            return;
        }

        try {
            WebSocketContainer container = ContainerProvider.getWebSocketContainer();
            ClientEndpointConfig config = ClientEndpointConfig.Builder.create().build();
            Session connectedSession = container.connectToServer(new Endpoint() {
                @Override
                public void onOpen(Session openedSession, EndpointConfig endpointConfig) {
                    if (expectedGeneration != generation.get() || !isEnabled()) {
                        closeQuietly(openedSession);
                        return;
                    }
                    session = openedSession;
                    reconnectScheduled = false;
                    openedSession.addMessageHandler(new MessageHandler.Whole<String>() {
                        @Override
                        public void onMessage(String message) {
                            LilithWebSocketClient.this.handleMessage(message);
                        }
                    });
                    log.info("已连接 Lilith 点歌机 Now Playing 服务");
                }

                @Override
                public void onClose(Session closedSession, CloseReason closeReason) {
                    session = null;
                    log.info("Lilith 点歌机 Now Playing 连接已断开");
                    scheduleReconnect(expectedGeneration);
                }

                @Override
                public void onError(Session failedSession, Throwable throwable) {
                    log.warn("Lilith 点歌机 Now Playing 连接异常: {}", throwable.getMessage());
                    closeQuietly(failedSession);
                    scheduleReconnect(expectedGeneration);
                }
            }, config, URI.create(getUrl()));

            if (expectedGeneration != generation.get() || !isEnabled()) {
                closeQuietly(connectedSession);
            }
        } catch (Exception e) {
            log.debug("暂时无法连接 Lilith 点歌机 Now Playing 服务", e);
            scheduleReconnect(expectedGeneration);
        }
    }

    private void handleMessage(String json) {
        if (!isEnabled()) return;

        try {
            LilithMessage message = JSON.parseObject(json, LilithMessage.class);
            if (message == null || message.getType() == null) {
                log.warn("忽略无效的 Lilith Now Playing 消息");
                return;
            }

            switch (message.getType()) {
                case "track":
                    if (isBlank(message.getId()) || isBlank(message.getTitle()) || isBlank(message.getArtist())) {
                        log.warn("忽略字段不完整的 Lilith track 消息");
                        return;
                    }
                    nowPlayingService.updateLilithTrack(
                            message.getId(), message.getTitle(), message.getArtist(),
                            message.getLrc(), message.getTrans(), message.getDuration(), message.getElapsed());
                    break;
                case "playback":
                    if (message.getPlaying() == null) {
                        log.warn("忽略字段不完整的 Lilith playback 消息");
                        return;
                    }
                    nowPlayingService.updateLilithProgress(null, message.getElapsed());
                    nowPlayingService.updateLilithPlayback(message.getPlaying());
                    break;
                case "progress":
                    nowPlayingService.updateLilithProgress(message.getId(), message.getElapsed());
                    break;
                case "clear":
                    nowPlayingService.clearLilithTrack();
                    break;
                default:
                    log.warn("忽略未知的 Lilith Now Playing 消息类型: {}", message.getType());
            }
        } catch (Exception e) {
            log.warn("忽略无法解析的 Lilith Now Playing 消息: {}", e.getMessage());
        }
    }

    private void scheduleReconnect(long expectedGeneration) {
        if (expectedGeneration != generation.get() || !isEnabled() || reconnectScheduled) return;
        reconnectScheduled = true;
        executor.schedule(() -> {
            reconnectScheduled = false;
            connect(expectedGeneration);
        }, RECONNECT_DELAY_SECONDS, TimeUnit.SECONDS);
    }

    private boolean isEnabled() {
        return PLATFORM.equals(settingsService.getSettingsGeneral().getPlatform());
    }

    private String getUrl() {
        String configured = settingsService.getSettingsGeneral().getLilithWsUrl();
        return isBlank(configured) ? DEFAULT_URL : configured.trim();
    }

    private boolean isOpen() {
        Session current = session;
        return current != null && current.isOpen();
    }

    private void closeSession() {
        Session current = session;
        session = null;
        closeQuietly(current);
    }

    private static void closeQuietly(Session target) {
        if (target == null) return;
        try {
            target.close();
        } catch (Exception ignored) {
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
