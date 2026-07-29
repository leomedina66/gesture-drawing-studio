package com.gesturedrawing.service;

import com.gesturedrawing.domain.GestureImage;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * Service responsible for scanning directory folders and extracting valid image files.
 */
public class ImageLoaderService {
    private static final Set<String> SUPPORTED_EXTENSIONS = Set.of("jpg", "jpeg", "png", "webp", "gif", "bmp");

    public List<GestureImage> loadImagesFromDirectory(File folder, int maxImagesRequested) {
        if (folder == null || !folder.exists() || !folder.isDirectory()) {
            return Collections.emptyList();
        }

        File[] files = folder.listFiles();
        if (files == null) {
            return Collections.emptyList();
        }

        List<GestureImage> validImages = new ArrayList<>();
        for (File file : files) {
            if (file.isFile() && isSupportedImage(file)) {
                validImages.add(new GestureImage(file));
            }
        }

        // Shuffle images to guarantee random order without repetitions
        Collections.shuffle(validImages);

        // Limit to total requested images if validImages is larger
        if (maxImagesRequested > 0 && validImages.size() > maxImagesRequested) {
            return validImages.subList(0, maxImagesRequested);
        }

        return validImages;
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
