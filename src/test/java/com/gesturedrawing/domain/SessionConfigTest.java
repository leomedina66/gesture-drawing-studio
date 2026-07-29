package com.gesturedrawing.domain;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;

import static org.junit.jupiter.api.Assertions.*;

class SessionConfigTest {

    @Test
    void shouldCreateValidSessionConfig(@TempDir File tempDir) {
        SessionConfig config = new SessionConfig(tempDir, 60, 10);

        assertEquals(tempDir, config.getImageDirectory());
        assertEquals(60, config.getSecondsPerImage());
        assertEquals(10, config.getTotalImages());
    }

    @Test
    void shouldThrowExceptionWhenDirectoryIsInvalid() {
        File nonExistent = new File("C:/non_existent_folder_xyz_123");

        assertThrows(IllegalArgumentException.class, () ->
                new SessionConfig(nonExistent, 60, 10)
        );
    }

    @Test
    void shouldThrowExceptionWhenSecondsIsZeroOrNegative(@TempDir File tempDir) {
        assertThrows(IllegalArgumentException.class, () ->
                new SessionConfig(tempDir, 0, 10)
        );
    }

    @Test
    void shouldThrowExceptionWhenTotalImagesIsZeroOrNegative(@TempDir File tempDir) {
        assertThrows(IllegalArgumentException.class, () ->
                new SessionConfig(tempDir, 60, -5)
        );
    }
}
