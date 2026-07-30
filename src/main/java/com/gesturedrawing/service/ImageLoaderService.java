package com.gesturedrawing.service;

import com.gesturedrawing.domain.GestureImage;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * Service responsible for scanning directory folders or arbitrary file lists and extracting valid image files.
 * Features a Smart Pose Recycling Engine to loop/shuffle poses seamlessly if available images < total target session poses.
 */
public class ImageLoaderService {
    private static final Set<String> SUPPORTED_EXTENSIONS = Set.of("jpg", "jpeg", "png", "webp", "gif", "bmp");

    public List<GestureImage> loadImagesFromDirectory(File folder, int targetCount) {
        if (folder == null || !folder.exists() || !folder.isDirectory()) {
            return Collections.emptyList();
        }
        File[] files = folder.listFiles();
        if (files == null) {
            return Collections.emptyList();
        }
        return loadImagesFromFiles(List.of(files), targetCount);
    }

    public List<GestureImage> loadImagesFromFiles(List<File> files, int targetCount) {
        if (files == null || files.isEmpty()) {
            return Collections.emptyList();
        }

        List<GestureImage> validImages = new ArrayList<>();
        for (File file : files) {
            if (file != null && file.isFile() && isSupportedImage(file)) {
                validImages.add(new GestureImage(file));
            }
        }

        if (validImages.isEmpty()) {
            return Collections.emptyList();
        }

        // Case 1: Sufficient unique images available -> shuffle and slice exact count
        if (validImages.size() >= targetCount) {
            List<GestureImage> shuffled = new ArrayList<>(validImages);
            Collections.shuffle(shuffled);
            return shuffled.subList(0, targetCount);
        }

        // Case 2: Fewer images available than requested -> Smart Pose Recycling Loop
        List<GestureImage> recycledList = new ArrayList<>();
        List<GestureImage> pool = new ArrayList<>(validImages);

        while (recycledList.size() < targetCount) {
            Collections.shuffle(pool);

            // Avoid back-to-back duplicates across loop boundaries if pool has > 1 item
            if (!recycledList.isEmpty() && pool.size() > 1) {
                GestureImage lastAdded = recycledList.get(recycledList.size() - 1);
                if (pool.get(0).equals(lastAdded)) {
                    Collections.swap(pool, 0, 1);
                }
            }

            for (GestureImage img : pool) {
                recycledList.add(img);
                if (recycledList.size() == targetCount) {
                    break;
                }
            }
        }

        return recycledList;
    }

    private boolean isSupportedImage(File file) {
        String name = file.getName().toLowerCase();
        int lastDotIndex = name.lastIndexOf('.');
        if (lastDotIndex == -1 || lastDotIndex == name.length() - 1) {
            return false;
        }
        String extension = name.substring(lastDotIndex + 1);
        return SUPPORTED_EXTENSIONS.contains(extension);
    }
}
