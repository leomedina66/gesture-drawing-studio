package com.gesturedrawing.domain;

import java.io.File;

/**
 * Domain model representing configuration settings for a gesture drawing session.
 */
public class SessionConfig {
    private File imageDirectory;
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
        if (secondsPerImage <= 0) {
            throw new IllegalArgumentException("Duration per image must be greater than 0 seconds.");
        }
        if (totalImages <= 0) {
            throw new IllegalArgumentException("Total image count must be greater than 0.");
        }
        this.imageDirectory = imageDirectory;
        this.secondsPerImage = secondsPerImage;
        this.totalImages = totalImages;
        this.defaultFlipped = defaultFlipped;
        this.defaultGrayscale = defaultGrayscale;
    }

    public File getImageDirectory() {
        return imageDirectory;
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
                "directory=" + imageDirectory.getAbsolutePath() +
                ", secondsPerImage=" + secondsPerImage +
                ", totalImages=" + totalImages +
                ", defaultFlipped=" + defaultFlipped +
                ", defaultGrayscale=" + defaultGrayscale +
                '}';
    }
}
