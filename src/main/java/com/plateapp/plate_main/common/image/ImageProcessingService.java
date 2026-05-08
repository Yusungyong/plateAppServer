package com.plateapp.plate_main.common.image;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import net.coobird.thumbnailator.Thumbnails;
import org.springframework.stereotype.Service;

@Service
public class ImageProcessingService {

    public byte[] resizeMax(byte[] source, int maxWidth, int maxHeight, String formatName) throws IOException {
        try (ByteArrayInputStream in = new ByteArrayInputStream(source);
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Thumbnails.of(in)
                    .size(maxWidth, maxHeight)
                    .outputFormat(formatName)
                    .keepAspectRatio(true)
                    .toOutputStream(out);
            return out.toByteArray();
        }
    }

    public byte[] resizeCropCenter(byte[] source, int width, int height, String formatName) throws IOException {
        try (ByteArrayInputStream in = new ByteArrayInputStream(source);
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Thumbnails.of(in)
                    .size(width, height)
                    .crop(net.coobird.thumbnailator.geometry.Positions.CENTER)
                    .outputFormat(formatName)
                    .toOutputStream(out);
            return out.toByteArray();
        }
    }

    public Path resizeMaxToTempFile(Path sourcePath, int maxWidth, int maxHeight, String formatName) throws IOException {
        Path outputPath = createTempOutputFile("image-max-", formatName);
        Thumbnails.of(sourcePath.toFile())
                .size(maxWidth, maxHeight)
                .outputFormat(formatName)
                .keepAspectRatio(true)
                .toFile(outputPath.toFile());
        return outputPath;
    }

    public Path resizeCropCenterToTempFile(Path sourcePath, int width, int height, String formatName) throws IOException {
        Path outputPath = createTempOutputFile("image-thumb-", formatName);
        Thumbnails.of(sourcePath.toFile())
                .size(width, height)
                .crop(net.coobird.thumbnailator.geometry.Positions.CENTER)
                .outputFormat(formatName)
                .toFile(outputPath.toFile());
        return outputPath;
    }

    private Path createTempOutputFile(String prefix, String formatName) throws IOException {
        String suffix = "." + (formatName == null || formatName.isBlank() ? "tmp" : formatName);
        return Files.createTempFile(prefix, suffix);
    }
}
