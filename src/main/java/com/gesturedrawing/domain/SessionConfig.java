package com.gesturedrawing.domain;

import java.io.File;
import java.util.List;

/**
 * Domain model representing configuration settings for a gesture drawing session.
 * Supports loading reference artwork from either a whole Directory or a List of specific Files.
 * Exactly one of {@link #getImageDirectory()} / {@link #getSelectedFiles()} is the source.
 * The session is a sequence of {@link SessionPhase phases}; a simple session has one phase.
 */
public final class SessionConfig {
    private final File imageDirectory;
    private final List<File> selectedFiles;
    private final List<SessionPhase> phases;
    private final boolean defaultFlipped;
    private final boolean defaultGrayscale;

    public SessionConfig(File imageDirectory, int secondsPerImage, int totalImages) {
        this(imageDirectory, secondsPerImage, totalImages, false, false);
    }

    public SessionConfig(File imageDirectory, int secondsPerImage, int totalImages, boolean defaultFlipped, boolean defaultGrayscale) {
        this(imageDirectory, List.of(new SessionPhase(totalImages, secondsPerImage)), defaultFlipped, defaultGrayscale);
    }

    public SessionConfig(List<File> selectedFiles, int secondsPerImage, int totalImages, boolean defaultFlipped, boolean defaultGrayscale) {
        this(selectedFiles, List.of(new SessionPhase(totalImages, secondsPerImage)), defaultFlipped, defaultGrayscale);
    }

    public SessionConfig(File imageDirectory, List<SessionPhase> phases, boolean defaultFlipped, boolean defaultGrayscale) {
        if (imageDirectory == null || !imageDirectory.isDirectory()) {
            throw new IllegalArgumentException("Invalid directory path provided.");
        }
        this.imageDirectory = imageDirectory;
        this.selectedFiles = List.of();
        this.phases = validatePhases(phases);
        this.defaultFlipped = defaultFlipped;
        this.defaultGrayscale = defaultGrayscale;
    }

    public SessionConfig(List<File> selectedFiles, List<SessionPhase> phases, boolean defaultFlipped, boolean defaultGrayscale) {
        if (selectedFiles == null || selectedFiles.isEmpty()) {
            throw new IllegalArgumentException("No image files selected.");
        }
        this.imageDirectory = null;
        this.selectedFiles = List.copyOf(selectedFiles);
        this.phases = validatePhases(phases);
        this.defaultFlipped = defaultFlipped;
        this.defaultGrayscale = defaultGrayscale;
    }

    private static List<SessionPhase> validatePhases(List<SessionPhase> phases) {
        if (phases == null || phases.isEmpty()) {
            throw new IllegalArgumentException("A session needs at least one phase.");
        }
        List<SessionPhase> copy = List.copyOf(phases);
        // long sum: a long list of phases can't overflow into a "valid" negative total
        if (copy.stream().mapToLong(SessionPhase::poseCount).sum() > SessionPhase.MAX_POSES) {
            throw new IllegalArgumentException("A session can have at most " + SessionPhase.MAX_POSES + " poses.");
        }
        return copy;
    }

    /** Source folder, or {@code null} when the session uses an explicit file selection. */
    public File getImageDirectory() {
        return imageDirectory;
    }

    /** Explicitly selected files (immutable); empty when the session uses a folder. */
    public List<File> getSelectedFiles() {
        return selectedFiles;
    }

    /** Session phases in play order (immutable, never empty). */
    public List<SessionPhase> getPhases() {
        return phases;
    }

    /** Total poses across all phases. */
    public int getTotalImages() {
        return phases.stream().mapToInt(SessionPhase::poseCount).sum();
    }

    /** Planned session length if every pose runs its full time. */
    public int getTotalSeconds() {
        return phases.stream().mapToInt(SessionPhase::totalSeconds).sum();
    }

    public boolean isDefaultFlipped() {
        return defaultFlipped;
    }

    public boolean isDefaultGrayscale() {
        return defaultGrayscale;
    }

    @Override
    public String toString() {
        return "SessionConfig{" +
                "directory=" + (imageDirectory != null ? imageDirectory.getAbsolutePath() : "Custom File Selection (" + selectedFiles.size() + " files)") +
                ", phases=" + phases +
                ", defaultFlipped=" + defaultFlipped +
                ", defaultGrayscale=" + defaultGrayscale +
                '}';
    }
}
