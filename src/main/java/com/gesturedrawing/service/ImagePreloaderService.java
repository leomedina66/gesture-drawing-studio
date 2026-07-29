package com.gesturedrawing.service;

import com.gesturedrawing.domain.GestureImage;
import javafx.scene.image.Image;

/**
 * 2-Image Pipeline Preloader (Current N + Next N+1).
 * Guarantees that at most 2 downscaled images exist in heap memory at any given time.
 */
public class ImagePreloaderService {
    private static final double TARGET_MAX_WIDTH = 1920;
    private static final double TARGET_MAX_HEIGHT = 1080;

    private Image currentImage;
    private String currentUri;

    private Image nextImage;
    private String nextUri;

    /**
     * Advances to image N, discards image N-1, and triggers background preloading of image N+1.
     */
    public Image loadAndPipeline(GestureImage currentGestureImg, GestureImage nextGestureImg) {
        if (currentGestureImg == null) {
            clear();
            return null;
        }

        String targetCurrentUri = currentGestureImg.getFileUri();

        // 1. If N+1 was already pre-loaded, promote it to Current (N)
        if (nextImage != null && targetCurrentUri.equals(nextUri)) {
            currentImage = nextImage;
            currentUri = nextUri;
        } else {
            // Otherwise decode downscaled asynchronously
            currentImage = new Image(targetCurrentUri, TARGET_MAX_WIDTH, TARGET_MAX_HEIGHT, true, true, true);
            currentUri = targetCurrentUri;
        }

        // 2. Clear next slot reference
        nextImage = null;
        nextUri = null;

        // 3. Asynchronously trigger background load for Image N+1 (Preload)
        if (nextGestureImg != null) {
            nextUri = nextGestureImg.getFileUri();
            nextImage = new Image(nextUri, TARGET_MAX_WIDTH, TARGET_MAX_HEIGHT, true, true, true);
        }

        return currentImage;
    }

    public void clear() {
        currentImage = null;
        currentUri = null;
        nextImage = null;
        nextUri = null;
    }
}
