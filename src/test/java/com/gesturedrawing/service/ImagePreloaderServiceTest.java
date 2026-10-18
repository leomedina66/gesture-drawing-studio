package com.gesturedrawing.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import static org.junit.jupiter.api.Assertions.*;

class ImagePreloaderServiceTest {

    @Test
    void shouldReadImageSizeFromHeader(@TempDir File tempDir) throws IOException {
        File png = new File(tempDir, "pose.png");
        ImageIO.write(new BufferedImage(320, 200, BufferedImage.TYPE_INT_RGB), "png", png);

        assertArrayEquals(new int[]{320, 200}, ImagePreloaderService.readDimensions(png));
    }

    @Test
    void shouldReturnNullForUnreadableFile(@TempDir File tempDir) throws IOException {
        File fake = new File(tempDir, "fake.jpg");
        Files.writeString(fake.toPath(), "not an image");

        assertNull(ImagePreloaderService.readDimensions(fake));
        assertNull(ImagePreloaderService.readDimensions(new File(tempDir, "missing.png")));
    }

    @Test
    void shouldFlagImagesAboveThePixelCap() {
        assertTrue(ImagePreloaderService.isTooLarge(new int[]{20_000, 20_000}), "400 MP decompression bomb");
        assertFalse(ImagePreloaderService.isTooLarge(new int[]{8_000, 6_000}), "48 MP camera photo");
        assertFalse(ImagePreloaderService.isTooLarge(null), "unknown size is left to the decoder");
    }
}
