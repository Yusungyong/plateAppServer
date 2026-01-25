package com.plateapp.plate_main.common.video;

import java.io.BufferedReader;
import java.io.File;
import java.nio.file.Files;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
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

    private static final long COMMAND_TIMEOUT_SECONDS = 60;

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
        Process p = new ProcessBuilder(cmd).redirectErrorStream(true).start();
        drainAsync(p);
        if (!p.waitFor(COMMAND_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
            p.destroyForcibly();
            throw new IOException("ffmpeg command timed out");
        }
        if (p.exitValue() != 0) {
            throw new IOException("ffmpeg exited with code " + p.exitValue());
        }
    }

    private String execAndRead(List<String> cmd) throws IOException, InterruptedException {
        Process p = new ProcessBuilder(cmd).redirectErrorStream(true).start();
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }
        }
        if (!p.waitFor(COMMAND_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
            p.destroyForcibly();
            throw new IOException("ffprobe command timed out");
        }
        if (p.exitValue() != 0) {
            throw new IOException("ffprobe exited with code " + p.exitValue());
        }
        return sb.toString();
    }

    private void drainAsync(Process p) {
        Thread t = new Thread(() -> {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
                while (reader.readLine() != null) {
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
