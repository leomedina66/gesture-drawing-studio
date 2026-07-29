package com.gesturedrawing.ui;

import com.gesturedrawing.domain.GestureImage;
import com.gesturedrawing.domain.SessionResult;
import com.gesturedrawing.service.SessionManager;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.*;
import javafx.scene.text.Text;
import javafx.util.Duration;

import java.util.function.Consumer;

/**
 * Active gesture session viewer screen with image display, top-left timer, and bottom control bar.
 */
public class SessionScreen extends BorderPane {
    private final SessionManager sessionManager;
    private final Consumer<SessionResult> onSessionComplete;
    private final ImageView imageView = new ImageView();
    private final Text timerText = new Text();
    private final Text counterText = new Text();
    private final Button pauseBtn = new Button("Pause (Space)");
    private Timeline timeline;

    public SessionScreen(SessionManager sessionManager, Consumer<SessionResult> onSessionComplete) {
        this.sessionManager = sessionManager;
        this.onSessionComplete = onSessionComplete;

        setStyle("-fx-background-color: #0F1117;");

        // 1. TOP BAR: Timer & Pose Counter in Top-Left Corner
        HBox topBar = new HBox(12);
        topBar.setAlignment(Pos.CENTER_LEFT);
        topBar.setPadding(new Insets(12, 20, 4, 20));

        HBox timerBadge = new HBox(12);
        timerBadge.setAlignment(Pos.CENTER);
        timerBadge.getStyleClass().add("timer-badge");

        timerText.getStyleClass().add("timer-text");
        counterText.getStyleClass().add("counter-text");

        timerBadge.getChildren().addAll(timerText, counterText);
        topBar.getChildren().add(timerBadge);
        setTop(topBar);

        // 2. CENTER AREA: Non-recursive image container (Fully modular & responsive)
        ResizableImagePane imageContainer = new ResizableImagePane(imageView);
        imageContainer.setPadding(new Insets(8, 16, 8, 16));
        setCenter(imageContainer);

        // 3. BOTTOM CONTROL BAR: Placed strictly below the image area
        HBox controlBar = new HBox(16);
        controlBar.setAlignment(Pos.CENTER);
        controlBar.getStyleClass().add("bottom-control-bar");

        Button prevBtn = new Button("⏮ Prev (Left)");
        prevBtn.getStyleClass().add("button-secondary");
        prevBtn.setOnAction(e -> {
            sessionManager.previousImage();
            updateDisplay();
        });

        pauseBtn.getStyleClass().add("button-secondary");
        pauseBtn.setOnAction(e -> togglePause());

        Button nextBtn = new Button("Next ⏭ (Right)");
        nextBtn.getStyleClass().add("button-secondary");
        nextBtn.setOnAction(e -> {
            sessionManager.skipNext();
            checkAndUpdate();
        });

        Button stopBtn = new Button("Finish Session ⏹ (Esc)");
        stopBtn.setStyle("-fx-background-color: #DC2626; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 6px; -fx-cursor: hand;");
        stopBtn.setOnAction(e -> finishSession());

        controlBar.getChildren().addAll(prevBtn, pauseBtn, nextBtn, stopBtn);
        setBottom(controlBar);

        // Keyboard Shortcuts
        setOnKeyPressed(event -> {
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
            }
        });

        // Initialize display and start timer loop
        updateDisplay();
        startTimer();
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
            pauseBtn.setText("Resume (Space)");
            pauseBtn.setStyle("-fx-background-color: #D97706; -fx-text-fill: white;");
        } else {
            pauseBtn.setText("Pause (Space)");
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

    private void updateDisplay() {
        GestureImage current = sessionManager.getCurrentImage();
        if (current != null) {
            try {
                Image img = new Image(current.getFileUri(), true);
                imageView.setImage(img);
            } catch (Exception ex) {
                System.err.println("Error loading image: " + current.getFileUri());
            }
        }
        counterText.setText(String.format("Pose %d of %d", sessionManager.getCurrentIndex() + 1, sessionManager.getTotalImagesInSession()));
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

    private void finishSession() {
        if (timeline != null) {
            timeline.stop();
        }
        onSessionComplete.accept(sessionManager.createResult());
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
            // CRITICAL FOR MODULAR RESPONSIVE LAYOUT:
            // Force container minSize to 0,0 so BorderPane allocates space from window size
            // rather than expanding to unscaled image dimensions.
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
