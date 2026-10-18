package com.gesturedrawing.domain;

import java.util.List;

/**
 * Domain model summarizing the outcome of a completed gesture drawing session.
 * {@code completedPosesCount} counts poses (recycled images count once per appearance),
 * while {@code imagesUsed} holds each distinct image once, in practice order, for the gallery.
 */
public final class SessionResult {
    private final int completedPosesCount;
    private final int totalSecondsElapsed;
    private final List<GestureImage> imagesUsed;

    public SessionResult(int completedPosesCount, int totalSecondsElapsed, List<GestureImage> imagesUsed) {
        this.completedPosesCount = completedPosesCount;
        this.totalSecondsElapsed = totalSecondsElapsed;
        this.imagesUsed = imagesUsed != null ? List.copyOf(imagesUsed) : List.of();
    }

    public int getCompletedImagesCount() {
        return completedPosesCount;
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
