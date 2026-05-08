package com.plateapp.plate_main.common.image;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.imageio.ImageIO;
import javax.imageio.ImageReadParam;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
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
        BufferedImage source = readSubsampled(sourcePath, maxWidth, maxHeight);
        Thumbnails.of(source)
                .size(maxWidth, maxHeight)
                .outputFormat(formatName)
                .keepAspectRatio(true)
                .toFile(outputPath.toFile());
        return outputPath;
    }

    public Path resizeCropCenterToTempFile(Path sourcePath, int width, int height, String formatName) throws IOException {
        Path outputPath = createTempOutputFile("image-thumb-", formatName);
        BufferedImage source = readSubsampled(sourcePath, width, height);
        Thumbnails.of(source)
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

    private BufferedImage readSubsampled(Path sourcePath, int targetWidth, int targetHeight) throws IOException {
        try (ImageInputStream imageInput = ImageIO.createImageInputStream(sourcePath.toFile())) {
            if (imageInput == null) {
                throw new IOException("Failed to open image input stream");
            }

            java.util.Iterator<ImageReader> readers = ImageIO.getImageReaders(imageInput);
            if (!readers.hasNext()) {
                throw new IOException("Unsupported image format");
            }

            ImageReader reader = readers.next();
            try {
                reader.setInput(imageInput, true, true);
                int width = reader.getWidth(0);
                int height = reader.getHeight(0);

                ImageReadParam param = reader.getDefaultReadParam();
                int subsampling = calculateSubsampling(width, height, targetWidth, targetHeight);
                if (subsampling > 1) {
                    param.setSourceSubsampling(subsampling, subsampling, 0, 0);
                }

                return reader.read(0, param);
            } finally {
                reader.dispose();
            }
        }
    }

    private int calculateSubsampling(int width, int height, int targetWidth, int targetHeight) {
        int safeTargetWidth = Math.max(1, targetWidth);
        int safeTargetHeight = Math.max(1, targetHeight);
        double widthRatio = (double) width / safeTargetWidth;
        double heightRatio = (double) height / safeTargetHeight;
        double scale = Math.max(widthRatio, heightRatio);
        if (scale < 2.0d) {
            return 1;
        }
        return Math.max(1, (int) Math.floor(scale));
    }
}
