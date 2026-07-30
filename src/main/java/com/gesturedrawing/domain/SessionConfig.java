package com.gesturedrawing.domain;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Domain model representing configuration settings for a gesture drawing session.
 * Supports loading reference artwork from either a whole Directory or a List of specific Files.
 */
public class SessionConfig {
    private File imageDirectory;
    private List<File> selectedFiles;
    private int secondsPerImage;
    private int totalImages;
    private boolean defaultFlipped;
    private boolean defaultGrayscale;

    public SessionConfig(File imageDirectory, int secondsPerImage, int totalImages) {
        this(imageDirectory, secondsPerImage, totalImages, false, false);
    }

    public SessionConfig(File imageDirectory, int secondsPerImage, int totalImages, boolean defaultFlipped, boolean defaultGrayscale) {
        if (imageDirectory == null || !imageDirectory.exists() || !imageDirectory.isDirectory()) {
            throw new IllegalArgumentException("Invalid directory path provided.");
        }
        validateNumbers(secondsPerImage, totalImages);
        this.imageDirectory = imageDirectory;
        this.selectedFiles = scanDirectory(imageDirectory);
        this.secondsPerImage = secondsPerImage;
        this.totalImages = totalImages;
        this.defaultFlipped = defaultFlipped;
        this.defaultGrayscale = defaultGrayscale;
    }

    public SessionConfig(List<File> selectedFiles, int secondsPerImage, int totalImages, boolean defaultFlipped, boolean defaultGrayscale) {
        if (selectedFiles == null || selectedFiles.isEmpty()) {
            throw new IllegalArgumentException("No image files selected.");
        }
        validateNumbers(secondsPerImage, totalImages);
        this.imageDirectory = null;
        this.selectedFiles = new ArrayList<>(selectedFiles);
        this.secondsPerImage = secondsPerImage;
        this.totalImages = totalImages;
        this.defaultFlipped = defaultFlipped;
        this.defaultGrayscale = defaultGrayscale;
    }

    private void validateNumbers(int secondsPerImage, int totalImages) {
        if (secondsPerImage <= 0) {
            throw new IllegalArgumentException("Duration per image must be greater than 0 seconds.");
        }
        if (totalImages <= 0) {
            throw new IllegalArgumentException("Total image count must be greater than 0.");
        }
    }

    private List<File> scanDirectory(File dir) {
        File[] files = dir.listFiles();
        if (files == null) {
            return Collections.emptyList();
        }
        List<File> list = new ArrayList<>();
        for (File f : files) {
            if (f.isFile()) {
                list.add(f);
            }
        }
        return list;
    }

    public File getImageDirectory() {
        return imageDirectory;
    }

    public List<File> getSelectedFiles() {
        return selectedFiles;
    }

    public int getSecondsPerImage() {
        return secondsPerImage;
    }

    public int getTotalImages() {
        return totalImages;
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
                ", secondsPerImage=" + secondsPerImage +
                ", totalImages=" + totalImages +
                ", defaultFlipped=" + defaultFlipped +
                ", defaultGrayscale=" + defaultGrayscale +
                '}';
    }
}
