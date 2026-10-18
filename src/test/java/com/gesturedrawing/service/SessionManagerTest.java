package com.gesturedrawing.service;

import com.gesturedrawing.domain.GestureImage;
import com.gesturedrawing.domain.SessionConfig;
import com.gesturedrawing.domain.SessionPhase;
import com.gesturedrawing.domain.SessionResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SessionManagerTest {

    @TempDir
    File tempDir;

    private GestureImage a;
    private GestureImage b;
    private SessionConfig config;

    @BeforeEach
    void setUp() throws IOException {
        a = image("a.jpg");
        b = image("b.jpg");
        config = new SessionConfig(tempDir, 5, 4);
    }

    private GestureImage image(String name) throws IOException {
        File file = new File(tempDir, name);
        file.createNewFile();
        return new GestureImage(file);
    }

    private static void tick(SessionManager manager, int seconds) {
        for (int i = 0; i < seconds; i++) {
            manager.tickSecond();
        }
    }

    @Test
    void timerShouldAdvanceToNextPoseWhenItRunsOut() {
        SessionManager manager = new SessionManager(config, List.of(a, b));

        assertFalse(manager.tickSecond());
        assertFalse(manager.tickSecond());
        assertEquals(3, manager.getCurrentRemainingSeconds());
        assertFalse(manager.tickSecond());
        assertFalse(manager.tickSecond());
        assertTrue(manager.tickSecond());

        assertEquals(1, manager.getCurrentIndex());
        assertEquals(b, manager.getCurrentImage());
        assertEquals(5, manager.getCurrentRemainingSeconds());
    }

    @Test
    void recycledImagesShouldCountEveryCompletedPose() {
        SessionManager manager = new SessionManager(config, List.of(a, b, a, b));

        tick(manager, 20);

        assertTrue(manager.isSessionFinished());
        SessionResult result = manager.createResult();
        assertEquals(4, result.getCompletedImagesCount());
        assertEquals(20, result.getTotalSecondsElapsed());
        assertEquals(List.of(a, b), result.getImagesUsed(), "Gallery lists each distinct image once, in order");
    }

    @Test
    void skippingWithoutDrawingShouldNotCountAsCompleted() {
        SessionManager manager = new SessionManager(config, List.of(a, b));

        manager.skipNext();
        manager.tickSecond();
        manager.skipNext();

        assertTrue(manager.isSessionFinished());
        assertEquals(1, manager.createResult().getCompletedImagesCount());
        assertEquals(List.of(b), manager.createResult().getImagesUsed());
    }

    @Test
    void goingBackAndForthShouldNotDoubleCountAPose() {
        SessionManager manager = new SessionManager(config, List.of(a, b, a));

        manager.tickSecond();
        manager.skipNext();      // pose 1 completed
        manager.previousImage(); // back to pose 1
        manager.tickSecond();
        manager.skipNext();      // pose 1 again

        assertEquals(1, manager.createResult().getCompletedImagesCount());
    }

    @Test
    void previousShouldResetTimerAndStopAtFirstPose() {
        SessionManager manager = new SessionManager(config, List.of(a, b));

        manager.previousImage();
        assertEquals(0, manager.getCurrentIndex());

        manager.skipNext();
        manager.tickSecond();
        manager.previousImage();

        assertEquals(0, manager.getCurrentIndex());
        assertEquals(5, manager.getCurrentRemainingSeconds());
        assertNull(manager.getPreviousImage());
        assertEquals(b, manager.getNextImage());
    }

    @Test
    void pausedSessionShouldNotTick() {
        SessionManager manager = new SessionManager(config, List.of(a));

        manager.togglePause();
        tick(manager, 10);

        assertTrue(manager.isPaused());
        assertEquals(5, manager.getCurrentRemainingSeconds());
        assertEquals(0, manager.createResult().getTotalSecondsElapsed());
    }

    @Test
    void finishedSessionShouldIgnoreFurtherInput() {
        SessionManager manager = new SessionManager(config, List.of(a));

        tick(manager, 5);
        assertTrue(manager.isSessionFinished());

        manager.skipNext();
        assertFalse(manager.tickSecond());
        assertEquals(1, manager.getCurrentIndex());
        assertNull(manager.getCurrentImage());
        assertEquals(5, manager.createResult().getTotalSecondsElapsed());
    }

    @Test
    void unreadableImageSkipShouldNotCountAsCompleted() {
        SessionManager manager = new SessionManager(config, List.of(a, b));

        tick(manager, 2); // async load error arrives after the timer already ticked
        manager.skipUnreadable();

        assertEquals(1, manager.getCurrentIndex());
        assertEquals(0, manager.createResult().getCompletedImagesCount());
        assertTrue(manager.createResult().getImagesUsed().isEmpty());
    }

    @Test
    void shouldRejectMissingImages() {
        assertThrows(IllegalArgumentException.class, () -> new SessionManager(config, List.of()));
        assertThrows(IllegalArgumentException.class, () -> new SessionManager(null, List.of(a)));
    }

    @Test
    void resultShouldFormatTime() {
        assertEquals("45 seconds", new SessionResult(1, 45, List.of()).getFormattedTotalTime());
        assertEquals("2 min 5 sec", new SessionResult(1, 125, List.of()).getFormattedTotalTime());
    }

    @Test
    void phasedSessionShouldUseEachPhasesDuration() {
        SessionConfig phased = new SessionConfig(tempDir,
                List.of(new SessionPhase(2, 5), new SessionPhase(1, 7)), false, false);
        SessionManager manager = new SessionManager(phased, List.of(a, b, a));

        assertEquals(2, manager.getPhaseCount());
        assertEquals(0, manager.getCurrentPhaseIndex());
        assertTrue(manager.isFirstPoseOfPhase());
        assertEquals(5, manager.getCurrentRemainingSeconds());

        tick(manager, 5);
        assertEquals(0, manager.getCurrentPhaseIndex());
        assertFalse(manager.isFirstPoseOfPhase());
        assertEquals(5, manager.getCurrentRemainingSeconds());

        tick(manager, 5);
        assertEquals(1, manager.getCurrentPhaseIndex());
        assertTrue(manager.isFirstPoseOfPhase());
        assertEquals(new SessionPhase(1, 7), manager.getCurrentPhase());
        assertEquals(7, manager.getCurrentRemainingSeconds());

        tick(manager, 7);
        assertTrue(manager.isSessionFinished());
        assertEquals(1, manager.getCurrentPhaseIndex(), "Stays on the last phase after the end");
        assertFalse(manager.isFirstPoseOfPhase());
        assertEquals(0, manager.getCurrentPoseSeconds());
        assertEquals(17, manager.createResult().getTotalSecondsElapsed());
        assertEquals(3, manager.createResult().getCompletedImagesCount());
    }

    @Test
    void goingBackIntoEarlierPhaseShouldRestoreItsDuration() {
        SessionConfig phased = new SessionConfig(tempDir,
                List.of(new SessionPhase(1, 5), new SessionPhase(1, 10)), false, false);
        SessionManager manager = new SessionManager(phased, List.of(a, b));

        manager.skipNext();
        assertEquals(10, manager.getCurrentRemainingSeconds());
        manager.previousImage();
        assertEquals(5, manager.getCurrentRemainingSeconds());
        assertEquals(0, manager.getCurrentPhaseIndex());
    }
}
