package com.gesturedrawing.service;

import com.gesturedrawing.domain.GestureImage;
import javafx.scene.image.Image;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Sliding-window image cache (Previous N-1, Current N, Next N+1).
 * Images are decoded in the background. An image larger than the display cap is decoded
 * downscaled to that cap (still pin-sharp on screen); smaller images keep their native resolution.
 * Note: JavaFX only decodes JPEG at reduced size — PNG/GIF/BMP are decoded at full size first —
 * so images whose header claims more than {@link #MAX_PIXELS} are rejected up front instead
 * (a 1 MB "decompression bomb" PNG can otherwise demand gigabytes of heap).
 * Going back one pose reuses the cached image instead of decoding it again.
 */
public class ImagePreloaderService {
    /** 100 megapixels: well above any camera photo, far below what would exhaust the heap. */
    public static final long MAX_PIXELS = 100_000_000L;

    private final double maxWidth;
    private final double maxHeight;
    private Map<String, Image> cache = new HashMap<>();

    /**
     * @param maxWidth  largest useful decode width in physical pixels (e.g. biggest screen width x output scale)
     * @param maxHeight largest useful decode height in physical pixels
     */
    public ImagePreloaderService(double maxWidth, double maxHeight) {
        this.maxWidth = maxWidth;
        this.maxHeight = maxHeight;
    }

    /**
     * Returns the image for {@code current} (or {@code null} if it is too large to decode safely),
     * starts preloading {@code next}, keeps {@code previous} only if it is already cached,
     * and releases everything else.
     */
    public Image loadAndPipeline(GestureImage previous, GestureImage current, GestureImage next) {
        if (current == null) {
            clear();
            return null;
        }

        Map<String, Image> window = new HashMap<>(4);
        Image cachedPrevious = previous != null ? cache.get(previous.getFileUri()) : null;
        if (cachedPrevious != null && !cachedPrevious.isError()) {
            window.put(previous.getFileUri(), cachedPrevious);
        }
        Image currentImage = obtain(current, window);
        if (next != null) {
            obtain(next, window);
        }
        cache = window;
        return currentImage;
    }

    private Image obtain(GestureImage gestureImage, Map<String, Image> window) {
        // computeIfAbsent stores nothing when decode() returns null (rejected image)
        return window.computeIfAbsent(gestureImage.getFileUri(), uri -> {
            Image cached = cache.get(uri);
            return cached != null && !cached.isError() ? cached : decode(gestureImage);
        });
    }

    private Image decode(GestureImage gestureImage) {
        int[] size = readDimensions(gestureImage.getFile());
        if (isTooLarge(size)) {
            return null;
        }
        boolean fitsNatively = size != null && size[0] <= maxWidth && size[1] <= maxHeight;
        if (fitsNatively) {
            return new Image(gestureImage.getFileUri(), 0, 0, true, true, true);
        }
        return new Image(gestureImage.getFileUri(), maxWidth, maxHeight, true, true, true);
    }

    static boolean isTooLarge(int[] size) {
        return size != null && (long) size[0] * size[1] > MAX_PIXELS;
    }

    /** Reads only the image header to get its pixel size; returns null when the format can't be probed. */
    static int[] readDimensions(File file) {
        try (ImageInputStream in = ImageIO.createImageInputStream(file)) {
            if (in == null) {
                return null;
            }
            Iterator<ImageReader> readers = ImageIO.getImageReaders(in);
            if (!readers.hasNext()) {
                return null;
            }
            ImageReader reader = readers.next();
            try {
                reader.setInput(in, true, true);
                return new int[]{reader.getWidth(0), reader.getHeight(0)};
            } finally {
                reader.dispose();
            }
        } catch (IOException | RuntimeException ex) {
            return null;
        }
    }

    public void clear() {
        cache = new HashMap<>();
    }
}
