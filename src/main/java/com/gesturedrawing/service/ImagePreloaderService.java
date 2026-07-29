package com.gesturedrawing.service;

import com.gesturedrawing.domain.GestureImage;
import javafx.scene.image.Image;

/**
 * 2-Image Pipeline Preloader (Current N + Next N+1).
 * Decodes images at full native resolution in the background while strictly capping
 * memory to at most 2 images in RAM at any given moment.
 */
public class ImagePreloaderService {
    private Image currentImage;
    private String currentUri;

    private Image nextImage;
    private String nextUri;

    /**
     * Advances to image N at full native resolution, discards image N-1, and triggers background preloading of image N+1.
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
            // Decode at full native resolution in background (0, 0 requested dimensions)
            currentImage = new Image(targetCurrentUri, 0, 0, true, true, true);
            currentUri = targetCurrentUri;
        }

        // 2. Clear next slot reference
        nextImage = null;
        nextUri = null;

        // 3. Asynchronously trigger background pre-load for Image N+1 at full native resolution
        if (nextGestureImg != null) {
            nextUri = nextGestureImg.getFileUri();
            nextImage = new Image(nextUri, 0, 0, true, true, true);
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
