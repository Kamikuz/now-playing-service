package com.widdit.nowplaying.service;

import com.widdit.nowplaying.entity.SettingsGeneral;
import com.widdit.nowplaying.entity.Track;
import com.widdit.nowplaying.event.TrackChangedEvent;
import com.widdit.nowplaying.service.netease.NeteaseMusicService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class NowPlayingServiceLilithTest {

    private NowPlayingService service;
    private SettingsGeneral settings;
    private NeteaseMusicService neteaseMusicService;
    private OutputService outputService;
    private ApplicationEventPublisher eventPublisher;

    @BeforeEach
    void setUp() throws Exception {
        service = new NowPlayingService();
        settings = new SettingsGeneral();
        settings.setPlatform("lilith");

        SettingsService settingsService = mock(SettingsService.class);
        when(settingsService.getSettingsGeneral()).thenReturn(settings);
        neteaseMusicService = mock(NeteaseMusicService.class);
        when(neteaseMusicService.search(any())).thenReturn(Track.builder()
                .title("搜索标题")
                .author("搜索歌手")
                .album("搜索专辑")
                .duration(180)
                .build());
        outputService = mock(OutputService.class);
        eventPublisher = mock(ApplicationEventPublisher.class);

        ReflectionTestUtils.setField(service, "settingsService", settingsService);
        ReflectionTestUtils.setField(service, "neteaseMusicService", neteaseMusicService);
        ReflectionTestUtils.setField(service, "outputService", outputService);
        ReflectionTestUtils.setField(service, "eventPublisher", eventPublisher);
    }

    @Test
    void differentPlaybackIdsRestartEvenForSameSong() throws Exception {
        service.updateLilithTrack("play-1", "Song A", "Artist A");
        service.updateLilithTrack("play-2", "Song A", "Artist A");

        verify(neteaseMusicService, times(2)).search("Song A Artist A");
        verify(eventPublisher, times(2)).publishEvent(any(TrackChangedEvent.class));
        assertEquals("Song A", service.queryTrack().getTitle());
        assertEquals("Artist A", service.queryTrack().getAuthor());
        assertEquals("搜索专辑", service.queryTrack().getAlbum());
    }

    @Test
    void duplicatePlaybackIdIsIgnored() throws Exception {
        service.updateLilithTrack("play-1", "Song A", "Artist A");
        service.updateLilithTrack("play-1", "Song A", "Artist A");

        verify(neteaseMusicService, times(1)).search("Song A Artist A");
    }

    @Test
    void playbackAndClearUseExistingPlayerState() {
        service.updateLilithTrack("play-1", "Song A", "Artist A");
        service.updateLilithPlayback(false);
        assertTrue(service.queryPlayer().getIsPaused());

        service.updateLilithPlayback(true);
        assertFalse(service.queryPlayer().getIsPaused());

        service.clearLilithTrack();
        assertFalse(service.hasSong().getData());
        assertEquals("", service.queryTrack().getTitle());
        assertEquals(0L, service.queryProgress().getProgress());
    }

    @Test
    void nonLilithPlatformIgnoresExternalUpdates() throws Exception {
        settings.setPlatform("netease");
        service.updateLilithTrack("play-1", "Song A", "Artist A");
        service.updateLilithPlayback(true);
        service.clearLilithTrack();

        verify(neteaseMusicService, never()).search(any());
        assertFalse(service.hasSong().getData());
    }
}
