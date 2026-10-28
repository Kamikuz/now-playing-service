package com.widdit.nowplaying.service;

import com.widdit.nowplaying.entity.SettingsGeneral;
import com.widdit.nowplaying.util.TimedAudioCommand;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;
import java.io.*;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AudioServiceLifecycleTest {
    private Process process(String output, boolean exits) throws Exception {
        Process process = mock(Process.class);
        when(process.getInputStream()).thenReturn(new ByteArrayInputStream(output.getBytes("UTF-8")));
        when(process.getErrorStream()).thenReturn(new ByteArrayInputStream(new byte[0]));
        when(process.getOutputStream()).thenReturn(new ByteArrayOutputStream());
        when(process.waitFor(anyLong(), eq(TimeUnit.MILLISECONDS))).thenReturn(exits);
        when(process.isAlive()).thenReturn(!exits);
        return process;
    }
    @Test void queriesFinishTimeoutAndBoundOutput() throws Exception {
        Process success = process(" true\n", true);
        assertEquals("true", TimedAudioCommand.readOwned(success,100));
        verify(success,never()).destroyForcibly();
        Process hung = process("",false);
        assertThrows(TimeoutException.class,()->TimedAudioCommand.readOwned(hung,10));
        verify(hung).destroyForcibly();
        Process oversized=process("x".repeat(1024*1024+1),true);
        assertThrows(java.util.concurrent.ExecutionException.class,()->TimedAudioCommand.readOwned(oversized,1000));
    }
    @Test void stopCapturesOnlyOwnedProcessAndInvalidatesGeneration() throws Exception {
        AudioService service = new AudioService();
        Process old = process("",false);
        ReflectionTestUtils.setField(service,"getMusicStatusProcess",old);
        ReflectionTestUtils.setField(service,"musicGeneration",7L);
        service.stopGetMusicStatus();
        assertNull(ReflectionTestUtils.getField(service,"getMusicStatusProcess"));
        assertEquals(8L,ReflectionTestUtils.getField(service,"musicGeneration"));
        verify(old).destroyForcibly();
        verify(old).waitFor(2500,TimeUnit.MILLISECONDS);
        verify(old).waitFor(500,TimeUnit.MILLISECONDS);
        service.stopGetMusicStatus();
        verify(old,times(1)).destroyForcibly();
    }
    @Test void closePreventsNewStartAndEmptyDeviceFailureRemainsValid() throws Exception {
        AudioService service=new AudioService();
        service.cleanup();
        service.startGetMusicStatus(); // must not dereference unset SettingsService or spawn
        assertNull(ReflectionTestUtils.getField(service,"getMusicStatusProcess"));
    }
    @Test void protocolReaderKeepsStatusTitleProgress() throws Exception {
        Process child=process("Playing\nSong - Author\nProgress:8|123\n",true);
        AudioService service=new AudioService(){
            @Override protected Process createMusicStatusProcess(List<String> ignored) { return child; }
        };
        SettingsService settings=mock(SettingsService.class);
        SettingsGeneral general=new SettingsGeneral();
        general.setPlatform("netease");general.setDeviceId("default");general.setSmtc(true);general.setPollInterval(100);
        general.setFallbackPlatformEnabled(false);
        when(settings.getSettingsGeneral()).thenReturn(general);
        ReflectionTestUtils.setField(service,"settingsService",settings);
        ReflectionTestUtils.setField(service,"eventPublisher",mock(ApplicationEventPublisher.class));
        try {
            service.startGetMusicStatus();
            Thread reader=(Thread)ReflectionTestUtils.getField(service,"musicStatusReaderThread");
            reader.join(1000);assertFalse(reader.isAlive());
            assertEquals("Playing",service.getStatus());assertEquals("Song - Author",service.getWindowTitle());
            assertEquals(8,service.getProgressSeconds());assertEquals(123,service.getTotalSeconds());
        } finally {service.cleanup();}
    }
}
