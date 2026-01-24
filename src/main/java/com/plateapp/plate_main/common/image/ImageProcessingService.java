package com.plateapp.plate_main.common.image;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
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
}
