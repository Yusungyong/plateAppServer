package com.plateapp.plate_main.common.file;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class LocalFileStorageServiceTest {

    @TempDir
    Path tempDir;

    @Test
    void deletesFilesInsideConfiguredRoot() throws Exception {
        Path uploadRoot = Files.createDirectory(tempDir.resolve("uploads"));
        Path target = uploadRoot.resolve("restaurants/image.jpg");
        Files.createDirectories(target.getParent());
        Files.writeString(target, "image");
        LocalFileStorageService service = new LocalFileStorageService(uploadRoot.toString(), "/files");

        service.deleteByPublicUrl("https://plate.example/files/restaurants/image.jpg");

        assertThat(target).doesNotExist();
    }

    @Test
    void doesNotDeleteFilesOutsideConfiguredRoot() throws Exception {
        Path uploadRoot = Files.createDirectory(tempDir.resolve("uploads"));
        Path outside = tempDir.resolve("outside.txt");
        Files.writeString(outside, "keep");
        LocalFileStorageService service = new LocalFileStorageService(uploadRoot.toString(), "/files");

        service.deleteByPublicUrl("/files/../outside.txt");
        service.deleteByPublicUrl("/files/%2e%2e/outside.txt");
        service.deleteByPublicUrl("/files-archive/../outside.txt");

        assertThat(outside).exists();
    }
}
