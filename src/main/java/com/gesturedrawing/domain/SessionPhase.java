package com.gesturedrawing.domain;

import java.util.ArrayList;
import java.util.List;

/**
 * One block of a phased session: {@code poseCount} poses of {@code secondsPerPose} each
 * (e.g. "5 poses of 2 min"). A simple session is a single phase.
 * Limits are enforced here so every source (UI, saved preferences, future presets) obeys them.
 */
public record SessionPhase(int poseCount, int secondsPerPose) {
    public static final int MAX_POSES = 500;
    public static final int MIN_SECONDS = 5;
    public static final int MAX_SECONDS = 3600;

    public SessionPhase {
        if (secondsPerPose < MIN_SECONDS || secondsPerPose > MAX_SECONDS) {
            throw new IllegalArgumentException("Duration per pose must be between " + MIN_SECONDS + " and " + MAX_SECONDS + " seconds.");
        }
        if (poseCount <= 0 || poseCount > MAX_POSES) {
            throw new IllegalArgumentException("Pose count must be between 1 and " + MAX_POSES + ".");
        }
    }

    public int totalSeconds() {
        return poseCount * secondsPerPose;
    }

    /** Human label such as "5 × 2 min". */
    @Override
    public String toString() {
        return poseCount + " × " + formatDuration(secondsPerPose);
    }

    /** "30s", "2 min", "1 min 30s", "1 h 5 min". */
    public static String formatDuration(int seconds) {
        int h = seconds / 3600;
        int m = (seconds % 3600) / 60;
        int s = seconds % 60;
        StringBuilder sb = new StringBuilder();
        if (h > 0) {
            sb.append(h).append(" h");
        }
        if (m > 0) {
            sb.append(sb.isEmpty() ? "" : " ").append(m).append(" min");
        }
        if (s > 0 || sb.isEmpty()) {
            sb.append(sb.isEmpty() ? "" : " ").append(s).append('s');
        }
        return sb.toString();
    }

    /** Serializes phases as "2x60;5x120;1x600" (pose count x seconds), e.g. for preferences. */
    public static String serialize(List<SessionPhase> phases) {
        StringBuilder sb = new StringBuilder();
        for (SessionPhase phase : phases) {
            if (!sb.isEmpty()) {
                sb.append(';');
            }
            sb.append(phase.poseCount).append('x').append(phase.secondsPerPose);
        }
        return sb.toString();
    }

    /**
     * Parses {@link #serialize} output; malformed or out-of-range entries are ignored, and entries
     * that would push the plan past {@link #MAX_POSES} poses in total are dropped, so the result
     * always forms a valid {@link SessionConfig} (when non-empty).
     */
    public static List<SessionPhase> parse(String text) {
        List<SessionPhase> phases = new ArrayList<>();
        if (text == null || text.isBlank()) {
            return phases;
        }
        int totalPoses = 0;
        for (String entry : text.split(";")) {
            String[] parts = entry.trim().split("[xX]");
            if (parts.length != 2) {
                continue;
            }
            try {
                SessionPhase phase = new SessionPhase(Integer.parseInt(parts[0].trim()), Integer.parseInt(parts[1].trim()));
                if (totalPoses + phase.poseCount() <= MAX_POSES) {
                    phases.add(phase);
                    totalPoses += phase.poseCount();
                }
            } catch (IllegalArgumentException ignored) {
                // NumberFormatException is an IllegalArgumentException too
            }
        }
        return phases;
    }
}
