package com.gesturedrawing.domain;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SessionConfigTest {

    @Test
    void shouldCreateValidSessionConfig(@TempDir File tempDir) {
        SessionConfig config = new SessionConfig(tempDir, 60, 10);

        assertEquals(tempDir, config.getImageDirectory());
        assertEquals(List.of(new SessionPhase(10, 60)), config.getPhases());
        assertEquals(10, config.getTotalImages());
        assertFalse(config.isDefaultFlipped());
        assertFalse(config.isDefaultGrayscale());
    }

    @Test
    void shouldSupportVisualFilterFlags(@TempDir File tempDir) {
        SessionConfig config = new SessionConfig(tempDir, 30, 5, true, true);

        assertTrue(config.isDefaultFlipped());
        assertTrue(config.isDefaultGrayscale());
    }

    @Test
    void shouldThrowExceptionWhenDirectoryIsInvalid(@TempDir File tempDir) {
        File nonExistent = new File(tempDir, "missing");

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

    @Test
    void fileSelectionShouldBeImmutableCopy(@TempDir File tempDir) {
        List<File> files = new ArrayList<>(List.of(new File(tempDir, "a.jpg")));
        SessionConfig config = new SessionConfig(files, 30, 5, false, false);
        files.clear();

        assertNull(config.getImageDirectory());
        assertEquals(1, config.getSelectedFiles().size());
        assertThrows(UnsupportedOperationException.class, () -> config.getSelectedFiles().clear());
    }

    @Test
    void shouldRejectEmptyFileSelection() {
        assertThrows(IllegalArgumentException.class, () -> new SessionConfig(List.of(), 30, 5, false, false));
    }

    @Test
    void phasedConfigShouldSumPosesAndTime(@TempDir File tempDir) {
        SessionConfig config = new SessionConfig(tempDir,
                List.of(new SessionPhase(2, 60), new SessionPhase(5, 120), new SessionPhase(1, 600)), false, false);

        assertEquals(8, config.getTotalImages());
        assertEquals(2 * 60 + 5 * 120 + 600, config.getTotalSeconds());
        assertEquals(3, config.getPhases().size());
    }

    @Test
    void shouldRejectEmptyOrOversizedPlans(@TempDir File tempDir) {
        assertThrows(IllegalArgumentException.class, () -> new SessionConfig(tempDir, List.of(), false, false));
        assertThrows(IllegalArgumentException.class, () -> new SessionConfig(tempDir,
                List.of(new SessionPhase(300, 30), new SessionPhase(201, 30)), false, false));
        assertEquals(500, new SessionConfig(tempDir,
                List.of(new SessionPhase(300, 30), new SessionPhase(200, 30)), false, false).getTotalImages());
    }
}
