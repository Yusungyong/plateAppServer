package com.plateapp.plate_main.common.video;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class FfmpegServiceTest {

    @Test
    void drainsOutputConcurrentlyAndReturnsIt() throws Exception {
        byte[] processOutput = (" ".repeat(32 * 1024) + "12.6\n").getBytes();
        StubProcess process = StubProcess.completed(processOutput);
        TestableFfmpegService service = serviceWith(process, 1_000);

        String output = service.execAndRead(List.of("ffprobe"));

        assertThat(output).endsWith("12.6\n");
        assertThat(process.wasDestroyed()).isFalse();
    }

    @Test
    void enforcesDeadlineEvenWhenReadingWouldWaitForEofAndForcesTermination() {
        StubProcess process = StubProcess.hanging();
        TestableFfmpegService service = serviceWith(process, 100);
        long startedAt = System.nanoTime();

        IOException error = assertThrows(IOException.class,
                () -> service.execAndRead(List.of("ffprobe")));

        assertThat(error).hasMessageContaining("timed out");
        assertThat(Duration.ofNanos(System.nanoTime() - startedAt)).isLessThan(Duration.ofSeconds(2));
        assertThat(process.wasDestroyed()).isTrue();
        assertThat(process.isAlive()).isFalse();
    }

    private TestableFfmpegService serviceWith(StubProcess process, long timeoutMillis) {
        TestableFfmpegService service = new TestableFfmpegService(process);
        ReflectionTestUtils.setField(service, "commandTimeoutMillis", timeoutMillis);
        return service;
    }

    private static final class TestableFfmpegService extends FfmpegService {
        private final Process process;

        private TestableFfmpegService(Process process) {
            this.process = process;
        }

        @Override
        Process startProcess(List<String> cmd) {
            return process;
        }
    }

    private static final class StubProcess extends Process {
        private final InputStream inputStream;
        private final AtomicBoolean alive;
        private final AtomicBoolean destroyed = new AtomicBoolean(false);

        private StubProcess(InputStream inputStream, boolean alive) {
            this.inputStream = inputStream;
            this.alive = new AtomicBoolean(alive);
        }

        static StubProcess completed(byte[] output) {
            return new StubProcess(new ByteArrayInputStream(output), false);
        }

        static StubProcess hanging() {
            return new StubProcess(new BlockingInputStream(), true);
        }

        boolean wasDestroyed() {
            return destroyed.get();
        }

        @Override
        public OutputStream getOutputStream() {
            return new ByteArrayOutputStream();
        }

        @Override
        public InputStream getInputStream() {
            return inputStream;
        }

        @Override
        public InputStream getErrorStream() {
            return InputStream.nullInputStream();
        }

        @Override
        public int waitFor() throws InterruptedException {
            while (alive.get()) {
                Thread.sleep(10);
            }
            return 0;
        }

        @Override
        public boolean waitFor(long timeout, TimeUnit unit) throws InterruptedException {
            long deadline = System.nanoTime() + unit.toNanos(timeout);
            while (alive.get() && System.nanoTime() < deadline) {
                Thread.sleep(1);
            }
            return !alive.get();
        }

        @Override
        public int exitValue() {
            if (alive.get()) {
                throw new IllegalThreadStateException("Process is still running");
            }
            return 0;
        }

        @Override
        public void destroy() {
            destroyForcibly();
        }

        @Override
        public Process destroyForcibly() {
            destroyed.set(true);
            alive.set(false);
            try {
                inputStream.close();
            } catch (IOException ignored) {
                // Test double cleanup.
            }
            return this;
        }

        @Override
        public boolean isAlive() {
            return alive.get();
        }

        @Override
        public Stream<ProcessHandle> descendants() {
            return Stream.empty();
        }
    }

    private static final class BlockingInputStream extends InputStream {
        private boolean closed;

        @Override
        public synchronized int read() throws IOException {
            while (!closed) {
                try {
                    wait();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new IOException("Interrupted while waiting for process output", e);
                }
            }
            return -1;
        }

        @Override
        public synchronized void close() {
            closed = true;
            notifyAll();
        }
    }
}
