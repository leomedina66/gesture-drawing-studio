package com.gesturedrawing.ui;

import com.gesturedrawing.domain.GestureImage;
import com.gesturedrawing.domain.SessionConfig;
import com.gesturedrawing.domain.SessionPhase;
import com.gesturedrawing.domain.SessionResult;
import com.gesturedrawing.service.ImagePreloaderService;
import com.gesturedrawing.service.PreferencesService;
import com.gesturedrawing.service.SessionManager;
import javafx.animation.Animation;
import javafx.animation.FadeTransition;
import javafx.animation.KeyFrame;
import javafx.animation.PauseTransition;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.css.PseudoClass;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.ContextMenu;
import javafx.scene.effect.ColorAdjust;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.*;
import javafx.scene.text.Text;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.util.EnumSet;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Active gesture session viewer screen featuring 100% window height canvas, discrete floating timer,
 * temporary auto-fading "Pose X of Y" toast notification, auto-hiding control bar, and a
 * prev/current/next image cache.
 */
public class SessionScreen extends StackPane {
    private static final PseudoClass WARNING = PseudoClass.getPseudoClass("warning");
    private static final PseudoClass CRITICAL = PseudoClass.getPseudoClass("critical");
    private static final PseudoClass PAUSED = PseudoClass.getPseudoClass("paused");

    private final Stage stage;
    private final SessionManager sessionManager;
    private final Consumer<SessionResult> onSessionComplete;
    private final PreferencesService preferencesService;
    private final ImagePreloaderService preloaderService;
    private final ImageView imageView = new ImageView();
    private final Text timerText = new Text();
    private final Text phaseText = new Text();
    private final Button pauseBtn = new Button("Pause (Space)");
    private final HBox controlBar = new HBox(8);

    private final HBox poseToast = new HBox();
    private final Text poseToastText = new Text();

    private final CheckMenuItem alwaysOnTopItem = new CheckMenuItem("Always on Top (T)");
    private final CheckMenuItem flipItem = new CheckMenuItem("Flip Horizontal (H)");
    private final CheckMenuItem grayscaleItem = new CheckMenuItem("Grayscale / Black & White (G)");

    /** Toggle keys currently held down, so key auto-repeat doesn't flip them on and off. */
    private final Set<KeyCode> heldToggleKeys = EnumSet.noneOf(KeyCode.class);
    private static final Set<KeyCode> TOGGLE_KEYS = EnumSet.of(KeyCode.SPACE, KeyCode.T, KeyCode.H, KeyCode.G);

    private final Timeline timeline;
    /** The single image currently watched for async load errors (one listener, moved between images). */
    private Image watchedImage;
    private final ChangeListener<Boolean> errorListener = this::onWatchedImageError;
    private boolean cleanedUp = false;
    private boolean lastMoveForward = true;

    private boolean isFlipped;
    private boolean isGrayscale;
    private final ColorAdjust grayscaleAdjust = new ColorAdjust();

    private final PauseTransition inactivityTimer;
    private final FadeTransition controlFadeIn;
    private final FadeTransition controlFadeOut;
    private final FadeTransition toastFadeIn;
    private final FadeTransition toastFadeOut;

    public SessionScreen(Stage stage, SessionConfig config, SessionManager sessionManager,
                         PreferencesService preferencesService, Consumer<SessionResult> onSessionComplete) {
        this.stage = stage;
        this.sessionManager = sessionManager;
        this.onSessionComplete = onSessionComplete;
        this.preferencesService = preferencesService;
        this.isFlipped = config.isDefaultFlipped();
        this.isGrayscale = config.isDefaultGrayscale();
        this.grayscaleAdjust.setSaturation(-1.0);
        this.preloaderService = createPreloader();

        getStyleClass().add("screen-root");
        setFocusTraversable(true);

        // 1. CENTER AREA: 100% Window Height Resizable Image Container
        ResizableImagePane imageContainer = new ResizableImagePane(imageView);

        // 2. DISCRETE FLOATING TIMER BADGE OVERLAY (Top-Left Corner)
        HBox timerBadge = new HBox(10);
        timerBadge.setAlignment(Pos.CENTER);
        timerBadge.getStyleClass().add("timer-badge");
        timerBadge.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        timerBadge.setMouseTransparent(true);

        timerText.getStyleClass().add("timer-text");
        timerBadge.getChildren().add(timerText);
        if (sessionManager.getPhaseCount() > 1) {
            phaseText.getStyleClass().add("phase-badge");
            timerBadge.getChildren().add(phaseText);
        }

        // 3. TEMPORARY "POSE X OF Y" TOAST NOTIFICATION OVERLAY (Top-Center)
        poseToast.setAlignment(Pos.CENTER);
        poseToast.getStyleClass().add("pose-toast");
        poseToast.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        poseToast.setMouseTransparent(true); // Allow mouse clicks to pass straight through

        poseToastText.getStyleClass().add("pose-toast-text");
        poseToast.getChildren().add(poseToastText);

        // 4. FLOATING AUTO-HIDE CONTROL BAR OVERLAY (Bottom-Center)
        controlBar.setAlignment(Pos.CENTER);
        controlBar.getStyleClass().add("floating-control-bar");
        controlBar.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);

        Button prevBtn = createControl(new Button("⏮ Prev"), "button-secondary", this::previous);
        createControl(pauseBtn, "button-secondary", this::togglePause);
        Button nextBtn = createControl(new Button("Next ⏭"), "button-secondary", this::next);
        Button stopBtn = createControl(new Button("Finish ⏹"), "button-danger", this::finishSession);
        controlBar.getChildren().addAll(prevBtn, pauseBtn, nextBtn, stopBtn);

        // StackPane Layer Hierarchy
        getChildren().addAll(imageContainer, timerBadge, poseToast, controlBar);
        StackPane.setAlignment(timerBadge, Pos.TOP_LEFT);
        StackPane.setMargin(timerBadge, new Insets(12, 0, 0, 14));

        StackPane.setAlignment(poseToast, Pos.TOP_CENTER);
        StackPane.setMargin(poseToast, new Insets(14, 0, 0, 0));

        StackPane.setAlignment(controlBar, Pos.BOTTOM_CENTER);
        StackPane.setMargin(controlBar, new Insets(0, 0, 14, 0));

        // Auto-Hide Timers & Fade Transitions
        inactivityTimer = new PauseTransition(Duration.seconds(2.5));
        inactivityTimer.setOnFinished(e -> {
            if (!controlBar.isHover()) {
                hideOverlays();
            }
        });

        controlFadeIn = new FadeTransition(Duration.millis(200), controlBar);
        controlFadeOut = new FadeTransition(Duration.millis(400), controlBar);

        toastFadeIn = new FadeTransition(Duration.millis(200), poseToast);
        toastFadeOut = new FadeTransition(Duration.millis(400), poseToast);

        // Control bar mouse hover listeners
        controlBar.setOnMouseEntered(e -> {
            showOverlays();
            inactivityTimer.stop();
        });
        controlBar.setOnMouseExited(e -> inactivityTimer.playFromStart());

        // Mouse activity listeners on root SessionScreen
        setOnMouseMoved(e -> showOverlays());
        setOnMouseEntered(e -> showOverlays());
        setOnMouseExited(e -> {
            if (!controlBar.isHover()) {
                inactivityTimer.playFromStart();
            }
        });
        // Clicking the image area keeps keyboard focus on the session
        setOnMousePressed(e -> requestFocus());

        // 5. CONTEXT MENU (Right Click)
        alwaysOnTopItem.setOnAction(e -> setAlwaysOnTop(alwaysOnTopItem.isSelected()));
        flipItem.setOnAction(e -> setFlipped(flipItem.isSelected()));
        grayscaleItem.setOnAction(e -> setGrayscale(grayscaleItem.isSelected()));

        ContextMenu contextMenu = new ContextMenu(alwaysOnTopItem, flipItem, grayscaleItem);
        setOnContextMenuRequested(event -> {
            alwaysOnTopItem.setSelected(stage.isAlwaysOnTop());
            contextMenu.show(this, event.getScreenX(), event.getScreenY());
        });

        // 6. KEYBOARD SHORTCUTS: an event filter runs before any focused control,
        // so Space/arrows always drive the session instead of "clicking" a focused button.
        addEventFilter(KeyEvent.KEY_PRESSED, this::handleShortcut);
        addEventFilter(KeyEvent.KEY_RELEASED, event -> heldToggleKeys.remove(event.getCode()));

        timeline = new Timeline(new KeyFrame(Duration.seconds(1), event -> onTick()));
        timeline.setCycleCount(Animation.INDEFINITE);

        // Initialize display and start timer loop
        applyVisualFilters();
        updateDisplay();
        timeline.play();
    }

    /** Decode cap = largest connected screen in physical pixels, so images stay pin-sharp at full screen. */
    private static ImagePreloaderService createPreloader() {
        double maxW = 1920;
        double maxH = 1080;
        for (Screen screen : Screen.getScreens()) {
            Rectangle2D bounds = screen.getBounds();
            maxW = Math.max(maxW, bounds.getWidth() * screen.getOutputScaleX());
            maxH = Math.max(maxH, bounds.getHeight() * screen.getOutputScaleY());
        }
        return new ImagePreloaderService(maxW, maxH);
    }

    private Button createControl(Button button, String styleClass, Runnable action) {
        button.getStyleClass().add(styleClass);
        button.setFocusTraversable(false);
        button.setOnAction(e -> action.run());
        return button;
    }

    private void handleShortcut(KeyEvent event) {
        // Leave Ctrl/Alt/Win combinations (e.g. Alt+F4) to the system
        if (event.isControlDown() || event.isAltDown() || event.isMetaDown() || event.isShortcutDown()) {
            return;
        }
        // Holding a toggle key fires repeated KEY_PRESSED events; act only on the first one
        if (TOGGLE_KEYS.contains(event.getCode()) && !heldToggleKeys.add(event.getCode())) {
            event.consume();
            return;
        }
        Runnable action = switch (event.getCode()) {
            case SPACE -> this::togglePause;
            case RIGHT -> this::next;
            case LEFT -> this::previous;
            case ESCAPE -> this::finishSession;
            case T -> () -> setAlwaysOnTop(!stage.isAlwaysOnTop());
            case H -> () -> setFlipped(!isFlipped);
            case G -> () -> setGrayscale(!isGrayscale);
            default -> null;
        };
        showOverlays();
        if (action != null) {
            event.consume();
            action.run();
        }
    }

    // --- Session actions (shared by buttons, keyboard shortcuts and the context menu) ---

    private void previous() {
        lastMoveForward = false;
        sessionManager.previousImage();
        onPoseChanged();
    }

    private void next() {
        lastMoveForward = true;
        sessionManager.skipNext();
        onPoseChanged();
    }

    private void onTick() {
        if (sessionManager.tickSecond()) {
            lastMoveForward = true;
            onPoseChanged();
        } else {
            updateTimerText();
        }
    }

    /** Restarts the 1s cadence so every new pose gets its full duration, then refreshes the view. */
    private void onPoseChanged() {
        if (cleanedUp) {
            return;
        }
        if (sessionManager.isSessionFinished()) {
            finishSession();
            return;
        }
        if (sessionManager.isPaused()) {
            timeline.stop(); // resume will then start a fresh 1s cycle
        } else {
            timeline.playFromStart();
        }
        updateDisplay();
    }

    private void togglePause() {
        sessionManager.togglePause();
        boolean paused = sessionManager.isPaused();
        if (paused) {
            timeline.pause();
        } else {
            timeline.play();
        }
        pauseBtn.setText(paused ? "Resume (Space)" : "Pause (Space)");
        pauseBtn.pseudoClassStateChanged(PAUSED, paused);
        timerText.pseudoClassStateChanged(PAUSED, paused);
    }

    private void setAlwaysOnTop(boolean onTop) {
        stage.setAlwaysOnTop(onTop);
        preferencesService.saveAlwaysOnTop(onTop);
        alwaysOnTopItem.setSelected(onTop);
    }

    private void setFlipped(boolean flipped) {
        isFlipped = flipped;
        applyVisualFilters();
    }

    private void setGrayscale(boolean grayscale) {
        isGrayscale = grayscale;
        applyVisualFilters();
    }

    private void applyVisualFilters() {
        imageView.setScaleX(isFlipped ? -1.0 : 1.0);
        imageView.setEffect(isGrayscale ? grayscaleAdjust : null);
        flipItem.setSelected(isFlipped);
        grayscaleItem.setSelected(isGrayscale);
    }

    // --- Overlays ---

    private void showOverlays() {
        controlFadeOut.stop();
        toastFadeOut.stop();
        fadeTo(controlBar, controlFadeIn, 1.0);
        fadeTo(poseToast, toastFadeIn, 1.0);
        inactivityTimer.playFromStart();
    }

    private void hideOverlays() {
        controlFadeIn.stop();
        toastFadeIn.stop();
        fadeTo(controlBar, controlFadeOut, 0.0);
        fadeTo(poseToast, toastFadeOut, 0.0);
    }

    private static void fadeTo(Node node, FadeTransition fade, double target) {
        if (node.getOpacity() != target && fade.getStatus() != Animation.Status.RUNNING) {
            fade.setFromValue(node.getOpacity());
            fade.setToValue(target);
            fade.play();
        }
    }

    // --- Display ---

    /**
     * Shows the current pose from the image cache (and preloads the next one).
     * Unreadable images are detected asynchronously and skipped automatically.
     */
    private void updateDisplay() {
        GestureImage current = sessionManager.getCurrentImage();
        if (current != null) {
            Image activeImage = preloaderService.loadAndPipeline(
                    sessionManager.getPreviousImage(), current, sessionManager.getNextImage());
            imageView.setImage(activeImage);
            watchForLoadError(activeImage, current);
        }

        String poseLabel = String.format("Pose %d of %d",
                sessionManager.getCurrentIndex() + 1, sessionManager.getTotalImagesInSession());
        if (sessionManager.getPhaseCount() > 1) {
            int phase = sessionManager.getCurrentPhaseIndex() + 1;
            phaseText.setText(String.format("Phase %d/%d", phase, sessionManager.getPhaseCount()));
            // Announce the phase on its first pose; otherwise show progress plus this phase's pose time
            poseLabel = sessionManager.isFirstPoseOfPhase()
                    ? String.format("Phase %d of %d · %s · %s", phase, sessionManager.getPhaseCount(), sessionManager.getCurrentPhase(), poseLabel)
                    : poseLabel + " · " + SessionPhase.formatDuration(sessionManager.getCurrentPoseSeconds());
        }
        poseToastText.setText(poseLabel);
        showOverlays();
        updateTimerText();
    }

    /** Skips the pose if its image is rejected (null), already failed, or fails while loading. */
    private void watchForLoadError(Image image, GestureImage source) {
        detachErrorListener();
        if (image == null || image.isError()) {
            int pose = sessionManager.getCurrentIndex();
            Platform.runLater(() -> skipBrokenImage(image, source, pose));
        } else {
            watchedImage = image;
            image.errorProperty().addListener(errorListener);
        }
    }

    private void onWatchedImageError(ObservableValue<? extends Boolean> obs, Boolean wasError, Boolean isError) {
        if (isError) {
            Image broken = watchedImage;
            int pose = sessionManager.getCurrentIndex();
            GestureImage source = sessionManager.getCurrentImage();
            Platform.runLater(() -> skipBrokenImage(broken, source, pose));
        }
    }

    private void detachErrorListener() {
        if (watchedImage != null) {
            watchedImage.errorProperty().removeListener(errorListener);
            watchedImage = null;
        }
    }

    private void skipBrokenImage(Image image, GestureImage source, int pose) {
        if (cleanedUp || sessionManager.getCurrentIndex() != pose || imageView.getImage() != image) {
            return; // user already moved on
        }
        Exception cause = image != null ? image.getException() : null;
        System.err.println("Skipping unreadable image " + source.getFileName()
                + (image == null ? ": larger than " + ImagePreloaderService.MAX_PIXELS / 1_000_000 + " MP" : "")
                + (cause != null ? ": " + cause.getMessage() : ""));
        // Keep moving in the user's direction so Prev can still get past a broken image;
        // moving forward this way never counts the broken pose as completed
        if (lastMoveForward || sessionManager.getCurrentIndex() == 0) {
            lastMoveForward = true;
            sessionManager.skipUnreadable();
        } else {
            sessionManager.previousImage();
        }
        onPoseChanged();
        poseToastText.setText("Skipped unreadable image: " + source.getFileName());
    }

    private void updateTimerText() {
        int seconds = sessionManager.getCurrentRemainingSeconds();
        timerText.setText(String.format("%02d:%02d", seconds / 60, seconds % 60));
        timerText.pseudoClassStateChanged(CRITICAL, seconds <= 5);
        timerText.pseudoClassStateChanged(WARNING, seconds > 5 && seconds <= 10);
    }

    /**
     * Stops all animations and releases cached images. Safe to call more than once.
     */
    public void cleanup() {
        if (cleanedUp) {
            return;
        }
        cleanedUp = true;
        timeline.stop();
        inactivityTimer.stop();
        controlFadeIn.stop();
        controlFadeOut.stop();
        toastFadeIn.stop();
        toastFadeOut.stop();
        detachErrorListener();
        imageView.setImage(null);
        preloaderService.clear();
    }

    private void finishSession() {
        if (cleanedUp) {
            return;
        }
        SessionResult result = sessionManager.createResult();
        cleanup();
        onSessionComplete.accept(result);
    }

    /**
     * Fully responsive container that bounds ImageView to available center space
     * without pushing top/bottom bars off-screen.
     */
    private static class ResizableImagePane extends StackPane {
        private final ImageView imageView;

        ResizableImagePane(ImageView imageView) {
            this.imageView = imageView;
            getChildren().add(imageView);
            imageView.setPreserveRatio(true);
            imageView.setSmooth(true);
            setMinSize(0, 0);
            setPrefSize(0, 0);
        }

        @Override
        protected double computeMinHeight(double width) {
            return 0;
        }

        @Override
        protected double computeMinWidth(double height) {
            return 0;
        }

        @Override
        protected void layoutChildren() {
            double w = Math.max(0, getWidth() - snappedLeftInset() - snappedRightInset());
            double h = Math.max(0, getHeight() - snappedTopInset() - snappedBottomInset());
            if (w > 0 && h > 0) {
                imageView.setFitWidth(w);
                imageView.setFitHeight(h);
            }
            super.layoutChildren();
        }
    }
}
