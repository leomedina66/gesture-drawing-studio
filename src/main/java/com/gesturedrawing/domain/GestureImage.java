package com.gesturedrawing.domain;

import java.io.File;

/**
 * Value object representing an image used during a drawing session.
 */
public class GestureImage {
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
}
