package com.gesturedrawing.domain;

import java.util.Collections;
import java.util.List;

/**
 * Domain model summarizing the outcome of a completed gesture drawing session.
 */
public class SessionResult {
    private final int completedImagesCount;
    private final int totalSecondsElapsed;
    private final List<GestureImage> imagesUsed;

    public SessionResult(int completedImagesCount, int totalSecondsElapsed, List<GestureImage> imagesUsed) {
        this.completedImagesCount = completedImagesCount;
        this.totalSecondsElapsed = totalSecondsElapsed;
        this.imagesUsed = imagesUsed != null ? List.copyOf(imagesUsed) : Collections.emptyList();
    }

    public int getCompletedImagesCount() {
        return completedImagesCount;
    }

    public int getTotalSecondsElapsed() {
        return totalSecondsElapsed;
    }

    public List<GestureImage> getImagesUsed() {
        return imagesUsed;
    }

    public String getFormattedTotalTime() {
        int minutes = totalSecondsElapsed / 60;
        int seconds = totalSecondsElapsed % 60;
        if (minutes > 0) {
            return String.format("%d min %d sec", minutes, seconds);
        }
        return String.format("%d seconds", seconds);
    }
}
