package com.gesturedrawing.domain;

import java.io.File;
import java.util.Objects;

/**
 * Value object representing an image used during a drawing session.
 * Two instances are equal when they point to the same file.
 */
public final class GestureImage {
    private final File file;
    private final String fileName;
    private final String fileUri;

    public GestureImage(File file) {
        if (file == null || !file.exists()) {
            throw new IllegalArgumentException("Image file does not exist.");
        }
        this.file = file;
        this.fileName = file.getName();
        this.fileUri = file.toURI().toString();
    }

    public File getFile() {
        return file;
    }

    public String getFileName() {
        return fileName;
    }

    public String getFileUri() {
        return fileUri;
    }

    @Override
    public boolean equals(Object o) {
        return this == o || (o instanceof GestureImage other && fileUri.equals(other.fileUri));
    }

    @Override
    public int hashCode() {
        return Objects.hash(fileUri);
    }

    @Override
    public String toString() {
        return fileName;
    }
}
