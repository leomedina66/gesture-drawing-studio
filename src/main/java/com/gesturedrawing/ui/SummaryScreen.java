package com.gesturedrawing.ui;

import com.gesturedrawing.domain.GestureImage;
import com.gesturedrawing.domain.SessionResult;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.text.Text;

import java.util.List;

/**
 * End-of-session screen showing overall stats and a thumbnail gallery of poses practiced.
 * Fits the exact same standardized menu dimensions (600x760) without window scrollbars.
 */
public class SummaryScreen extends VBox {
    private static final double THUMBNAIL_SIZE = 110;

    public SummaryScreen(SessionResult result, Runnable onNewSession) {
        setSpacing(16);
        setAlignment(Pos.CENTER);
        setPadding(new Insets(20, 24, 20, 24));
        getStyleClass().add("screen-root");

        // Header Section
        VBox headerBox = new VBox(4);
        headerBox.setAlignment(Pos.CENTER);

        Text title = new Text("Session Complete! 🎉");
        title.getStyleClass().add("header-title");

        Text subtitle = new Text("Great work! Here is a summary of the poses practiced in this session.");
        subtitle.getStyleClass().add("subtitle");

        headerBox.getChildren().addAll(title, subtitle);

        // Configuration Card Panel container matching MainScreen style
        VBox card = new VBox(16);
        card.setMaxWidth(540);
        card.getStyleClass().add("card-panel");

        // Stats Cards Bar
        HBox statsBar = new HBox(12);
        statsBar.setAlignment(Pos.CENTER);

        VBox timeCard = createStatCard("TOTAL PRACTICE TIME", result.getFormattedTotalTime());
        VBox countCard = createStatCard("POSES COMPLETED", String.valueOf(result.getCompletedImagesCount()));

        statsBar.getChildren().addAll(timeCard, countCard);

        // Thumbnail Gallery Section
        VBox gallerySection = new VBox(8);
        gallerySection.setAlignment(Pos.CENTER_LEFT);

        Label galleryTitle = new Label("Session Poses Gallery");
        galleryTitle.getStyleClass().add("label-text");

        FlowPane thumbnailGrid = new FlowPane();
        thumbnailGrid.setHgap(10);
        thumbnailGrid.setVgap(10);
        thumbnailGrid.setPadding(new Insets(10));
        thumbnailGrid.getStyleClass().add("thumbnail-grid");

        List<GestureImage> images = result.getImagesUsed();
        if (images.isEmpty()) {
            Label emptyLabel = new Label("No images were completed in this session.");
            emptyLabel.getStyleClass().add("muted-text");
            thumbnailGrid.getChildren().add(emptyLabel);
        } else {
            for (GestureImage gestureImg : images) {
                VBox thumbCard = new VBox();
                thumbCard.getStyleClass().add("thumbnail-card");

                // Decoded in the background straight at thumbnail size to keep the gallery light
                ImageView thumbView = new ImageView(new Image(gestureImg.getFileUri(), THUMBNAIL_SIZE, THUMBNAIL_SIZE, true, true, true));
                Tooltip.install(thumbCard, new Tooltip(gestureImg.getFileName()));

                thumbCard.getChildren().add(thumbView);
                thumbnailGrid.getChildren().add(thumbCard);
            }
        }

        ScrollPane galleryScroll = new ScrollPane(thumbnailGrid);
        galleryScroll.setFitToWidth(true);
        galleryScroll.setMaxHeight(260);
        galleryScroll.getStyleClass().add("gallery-scroll");

        gallerySection.getChildren().addAll(galleryTitle, galleryScroll);

        // Action Buttons
        Button newSessionBtn = new Button("Start New Session 🔄");
        newSessionBtn.getStyleClass().add("button-primary");
        newSessionBtn.setMaxWidth(Double.MAX_VALUE);
        newSessionBtn.setOnAction(e -> onNewSession.run());

        card.getChildren().addAll(statsBar, gallerySection, newSessionBtn);
        getChildren().addAll(headerBox, card);
    }

    private static VBox createStatCard(String labelText, String valueText) {
        VBox card = new VBox(4);
        card.setAlignment(Pos.CENTER);
        card.setPadding(new Insets(12, 18, 12, 18));
        card.getStyleClass().add("card-panel");
        card.setPrefWidth(210);

        Label label = new Label(labelText);
        label.getStyleClass().add("stat-label");

        Label value = new Label(valueText);
        value.getStyleClass().add("stat-value");

        card.getChildren().addAll(label, value);
        return card;
    }
}
