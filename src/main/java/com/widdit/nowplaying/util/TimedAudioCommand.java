package com.widdit.nowplaying.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.*;

/** Short audio queries only; never starts or controls unrelated processes. */
public final class TimedAudioCommand {
    private static final Semaphore budget = new Semaphore(4);
    private static final ExecutorService readers = Executors.newFixedThreadPool(4, task -> {
        Thread thread = new Thread(task, "audio-query-reader"); thread.setDaemon(true); return thread;
    });
    private TimedAudioCommand() {}
    public static String run(List<String> command) throws Exception {
        if (!budget.tryAcquire()) throw new IOException("Audio query capacity reached");
        try { return readOwned(new ProcessBuilder(command).redirectErrorStream(true).start(), 5000); }
        finally { budget.release(); }
    }
    public static String readOwned(Process process, long timeoutMs) throws Exception {
        Future<String> output = null;
        long deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(timeoutMs);
        try {
            output = readers.submit(() -> {
                try (InputStream input = process.getInputStream(); ByteArrayOutputStream bytes = new ByteArrayOutputStream()) {
                    byte[] chunk = new byte[4096]; int count;
                    while ((count = input.read(chunk)) != -1) {
                        if (bytes.size() + count > 1024 * 1024) throw new IOException("Audio query output capacity reached");
                        bytes.write(chunk, 0, count);
                    }
                    return new String(bytes.toByteArray(), StandardCharsets.UTF_8).trim();
                }
            });
            if (!process.waitFor(timeoutMs, TimeUnit.MILLISECONDS)) throw new TimeoutException("Audio query timed out");
            return output.get(Math.max(1, deadline - System.nanoTime()), TimeUnit.NANOSECONDS);
        } finally {
            if (process.isAlive()) process.destroyForcibly();
            if (output != null) output.cancel(true);
            process.getOutputStream().close();
            process.getInputStream().close();
            process.getErrorStream().close();
        }
    }
}
