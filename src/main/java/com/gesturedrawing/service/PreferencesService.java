package com.gesturedrawing.service;

import java.io.File;
import java.util.prefs.Preferences;

/**
 * Service to manage persistent user preferences (e.g. last selected image folder, session timer, window size, visual filter defaults).
 */
public class PreferencesService {
    private static final String KEY_LAST_FOLDER = "LAST_IMAGE_FOLDER";
    private static final String KEY_LAST_SECONDS = "LAST_SECONDS_PER_IMAGE";
    private static final String KEY_LAST_COUNT = "LAST_TOTAL_IMAGES";
    private static final String KEY_WINDOW_WIDTH = "WINDOW_WIDTH";
    private static final String KEY_WINDOW_HEIGHT = "WINDOW_HEIGHT";
    private static final String KEY_WINDOW_X = "WINDOW_X";
    private static final String KEY_WINDOW_Y = "WINDOW_Y";
    private static final String KEY_WINDOW_MAXIMIZED = "WINDOW_MAXIMIZED";
    private static final String KEY_ALWAYS_ON_TOP = "ALWAYS_ON_TOP";
    private static final String KEY_DEFAULT_FLIPPED = "DEFAULT_FLIPPED";
    private static final String KEY_DEFAULT_GRAYSCALE = "DEFAULT_GRAYSCALE";

    private final Preferences preferences = Preferences.userNodeForPackage(PreferencesService.class);

    public void saveLastFolder(File folder) {
        if (folder != null && folder.exists() && folder.isDirectory()) {
            preferences.put(KEY_LAST_FOLDER, folder.getAbsolutePath());
        }
    }

    public File getLastFolder() {
        String path = preferences.get(KEY_LAST_FOLDER, null);
        if (path != null) {
            File folder = new File(path);
            if (folder.exists() && folder.isDirectory()) {
                return folder;
            }
        }
        return null;
    }

    public void saveLastSessionSettings(int secondsPerImage, int totalImages) {
        preferences.putInt(KEY_LAST_SECONDS, secondsPerImage);
        preferences.putInt(KEY_LAST_COUNT, totalImages);
    }

    public int getLastSecondsPerImage(int defaultValue) {
        return preferences.getInt(KEY_LAST_SECONDS, defaultValue);
    }

    public int getLastTotalImages(int defaultValue) {
        return preferences.getInt(KEY_LAST_COUNT, defaultValue);
    }

    public void saveWindowBounds(double width, double height, double x, double y, boolean isMaximized) {
        preferences.putDouble(KEY_WINDOW_WIDTH, width);
        preferences.putDouble(KEY_WINDOW_HEIGHT, height);
        preferences.putDouble(KEY_WINDOW_X, x);
        preferences.putDouble(KEY_WINDOW_Y, y);
        preferences.putBoolean(KEY_WINDOW_MAXIMIZED, isMaximized);
    }

    public double getWindowWidth(double defaultValue) {
        return preferences.getDouble(KEY_WINDOW_WIDTH, defaultValue);
    }

    public double getWindowHeight(double defaultValue) {
        return preferences.getDouble(KEY_WINDOW_HEIGHT, defaultValue);
    }

    public double getWindowX(double defaultValue) {
        return preferences.getDouble(KEY_WINDOW_X, defaultValue);
    }

    public double getWindowY(double defaultValue) {
        return preferences.getDouble(KEY_WINDOW_Y, defaultValue);
    }

    public boolean isWindowMaximized(boolean defaultValue) {
        return preferences.getBoolean(KEY_WINDOW_MAXIMIZED, defaultValue);
    }

    public void saveAlwaysOnTop(boolean alwaysOnTop) {
        preferences.putBoolean(KEY_ALWAYS_ON_TOP, alwaysOnTop);
    }

    public boolean isAlwaysOnTop(boolean defaultValue) {
        return preferences.getBoolean(KEY_ALWAYS_ON_TOP, defaultValue);
    }

    public void saveDefaultFlipped(boolean flipped) {
        preferences.putBoolean(KEY_DEFAULT_FLIPPED, flipped);
    }

    public boolean isDefaultFlipped(boolean defaultValue) {
        return preferences.getBoolean(KEY_DEFAULT_FLIPPED, defaultValue);
    }

    public void saveDefaultGrayscale(boolean grayscale) {
        preferences.putBoolean(KEY_DEFAULT_GRAYSCALE, grayscale);
    }

    public boolean isDefaultGrayscale(boolean defaultValue) {
        return preferences.getBoolean(KEY_DEFAULT_GRAYSCALE, defaultValue);
    }
}
