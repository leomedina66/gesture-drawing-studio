package com.gesturedrawing.service;

import com.gesturedrawing.domain.GestureImage;
import com.gesturedrawing.domain.SessionConfig;
import com.gesturedrawing.domain.SessionResult;

import java.util.ArrayList;
import java.util.List;

/**
 * Manages state and progression for an active gesture drawing session.
 */
public class SessionManager {
    private final SessionConfig config;
    private final List<GestureImage> sessionImages;
    private int currentIndex = 0;
    private int currentRemainingSeconds;
    private int totalElapsedSeconds = 0;
    private boolean isPaused = false;
    private final List<GestureImage> completedImages = new ArrayList<>();

    public SessionManager(SessionConfig config, List<GestureImage> loadedImages) {
        if (config == null || loadedImages == null || loadedImages.isEmpty()) {
            throw new IllegalArgumentException("Cannot start session without valid configuration and images.");
        }
        this.config = config;
        this.sessionImages = new ArrayList<>(loadedImages);
        this.currentRemainingSeconds = config.getSecondsPerImage();
    }

    public GestureImage getCurrentImage() {
        if (currentIndex < sessionImages.size()) {
            return sessionImages.get(currentIndex);
        }
        return null;
    }

    public GestureImage getNextImage() {
        if (currentIndex + 1 < sessionImages.size()) {
            return sessionImages.get(currentIndex + 1);
        }
        return null;
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
        if (isPaused) {
            return false;
        }

        totalElapsedSeconds++;
        currentRemainingSeconds--;

        if (currentRemainingSeconds <= 0) {
            recordCurrentImageCompleted();
            currentIndex++;
            if (currentIndex < sessionImages.size()) {
                currentRemainingSeconds = config.getSecondsPerImage();
            }
            return true; // Indicates image state change
        }

        return false;
    }

    public void skipNext() {
        recordCurrentImageCompleted();
        currentIndex++;
        if (currentIndex < sessionImages.size()) {
            currentRemainingSeconds = config.getSecondsPerImage();
        }
    }

    public void previousImage() {
        if (currentIndex > 0) {
            currentIndex--;
            currentRemainingSeconds = config.getSecondsPerImage();
        }
    }

    public boolean isSessionFinished() {
        return currentIndex >= sessionImages.size();
    }

    private void recordCurrentImageCompleted() {
        if (currentIndex < sessionImages.size()) {
            GestureImage current = sessionImages.get(currentIndex);
            if (!completedImages.contains(current)) {
                completedImages.add(current);
            }
        }
    }

    public SessionResult createResult() {
        return new SessionResult(completedImages.size(), totalElapsedSeconds, completedImages);
    }
}
