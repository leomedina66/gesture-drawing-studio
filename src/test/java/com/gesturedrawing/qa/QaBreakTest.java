package com.gesturedrawing.qa;

import com.gesturedrawing.domain.*;
import com.gesturedrawing.service.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.prefs.Preferences;

import static org.junit.jupiter.api.Assertions.*;

class QaBreakTest {
    @TempDir Path tmp;

    private List<GestureImage> imgs(int n) throws Exception {
        List<GestureImage> l = new ArrayList<>();
        for (int i = 0; i < n; i++) l.add(new GestureImage(Files.createFile(tmp.resolve("i" + i + ".png")).toFile()));
        return l;
    }

    @Test
    void shouldRejectPhaseCountsThatOverflowTotal() {
        // Each phase is capped at construction, so an overflowing plan can't even be built
        assertThrows(IllegalArgumentException.class, () -> new SessionPhase(Integer.MAX_VALUE, 60));
        List<SessionPhase> many = Collections.nCopies(10_000, new SessionPhase(500, 60));
        assertThrows(IllegalArgumentException.class, () -> new SessionConfig(tmp.toFile(), many, false, false));
    }

    @Test
    void shouldRejectConfigBelowMinSeconds() {
        assertThrows(IllegalArgumentException.class, () -> new SessionConfig(tmp.toFile(), 1, 3));
    }

    @Test
    void parsedPhasesShouldAlwaysFitInSessionConfig() {
        List<SessionPhase> parsed = SessionPhase.parse("500x60;500x60");
        assertDoesNotThrow(() -> new SessionConfig(tmp.toFile(), parsed, false, false));
    }

    @Test
    void shouldParseUppercaseSeparator() {
        assertEquals(1, SessionPhase.parse("5X120").size());
    }

    @Test
    void parseShouldIgnoreGarbage() {
        assertTrue(SessionPhase.parse("5xx120;x;;-3x60;0x60;5x4;5x3601;abc;5x").isEmpty());
        assertEquals(List.of(new SessionPhase(2, 60)), SessionPhase.parse(" 2 x 60 ;zz"));
    }

    @Test
    void formatDurationEdges() {
        assertEquals("1 h", SessionPhase.formatDuration(3600));
        assertEquals("1 h 5 min", SessionPhase.formatDuration(3900));
        assertEquals("1 min 30s", SessionPhase.formatDuration(90));
    }

    @Test
    void previousImageAfterFinishShouldNotReopenSession() throws Exception {
        SessionManager m = new SessionManager(new SessionConfig(tmp.toFile(), 5, 1), imgs(1));
        m.tickSecond(); m.skipNext();
        assertTrue(m.isSessionFinished());
        m.previousImage();
        assertTrue(m.isSessionFinished(), "finished session was reopened by previousImage");
    }

    @Test
    void immediateNextOnLastPoseShouldNotCount() throws Exception {
        SessionManager m = new SessionManager(new SessionConfig(tmp.toFile(), 5, 2), imgs(2));
        m.tickSecond(); m.skipNext(); m.skipNext();
        assertEquals(1, m.createResult().getCompletedImagesCount());
    }

    @Test
    void goingBackAndForthShouldNotDoubleCount() throws Exception {
        SessionManager m = new SessionManager(new SessionConfig(tmp.toFile(), 5, 3), imgs(3));
        m.tickSecond(); m.skipNext(); m.previousImage(); m.tickSecond(); m.skipNext();
        assertEquals(1, m.createResult().getCompletedImagesCount());
    }

    @Test
    void pausedTickAndSkipBehaviour() throws Exception {
        SessionManager m = new SessionManager(new SessionConfig(tmp.toFile(), 5, 2), imgs(2));
        m.togglePause();
        assertFalse(m.tickSecond());
        assertEquals(5, m.getCurrentRemainingSeconds());
    }

    @Test
    void phasesShouldUsePerPhaseDuration() throws Exception {
        SessionConfig c = new SessionConfig(tmp.toFile(), List.of(new SessionPhase(1, 5), new SessionPhase(2, 10)), false, false);
        SessionManager m = new SessionManager(c, imgs(3));
        for (int i = 0; i < 5; i++) m.tickSecond();
        assertEquals(1, m.getCurrentPhaseIndex());
        assertEquals(10, m.getCurrentRemainingSeconds());
        assertTrue(m.isFirstPoseOfPhase());
        for (int i = 0; i < 10; i++) m.tickSecond();
        assertFalse(m.isFirstPoseOfPhase());
        for (int i = 0; i < 10; i++) m.tickSecond();
        assertTrue(m.isSessionFinished());
        assertEquals(25, m.createResult().getTotalSecondsElapsed());
        assertEquals(1, m.getCurrentPhaseIndex());
    }

    @Test
    void loaderShouldDropDirectoriesNamedLikeImagesAndHandleDuplicatesInSelection() throws Exception {
        File dir = Files.createDirectory(tmp.resolve("x.png")).toFile();
        File f = Files.createFile(tmp.resolve("a.PNG")).toFile();
        ImageLoaderService s = new ImageLoaderService(new Random(1));
        assertEquals(1, s.loadImagesFromFiles(List.of(dir, f), 1).size());
        // With one distinct file a repeat is unavoidable; with f listed twice plus g it must not happen
        File g = Files.createFile(tmp.resolve("b.png")).toFile();
        for (long seed = 0; seed < 200; seed++) {
            List<GestureImage> r = new ImageLoaderService(new Random(seed)).loadImagesFromFiles(List.of(f, f, g), 3);
            for (int i = 1; i < r.size(); i++) {
                assertNotEquals(r.get(i - 1), r.get(i), "same file selected twice yields back-to-back duplicate (seed " + seed + ")");
            }
        }
    }

    @Test
    void loaderRecyclingShouldNeverRepeatBackToBack() throws Exception {
        imgs(3);
        for (long seed = 0; seed < 200; seed++) {
            List<GestureImage> r = new ImageLoaderService(new Random(seed)).loadImagesFromDirectory(tmp.toFile(), 10);
            assertEquals(10, r.size());
            for (int i = 1; i < r.size(); i++) assertNotEquals(r.get(i - 1), r.get(i), "seed " + seed);
        }
    }

    @Test
    void readDimensionsShouldSurviveGarbage() throws Exception {
        File junk = Files.write(tmp.resolve("j.png"), new byte[]{1, 2, 3}).toFile();
        var m = ImagePreloaderService.class.getDeclaredMethod("readDimensions", File.class);
        m.setAccessible(true);
        assertNull(m.invoke(null, junk));
        assertNull(m.invoke(null, new File(tmp.toFile(), "missing.png")));
    }

    @Test
    void preferencesShouldRoundTripAndFallBack() throws Exception {
        Preferences node = Preferences.userRoot().node("gds-qa-test");
        try {
            PreferencesService p = new PreferencesService(node);
            List<SessionPhase> def = List.of(new SessionPhase(1, 60));
            assertEquals(def, p.getPhases(def));
            List<SessionPhase> big = new ArrayList<>();
            for (int i = 0; i < 500; i++) big.add(new SessionPhase(1, 3600));
            p.savePhases(big);
            assertEquals(500, p.getPhases(def).size());
            p.savePhases(List.of());
            assertEquals(def, p.getPhases(def));
            node.put("SESSION_PHASES", "garbage");
            assertEquals(def, p.getPhases(def));
            p.saveLastFolder(new File(tmp.toFile(), "nope"));
            assertNull(p.getLastFolder());
            p.saveLastFolder(tmp.toFile());
            tmp.toFile().setReadable(true);
            assertEquals(tmp.toFile(), p.getLastFolder());
        } finally {
            node.removeNode();
        }
    }
}
