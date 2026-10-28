package com.widdit.nowplaying.service;
import com.widdit.nowplaying.entity.*;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
class LilithBridgeTest {
 @Test void directLyricsSeekPauseAndStaleTrackProtection() {
  SettingsService settings=mock(SettingsService.class);
  SettingsGeneral general=new SettingsGeneral();general.setPlatform("lilith");
  when(settings.getSettingsGeneral()).thenReturn(general);
  ApplicationEventPublisher events=mock(ApplicationEventPublisher.class);
  LyricService lyrics=new LyricService();
  ReflectionTestUtils.setField(lyrics,"settingsService",settings);
  ReflectionTestUtils.setField(lyrics,"eventPublisher",events);
  NowPlayingService service=new NowPlayingService();
  ReflectionTestUtils.setField(service,"settingsService",settings);
  ReflectionTestUtils.setField(service,"lyricService",lyrics);
  ReflectionTestUtils.setField(service,"eventPublisher",events);
  service.updateLilithTrack("a","song","artist","[00:01.00]hello","[00:01.00]translation",120.0,15.0);
  assertEquals("[00:01.00]hello",lyrics.getLyric().getLrc());
  assertTrue(lyrics.getLyric().getHasTranslatedLyric());
  assertEquals(15000,service.getCurrentProgressMs());
  service.updateLilithPlayback(true);service.updateLilithPlayback(false);
  service.updateLilithProgress("a",42.5);assertEquals(42500,service.getCurrentProgressMs());
  service.updateLilithProgress("old",2.0);assertEquals(42500,service.getCurrentProgressMs());
  service.updateLilithProgress("a",Double.NaN);assertEquals(42500,service.getCurrentProgressMs());
  service.updateLilithTrack("b","next","artist","","",60.0,0.0);
  assertFalse(lyrics.getLyric().getHasLyric());assertEquals(0,service.getCurrentProgressMs());
  service.clearLilithTrack();assertFalse(lyrics.getLyric().getHasLyric());
  general.setPlatform("netease");service.updateLilithTrack("c","ignored","artist","text","",1.0,0.0);
  assertEquals("",service.queryTrack().getTitle());
 }
}
