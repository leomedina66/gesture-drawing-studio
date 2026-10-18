package com.gesturedrawing.service;

import com.gesturedrawing.domain.GestureImage;
import com.gesturedrawing.domain.SessionConfig;
import com.gesturedrawing.domain.SessionPhase;
import com.gesturedrawing.domain.SessionResult;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Manages state and progression for an active gesture drawing session.
 * A pose counts as completed when the session moves forward past it (timer ran out or Next)
 * after at least one second was spent on it; each pose position counts at most once.
 * Each pose gets the duration of the {@link SessionPhase} it belongs to.
 */
public class SessionManager {
    private final List<SessionPhase> phases;
    private final int[] poseSeconds;
    private final int[] posePhase;
    private final List<GestureImage> sessionImages;
    private final BitSet completedPoses = new BitSet();
    private final Set<GestureImage> practicedImages = new LinkedHashSet<>();
    private int currentIndex = 0;
    private int currentRemainingSeconds;
    private int secondsOnCurrentPose = 0;
    private int totalElapsedSeconds = 0;
    private boolean isPaused = false;

    public SessionManager(SessionConfig config, List<GestureImage> loadedImages) {
        if (config == null || loadedImages == null || loadedImages.isEmpty()) {
            throw new IllegalArgumentException("Cannot start session without valid configuration and images.");
        }
        this.phases = config.getPhases();
        this.sessionImages = List.copyOf(loadedImages);
        this.poseSeconds = new int[sessionImages.size()];
        this.posePhase = new int[sessionImages.size()];

        // Lay phases out pose by pose; if fewer images were loaded than planned, the plan is cut short
        int pose = 0;
        for (int phase = 0; phase < phases.size() && pose < poseSeconds.length; phase++) {
            for (int i = 0; i < phases.get(phase).poseCount() && pose < poseSeconds.length; i++, pose++) {
                poseSeconds[pose] = phases.get(phase).secondsPerPose();
                posePhase[pose] = phase;
            }
        }
        // ...and any extra images reuse the last phase's timing
        for (; pose < poseSeconds.length; pose++) {
            poseSeconds[pose] = phases.get(phases.size() - 1).secondsPerPose();
            posePhase[pose] = phases.size() - 1;
        }
        this.currentRemainingSeconds = poseSeconds[0];
    }

    public GestureImage getCurrentImage() {
        return imageAt(currentIndex);
    }

    public GestureImage getNextImage() {
        return imageAt(currentIndex + 1);
    }

    public GestureImage getPreviousImage() {
        return imageAt(currentIndex - 1);
    }

    private GestureImage imageAt(int index) {
        return index >= 0 && index < sessionImages.size() ? sessionImages.get(index) : null;
    }

    public int getCurrentIndex() {
        return currentIndex;
    }

    public int getTotalImagesInSession() {
        return sessionImages.size();
    }

    public int getCurrentRemainingSeconds() {
        return currentRemainingSeconds;
    }

    /** Full duration of the current pose. */
    public int getCurrentPoseSeconds() {
        return isSessionFinished() ? 0 : poseSeconds[currentIndex];
    }

    /** Zero-based index of the phase the current pose belongs to. */
    public int getCurrentPhaseIndex() {
        return posePhase[Math.min(currentIndex, posePhase.length - 1)];
    }

    public SessionPhase getCurrentPhase() {
        return phases.get(getCurrentPhaseIndex());
    }

    public int getPhaseCount() {
        return phases.size();
    }

    /** True when the current pose is the first one of its phase (handy for "new phase" cues). */
    public boolean isFirstPoseOfPhase() {
        return !isSessionFinished() && (currentIndex == 0 || posePhase[currentIndex - 1] != posePhase[currentIndex]);
    }

    public boolean isPaused() {
        return isPaused;
    }

    public void togglePause() {
        this.isPaused = !this.isPaused;
    }

    /**
     * Tick one second. Returns true if image transitioned or session ended.
     */
    public boolean tickSecond() {
        if (isPaused || isSessionFinished()) {
            return false;
        }

        totalElapsedSeconds++;
        secondsOnCurrentPose++;
        currentRemainingSeconds--;

        if (currentRemainingSeconds <= 0) {
            advance();
            return true;
        }
        return false;
    }

    public void skipNext() {
        if (!isSessionFinished()) {
            advance();
        }
    }

    public void previousImage() {
        if (!isSessionFinished() && currentIndex > 0) {
            moveTo(currentIndex - 1);
        }
    }

    /** Moves past an image that failed to load, without counting it as a completed pose. */
    public void skipUnreadable() {
        if (!isSessionFinished()) {
            moveTo(currentIndex + 1);
        }
    }

    public boolean isSessionFinished() {
        return currentIndex >= sessionImages.size();
    }

    private void advance() {
        if (secondsOnCurrentPose > 0) {
            completedPoses.set(currentIndex);
            practicedImages.add(sessionImages.get(currentIndex));
        }
        moveTo(currentIndex + 1);
    }

    private void moveTo(int index) {
        currentIndex = index;
        currentRemainingSeconds = index < poseSeconds.length ? poseSeconds[index] : 0;
        secondsOnCurrentPose = 0;
    }

    public SessionResult createResult() {
        return new SessionResult(completedPoses.cardinality(), totalElapsedSeconds, new ArrayList<>(practicedImages));
    }
}
