package com.plateapp.plate_main.common.image;

import com.drew.imaging.ImageMetadataReader;
import com.drew.metadata.Metadata;
import com.drew.metadata.exif.ExifIFD0Directory;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
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

    private static final int ORIENTATION_NORMAL = 1;
    private static final int ORIENTATION_FLIP_HORIZONTAL = 2;
    private static final int ORIENTATION_ROTATE_180 = 3;
    private static final int ORIENTATION_FLIP_VERTICAL = 4;
    private static final int ORIENTATION_TRANSPOSE = 5;
    private static final int ORIENTATION_ROTATE_90 = 6;
    private static final int ORIENTATION_TRANSVERSE = 7;
    private static final int ORIENTATION_ROTATE_270 = 8;

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
        int orientation = readOrientation(sourcePath);
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

                BufferedImage source = reader.read(0, param);
                return applyOrientation(source, orientation);
            } finally {
                reader.dispose();
            }
        }
    }

    private int readOrientation(Path sourcePath) {
        try {
            Metadata metadata = ImageMetadataReader.readMetadata(sourcePath.toFile());
            ExifIFD0Directory directory = metadata.getFirstDirectoryOfType(ExifIFD0Directory.class);
            if (directory == null) {
                return ORIENTATION_NORMAL;
            }
            return directory.getInt(ExifIFD0Directory.TAG_ORIENTATION);
        } catch (Exception ignored) {
            return ORIENTATION_NORMAL;
        }
    }

    private BufferedImage applyOrientation(BufferedImage source, int orientation) {
        if (source == null) {
            return null;
        }

        return switch (orientation) {
            case ORIENTATION_FLIP_HORIZONTAL -> transform(source, horizontalFlipTransform(source.getWidth(), 0), false);
            case ORIENTATION_ROTATE_180 -> transform(source, rotate180Transform(source.getWidth(), source.getHeight()), false);
            case ORIENTATION_FLIP_VERTICAL -> transform(source, verticalFlipTransform(0, source.getHeight()), false);
            case ORIENTATION_TRANSPOSE -> transform(source, transposeTransform(), true);
            case ORIENTATION_ROTATE_90 -> transform(source, rotate90Transform(source.getHeight()), true);
            case ORIENTATION_TRANSVERSE -> transform(source, transverseTransform(source.getWidth(), source.getHeight()), true);
            case ORIENTATION_ROTATE_270 -> transform(source, rotate270Transform(source.getWidth()), true);
            default -> source;
        };
    }

    private BufferedImage transform(BufferedImage source, AffineTransform transform, boolean swapDimensions) {
        int targetWidth = swapDimensions ? source.getHeight() : source.getWidth();
        int targetHeight = swapDimensions ? source.getWidth() : source.getHeight();
        int imageType = source.getType() == BufferedImage.TYPE_CUSTOM ? BufferedImage.TYPE_INT_RGB : source.getType();

        BufferedImage target = new BufferedImage(targetWidth, targetHeight, imageType);
        Graphics2D graphics = target.createGraphics();
        try {
            graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            graphics.drawImage(source, transform, null);
        } finally {
            graphics.dispose();
        }
        return target;
    }

    private AffineTransform horizontalFlipTransform(double width, double translateY) {
        AffineTransform transform = new AffineTransform();
        transform.scale(-1.0d, 1.0d);
        transform.translate(-width, translateY);
        return transform;
    }

    private AffineTransform verticalFlipTransform(double translateX, double height) {
        AffineTransform transform = new AffineTransform();
        transform.scale(1.0d, -1.0d);
        transform.translate(translateX, -height);
        return transform;
    }

    private AffineTransform rotate180Transform(double width, double height) {
        AffineTransform transform = new AffineTransform();
        transform.translate(width, height);
        transform.rotate(Math.PI);
        return transform;
    }

    private AffineTransform transposeTransform() {
        AffineTransform transform = new AffineTransform();
        transform.rotate(Math.PI / 2);
        transform.scale(1.0d, -1.0d);
        return transform;
    }

    private AffineTransform rotate90Transform(double height) {
        AffineTransform transform = new AffineTransform();
        transform.translate(height, 0);
        transform.rotate(Math.PI / 2);
        return transform;
    }

    private AffineTransform transverseTransform(double width, double height) {
        AffineTransform transform = new AffineTransform();
        transform.translate(height, width);
        transform.rotate(Math.PI / 2);
        transform.scale(-1.0d, 1.0d);
        return transform;
    }

    private AffineTransform rotate270Transform(double width) {
        AffineTransform transform = new AffineTransform();
        transform.translate(0, width);
        transform.rotate(-Math.PI / 2);
        return transform;
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
