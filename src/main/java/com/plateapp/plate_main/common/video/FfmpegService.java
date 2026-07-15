package com.plateapp.plate_main.common.video;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class FfmpegService {

    @Value("${ffmpeg.path:ffmpeg}")
    private String ffmpegPath;

    @Value("${ffprobe.path:ffprobe}")
    private String ffprobePath;

    @Value("${ffmpeg.command-timeout-millis:60000}")
    private long commandTimeoutMillis = 60_000;

    private static final int MAX_CAPTURED_OUTPUT_CHARS = 64 * 1024;
    private static final long TERMINATION_WAIT_MILLIS = 1_000;

    public Integer probeDurationSeconds(File videoFile) {
        List<String> cmd = List.of(
                ffprobePath,
                "-v", "error",
                "-show_entries", "format=duration",
                "-of", "default=noprint_wrappers=1:nokey=1",
                videoFile.getAbsolutePath()
        );
        try {
            String output = execAndRead(cmd);
            if (output == null || output.isBlank()) {
                return null;
            }
            double seconds = Double.parseDouble(output.trim());
            return (int) Math.round(seconds);
        } catch (Exception e) {
            log.warn("ffprobe failed: {}", e.getMessage());
            return null;
        }
    }

    public byte[] generateThumbnail(File videoFile, int captureSecond, int maxWidth, int maxHeight, String format) {
        File thumb = null;
        try {
            thumb = File.createTempFile("ffthumb-", "." + format);
            List<String> cmd = new ArrayList<>();
            cmd.add(ffmpegPath);
            cmd.add("-ss");
            cmd.add(String.format("00:00:%02d", Math.max(captureSecond, 0)));
            cmd.add("-i");
            cmd.add(videoFile.getAbsolutePath());
            cmd.add("-vframes");
            cmd.add("1");
            if (maxWidth > 0 || maxHeight > 0) {
                String scale = String.format("scale=%d:%d", maxWidth > 0 ? maxWidth : -1, maxHeight > 0 ? maxHeight : -1);
                cmd.add("-vf");
                cmd.add(scale);
            }
            cmd.add("-y");
            cmd.add(thumb.getAbsolutePath());

            exec(cmd);
            return java.nio.file.Files.readAllBytes(thumb.toPath());
        } catch (Exception e) {
            log.warn("ffmpeg thumbnail failed: {}", e.getMessage());
            return null;
        } finally {
            if (thumb != null && thumb.exists()) {
                thumb.delete();
            }
        }
    }

    /**
     * 영상 용량 축소를 위한 간단한 트랜스코딩 (H.264/AAC, 해상도 제한, 비트레이트 고정).
     * @param input 원본 파일
     * @param maxWidth 최대 가로
     * @param maxHeight 최대 세로
     * @param videoBitrateKbps 비디오 비트레이트(kbps)
     * @param audioBitrateKbps 오디오 비트레이트(kbps)
     * @return 트랜스코딩 결과 파일 (임시 파일). 호출 후 필요 시 삭제.
     */
    public File transcodeOptimized(File input, int maxWidth, int maxHeight, int videoBitrateKbps, int audioBitrateKbps) {
        File out = null;
        try {
            out = Files.createTempFile("fftrans-", ".mp4").toFile();
            List<String> cmd = new ArrayList<>();
            cmd.add(ffmpegPath);
            cmd.add("-y");
            cmd.add("-i");
            cmd.add(input.getAbsolutePath());
            cmd.add("-vf");
            cmd.add(String.format("scale='min(%d,iw)':'min(%d,ih)':force_original_aspect_ratio=decrease", maxWidth, maxHeight));
            cmd.add("-c:v");
            cmd.add("libx264");
            cmd.add("-b:v");
            cmd.add(videoBitrateKbps + "k");
            cmd.add("-preset");
            cmd.add("veryfast"); // 더 빠른 프리셋으로 타임아웃 완화
            cmd.add("-c:a");
            cmd.add("aac");
            cmd.add("-b:a");
            cmd.add(audioBitrateKbps + "k");
            cmd.add(out.getAbsolutePath());

            exec(cmd);
            return out;
        } catch (Exception e) {
            if (out != null && out.exists()) {
                out.delete();
            }
            log.warn("ffmpeg transcode failed: {}", e.getMessage());
            return null;
        }
    }

    private void exec(List<String> cmd) throws IOException, InterruptedException {
        Process p = startProcess(cmd);
        drainAsync(p);
        try {
            if (!p.waitFor(configuredTimeoutMillis(), TimeUnit.MILLISECONDS)) {
                terminateForcibly(p);
                throw new IOException("ffmpeg command timed out");
            }
            if (p.exitValue() != 0) {
                throw new IOException("ffmpeg exited with code " + p.exitValue());
            }
        } finally {
            if (p.isAlive()) {
                terminateForcibly(p);
            }
            closeProcessStreams(p);
        }
    }

    String execAndRead(List<String> cmd) throws IOException, InterruptedException {
        Process p = startProcess(cmd);
        StringBuilder output = new StringBuilder();
        AtomicReference<IOException> readFailure = new AtomicReference<>();
        AtomicBoolean outputTooLarge = new AtomicBoolean(false);
        Thread outputReader = collectOutputAsync(p, output, readFailure, outputTooLarge);
        long timeoutMillis = configuredTimeoutMillis();
        long deadlineNanos = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(timeoutMillis);

        try {
            if (!p.waitFor(timeoutMillis, TimeUnit.MILLISECONDS)) {
                terminateForcibly(p);
                throw new IOException("ffprobe command timed out");
            }

            if (outputReader.isAlive()) {
                long remainingMillis = remainingMillis(deadlineNanos);
                if (remainingMillis > 0) {
                    outputReader.join(remainingMillis);
                }
            }
            if (outputReader.isAlive()) {
                terminateForcibly(p);
                throw new IOException("ffprobe output read timed out");
            }
            if (readFailure.get() != null) {
                throw readFailure.get();
            }
            if (outputTooLarge.get()) {
                throw new IOException("ffprobe output exceeded limit");
            }
            if (p.exitValue() != 0) {
                throw new IOException("ffprobe exited with code " + p.exitValue());
            }
            return output.toString();
        } finally {
            if (p.isAlive()) {
                terminateForcibly(p);
            }
            closeProcessStreams(p);
            stopOutputReader(outputReader);
        }
    }

    private Thread collectOutputAsync(
            Process process,
            StringBuilder output,
            AtomicReference<IOException> readFailure,
            AtomicBoolean outputTooLarge
    ) {
        Thread thread = new Thread(() -> {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                char[] buffer = new char[1_024];
                int read;
                while ((read = reader.read(buffer)) != -1) {
                    int remaining = MAX_CAPTURED_OUTPUT_CHARS - output.length();
                    if (remaining > 0) {
                        output.append(buffer, 0, Math.min(read, remaining));
                    }
                    if (read > remaining) {
                        outputTooLarge.set(true);
                    }
                }
            } catch (IOException e) {
                readFailure.compareAndSet(null, e);
            }
        }, "ffprobe-output-drain");
        thread.setDaemon(true);
        thread.start();
        return thread;
    }

    Process startProcess(List<String> cmd) throws IOException {
        return new ProcessBuilder(cmd).redirectErrorStream(true).start();
    }

    private long configuredTimeoutMillis() {
        return Math.max(1L, commandTimeoutMillis);
    }

    private long remainingMillis(long deadlineNanos) {
        long remainingNanos = deadlineNanos - System.nanoTime();
        if (remainingNanos <= 0) {
            return 0;
        }
        return Math.max(1L, TimeUnit.NANOSECONDS.toMillis(remainingNanos));
    }

    private void terminateForcibly(Process process) {
        process.descendants().forEach(handle -> {
            try {
                handle.destroyForcibly();
            } catch (RuntimeException e) {
                log.debug("Unable to terminate media process descendant", e);
            }
        });
        process.destroyForcibly();
        try {
            if (!process.waitFor(TERMINATION_WAIT_MILLIS, TimeUnit.MILLISECONDS)) {
                log.warn("Media process did not terminate after forced shutdown");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void closeProcessStreams(Process process) {
        closeQuietly(process.getInputStream());
        closeQuietly(process.getErrorStream());
        closeQuietly(process.getOutputStream());
    }

    private void stopOutputReader(Thread outputReader) {
        if (!outputReader.isAlive()) {
            return;
        }
        outputReader.interrupt();
        try {
            outputReader.join(250);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void closeQuietly(Closeable closeable) {
        try {
            closeable.close();
        } catch (IOException ignored) {
            // Best-effort cleanup after process completion or forced termination.
        }
    }

    private void drainAsync(Process p) {
        Thread t = new Thread(() -> {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
                char[] buffer = new char[4_096];
                while (reader.read(buffer) != -1) {
                    // drain output to avoid blocking
                }
            } catch (IOException ignored) {
                // ignore
            }
        }, "ffmpeg-drain");
        t.setDaemon(true);
        t.start();
    }
}
