package com.gesturedrawing.ui;

import com.gesturedrawing.domain.GestureImage;
import com.gesturedrawing.domain.SessionConfig;
import com.gesturedrawing.domain.SessionResult;
import com.gesturedrawing.service.ImagePreloaderService;
import com.gesturedrawing.service.PreferencesService;
import com.gesturedrawing.service.SessionManager;
import javafx.animation.Animation;
import javafx.animation.FadeTransition;
import javafx.animation.KeyFrame;
import javafx.animation.PauseTransition;
import javafx.animation.Timeline;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.ContextMenu;
import javafx.scene.effect.ColorAdjust;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.*;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.util.function.Consumer;

/**
 * Active gesture session viewer screen featuring 100% window height canvas, discrete floating timer,
 * temporary auto-fading "Pose X of Y" toast notification, auto-hiding control bar, and 2-Image preloader pipeline.
 */
public class SessionScreen extends StackPane {
    private final SessionManager sessionManager;
    private final Consumer<SessionResult> onSessionComplete;
    private final PreferencesService preferencesService = new PreferencesService();
    private final ImagePreloaderService preloaderService = new ImagePreloaderService();
    private final ImageView imageView = new ImageView();
    private final Text timerText = new Text();
    private final Button pauseBtn = new Button("Pause (Space)");
    private final HBox controlBar = new HBox(8);

    private final HBox poseToast = new HBox();
    private final Text poseToastText = new Text();

    private Timeline timeline;

    private boolean isFlipped;
    private boolean isGrayscale;
    private final ColorAdjust grayscaleAdjust = new ColorAdjust();

    private final PauseTransition inactivityTimer;
    private final FadeTransition controlFadeIn;
    private final FadeTransition controlFadeOut;
    private final FadeTransition toastFadeIn;
    private final FadeTransition toastFadeOut;

    public SessionScreen(Stage stage, SessionConfig config, SessionManager sessionManager, Consumer<SessionResult> onSessionComplete) {
        this.sessionManager = sessionManager;
        this.onSessionComplete = onSessionComplete;
        this.isFlipped = config.isDefaultFlipped();
        this.isGrayscale = config.isDefaultGrayscale();
        this.grayscaleAdjust.setSaturation(-1.0);

        setStyle("-fx-background-color: #0F1117;");

        // 1. CENTER AREA: 100% Window Height Resizable Image Container
        ResizableImagePane imageContainer = new ResizableImagePane(imageView);
        imageContainer.setPadding(new Insets(0));

        // 2. DISCRETE FLOATING TIMER BADGE OVERLAY (Top-Left Corner)
        HBox timerBadge = new HBox();
        timerBadge.setAlignment(Pos.CENTER);
        timerBadge.getStyleClass().add("timer-badge");
        timerBadge.setMaxWidth(Region.USE_PREF_SIZE);
        timerBadge.setMaxHeight(Region.USE_PREF_SIZE);

        timerText.getStyleClass().add("timer-text");
        timerBadge.getChildren().add(timerText);

        // 3. TEMPORARY "POSE X OF Y" TOAST NOTIFICATION OVERLAY (Top-Center)
        poseToast.setAlignment(Pos.CENTER);
        poseToast.getStyleClass().add("pose-toast");
        poseToast.setMaxWidth(Region.USE_PREF_SIZE);
        poseToast.setMaxHeight(Region.USE_PREF_SIZE);
        poseToast.setMouseTransparent(true); // Allow mouse clicks to pass straight through

        poseToastText.getStyleClass().add("pose-toast-text");
        poseToast.getChildren().add(poseToastText);

        // 4. FLOATING AUTO-HIDE CONTROL BAR OVERLAY (Bottom-Center)
        controlBar.setAlignment(Pos.CENTER);
        controlBar.getStyleClass().add("floating-control-bar");
        controlBar.setMaxWidth(Region.USE_PREF_SIZE);
        controlBar.setMaxHeight(Region.USE_PREF_SIZE);

        Button prevBtn = new Button("⏮ Prev");
        prevBtn.getStyleClass().add("button-secondary");
        prevBtn.setOnAction(e -> {
            sessionManager.previousImage();
            updateDisplay();
        });

        pauseBtn.getStyleClass().add("button-secondary");
        pauseBtn.setOnAction(e -> togglePause());

        Button nextBtn = new Button("Next ⏭");
        nextBtn.getStyleClass().add("button-secondary");
        nextBtn.setOnAction(e -> {
            sessionManager.skipNext();
            checkAndUpdate();
        });

        Button stopBtn = new Button("Finish ⏹");
        stopBtn.setStyle("-fx-background-color: #DC2626; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 6px; -fx-cursor: hand;");
        stopBtn.setOnAction(e -> finishSession());

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

        // 5. CONTEXT MENU (Right Click)
        CheckMenuItem alwaysOnTopItem = new CheckMenuItem("Always on Top (T)");
        alwaysOnTopItem.setSelected(stage.isAlwaysOnTop());
        alwaysOnTopItem.setOnAction(e -> {
            boolean newState = alwaysOnTopItem.isSelected();
            stage.setAlwaysOnTop(newState);
            preferencesService.saveAlwaysOnTop(newState);
        });

        CheckMenuItem flipItem = new CheckMenuItem("Flip Horizontal (H)");
        flipItem.setSelected(isFlipped);
        flipItem.setOnAction(e -> {
            isFlipped = flipItem.isSelected();
            applyVisualFilters();
        });

        CheckMenuItem grayscaleItem = new CheckMenuItem("Grayscale / Black & White (G)");
        grayscaleItem.setSelected(isGrayscale);
        grayscaleItem.setOnAction(e -> {
            isGrayscale = grayscaleItem.isSelected();
            applyVisualFilters();
        });

        ContextMenu contextMenu = new ContextMenu(alwaysOnTopItem, flipItem, grayscaleItem);
        setOnContextMenuRequested(event -> {
            alwaysOnTopItem.setSelected(stage.isAlwaysOnTop());
            flipItem.setSelected(isFlipped);
            grayscaleItem.setSelected(isGrayscale);
            contextMenu.show(this, event.getScreenX(), event.getScreenY());
        });

        // 6. KEYBOARD SHORTCUTS
        setOnKeyPressed(event -> {
            showOverlays();
            if (event.getCode() == KeyCode.SPACE) {
                togglePause();
            } else if (event.getCode() == KeyCode.RIGHT) {
                sessionManager.skipNext();
                checkAndUpdate();
            } else if (event.getCode() == KeyCode.LEFT) {
                sessionManager.previousImage();
                updateDisplay();
            } else if (event.getCode() == KeyCode.ESCAPE) {
                finishSession();
            } else if (event.getCode() == KeyCode.T) {
                boolean newState = !stage.isAlwaysOnTop();
                stage.setAlwaysOnTop(newState);
                preferencesService.saveAlwaysOnTop(newState);
                alwaysOnTopItem.setSelected(newState);
            } else if (event.getCode() == KeyCode.H) {
                isFlipped = !isFlipped;
                applyVisualFilters();
                flipItem.setSelected(isFlipped);
            } else if (event.getCode() == KeyCode.G) {
                isGrayscale = !isGrayscale;
                applyVisualFilters();
                grayscaleItem.setSelected(isGrayscale);
            }
        });

        // Initialize display and start timer loop
        updateDisplay();
        startTimer();
    }

    private void showOverlays() {
        controlFadeOut.stop();
        toastFadeOut.stop();

        if (controlBar.getOpacity() < 1.0 && controlFadeIn.getStatus() != Animation.Status.RUNNING) {
            controlFadeIn.setFromValue(controlBar.getOpacity());
            controlFadeIn.setToValue(1.0);
            controlFadeIn.play();
        }

        if (poseToast.getOpacity() < 1.0 && toastFadeIn.getStatus() != Animation.Status.RUNNING) {
            toastFadeIn.setFromValue(poseToast.getOpacity());
            toastFadeIn.setToValue(1.0);
            toastFadeIn.play();
        }

        inactivityTimer.playFromStart();
    }

    private void hideOverlays() {
        controlFadeIn.stop();
        toastFadeIn.stop();

        if (controlBar.getOpacity() > 0.0 && controlFadeOut.getStatus() != Animation.Status.RUNNING) {
            controlFadeOut.setFromValue(controlBar.getOpacity());
            controlFadeOut.setToValue(0.0);
            controlFadeOut.play();
        }

        if (poseToast.getOpacity() > 0.0 && toastFadeOut.getStatus() != Animation.Status.RUNNING) {
            toastFadeOut.setFromValue(poseToast.getOpacity());
            toastFadeOut.setToValue(0.0);
            toastFadeOut.play();
        }
    }

    private void applyVisualFilters() {
        imageView.setScaleX(isFlipped ? -1.0 : 1.0);
        imageView.setEffect(isGrayscale ? grayscaleAdjust : null);
    }

    private void startTimer() {
        timeline = new Timeline(new KeyFrame(Duration.seconds(1), event -> {
            boolean imageChanged = sessionManager.tickSecond();
            if (imageChanged) {
                checkAndUpdate();
            } else {
                updateTimerTextOnly();
            }
        }));
        timeline.setCycleCount(Timeline.INDEFINITE);
        timeline.play();
    }

    private void togglePause() {
        sessionManager.togglePause();
        if (sessionManager.isPaused()) {
            pauseBtn.setText("Resume");
            pauseBtn.setStyle("-fx-background-color: #D97706; -fx-text-fill: white;");
        } else {
            pauseBtn.setText("Pause");
            pauseBtn.setStyle("");
        }
    }

    private void checkAndUpdate() {
        if (sessionManager.isSessionFinished()) {
            finishSession();
        } else {
            updateDisplay();
        }
    }

    /**
     * Updates display using the 2-Image Pipeline (Current N + Next N+1 Preload).
     * Triggers temporary "Pose X of Y" toast notification.
     */
    private void updateDisplay() {
        GestureImage current = sessionManager.getCurrentImage();
        GestureImage next = sessionManager.getNextImage();

        if (current != null) {
            try {
                // 1. Explicitly clear previous image node reference
                imageView.setImage(null);

                // 2. Execute 2-Image pipeline load & prefetch
                Image activeImage = preloaderService.loadAndPipeline(current, next);
                imageView.setImage(activeImage);
                applyVisualFilters();

            } catch (Exception ex) {
                System.err.println("Error loading image: " + current.getFileUri());
            }
        }

        // Set toast notification text and trigger temporary fade animation
        poseToastText.setText(String.format("Pose %d of %d", sessionManager.getCurrentIndex() + 1, sessionManager.getTotalImagesInSession()));
        showOverlays();
        updateTimerTextOnly();
    }

    private void updateTimerTextOnly() {
        int seconds = sessionManager.getCurrentRemainingSeconds();
        int mins = seconds / 60;
        int secs = seconds % 60;
        timerText.setText(String.format("%02d:%02d", mins, secs));

        if (seconds <= 5) {
            timerText.setStyle("-fx-fill: #EF4444;"); // Red warning
        } else if (seconds <= 10) {
            timerText.setStyle("-fx-fill: #F59E0B;"); // Amber warning
        } else {
            timerText.setStyle("-fx-fill: #10B981;"); // Normal green
        }
    }

    /**
     * Active memory cleanup method called when session ends or is cancelled.
     */
    public void cleanup() {
        if (inactivityTimer != null) {
            inactivityTimer.stop();
        }
        if (controlFadeIn != null) {
            controlFadeIn.stop();
        }
        if (controlFadeOut != null) {
            controlFadeOut.stop();
        }
        if (toastFadeIn != null) {
            toastFadeIn.stop();
        }
        if (toastFadeOut != null) {
            toastFadeOut.stop();
        }
        if (timeline != null) {
            timeline.stop();
            timeline = null;
        }
        // Explicitly clear node reference and preloader buffer
        imageView.setImage(null);
        preloaderService.clear();

        // Suggest garbage collection pass to release heap buffers
        System.gc();
    }

    private void finishSession() {
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

        public ResizableImagePane(ImageView imageView) {
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
            double w = Math.max(0, getWidth() - getPadding().getLeft() - getPadding().getRight());
            double h = Math.max(0, getHeight() - getPadding().getTop() - getPadding().getBottom());
            if (w > 0 && h > 0) {
                imageView.setFitWidth(w);
                imageView.setFitHeight(h);
            }
            super.layoutChildren();
        }
    }
}
