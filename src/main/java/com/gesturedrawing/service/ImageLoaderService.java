package com.gesturedrawing.service;

import com.gesturedrawing.domain.GestureImage;
import com.gesturedrawing.domain.SessionConfig;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.Set;

/**
 * Service responsible for scanning directory folders or arbitrary file lists and extracting valid image files.
 * Features a Smart Pose Recycling Engine to loop/shuffle poses seamlessly if available images < total target session poses.
 */
public class ImageLoaderService {
    /** Formats JavaFX can decode natively (WebP is not supported by JavaFX 21). */
    public static final List<String> SUPPORTED_EXTENSIONS = List.of("jpg", "jpeg", "png", "gif", "bmp");

    private final Random random;

    public ImageLoaderService() {
        this(new Random());
    }

    /** Allows a seeded {@link Random} for deterministic shuffling in tests. */
    public ImageLoaderService(Random random) {
        this.random = random;
    }

    /** Loads the session poses from whichever source the config uses (folder or explicit files). */
    public List<GestureImage> loadImages(SessionConfig config) {
        return config.getImageDirectory() != null
                ? loadImagesFromDirectory(config.getImageDirectory(), config.getTotalImages())
                : loadImagesFromFiles(config.getSelectedFiles(), config.getTotalImages());
    }

    public List<GestureImage> loadImagesFromDirectory(File folder, int targetCount) {
        if (folder == null || !folder.isDirectory()) {
            return List.of();
        }
        File[] files = folder.listFiles();
        if (files == null) {
            return List.of();
        }
        return loadImagesFromFiles(List.of(files), targetCount);
    }

    public List<GestureImage> loadImagesFromFiles(List<File> files, int targetCount) {
        if (files == null || files.isEmpty() || targetCount <= 0) {
            return List.of();
        }

        // LinkedHashSet drops files listed twice, which would otherwise defeat the no-back-to-back rule
        Set<GestureImage> unique = new LinkedHashSet<>();
        for (File file : files) {
            if (file != null && isSupportedImage(file) && file.isFile()) {
                unique.add(new GestureImage(file));
            }
        }
        List<GestureImage> pool = new ArrayList<>(unique);

        if (pool.isEmpty()) {
            return List.of();
        }

        Collections.shuffle(pool, random);

        // Case 1: Sufficient unique images available -> shuffled slice of exact count
        if (pool.size() >= targetCount) {
            return new ArrayList<>(pool.subList(0, targetCount));
        }

        // Case 2: Fewer images available than requested -> Smart Pose Recycling Loop
        List<GestureImage> recycledList = new ArrayList<>(targetCount);
        recycledList.addAll(pool);

        while (recycledList.size() < targetCount) {
            Collections.shuffle(pool, random);

            // Avoid back-to-back duplicates across loop boundaries if pool has > 1 item
            if (pool.size() > 1 && pool.get(0).equals(recycledList.get(recycledList.size() - 1))) {
                Collections.swap(pool, 0, 1);
            }

            int needed = targetCount - recycledList.size();
            recycledList.addAll(pool.subList(0, Math.min(needed, pool.size())));
        }

        return recycledList;
    }

    public static boolean isSupportedImage(File file) {
        String name = file.getName();
        int lastDotIndex = name.lastIndexOf('.');
        if (lastDotIndex == -1 || lastDotIndex == name.length() - 1) {
            return false;
        }
        return SUPPORTED_EXTENSIONS.contains(name.substring(lastDotIndex + 1).toLowerCase(Locale.ROOT));
    }
}
