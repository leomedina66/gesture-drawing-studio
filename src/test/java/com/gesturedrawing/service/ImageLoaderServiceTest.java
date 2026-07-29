package com.gesturedrawing.service;

import com.gesturedrawing.domain.GestureImage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ImageLoaderServiceTest {

    private final ImageLoaderService imageLoaderService = new ImageLoaderService();

    @Test
    void shouldLoadOnlySupportedImageFormats(@TempDir File tempDir) throws IOException {
        // Create sample test files
        new File(tempDir, "pose1.jpg").createNewFile();
        new File(tempDir, "pose2.PNG").createNewFile();
        new File(tempDir, "pose3.webp").createNewFile();
        new File(tempDir, "notes.txt").createNewFile();
        new File(tempDir, "document.pdf").createNewFile();

        List<GestureImage> images = imageLoaderService.loadImagesFromDirectory(tempDir, 10);

        assertEquals(3, images.size());
        assertTrue(images.stream().anyMatch(img -> img.getFileName().equalsIgnoreCase("pose1.jpg")));
        assertTrue(images.stream().anyMatch(img -> img.getFileName().equalsIgnoreCase("pose2.PNG")));
        assertTrue(images.stream().anyMatch(img -> img.getFileName().equalsIgnoreCase("pose3.webp")));
        assertFalse(images.stream().anyMatch(img -> img.getFileName().equalsIgnoreCase("notes.txt")));
    }

    @Test
    void shouldLimitLoadedImagesToMaxRequested(@TempDir File tempDir) throws IOException {
        for (int i = 1; i <= 10; i++) {
            new File(tempDir, "img" + i + ".jpeg").createNewFile();
        }

        List<GestureImage> images = imageLoaderService.loadImagesFromDirectory(tempDir, 4);

        assertEquals(4, images.size());
    }

    @Test
    void shouldReturnEmptyListForEmptyFolder(@TempDir File tempDir) {
        List<GestureImage> images = imageLoaderService.loadImagesFromDirectory(tempDir, 10);

        assertTrue(images.isEmpty());
    }
}
