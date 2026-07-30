package com.gesturedrawing.ui;

import com.gesturedrawing.domain.SessionConfig;
import com.gesturedrawing.service.PreferencesService;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.Text;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;

import java.io.File;
import java.util.function.Consumer;

/**
 * Initial setup view where user selects image directory, session timer, image count, Always-on-Top preference,
 * and visual filters (Flip Horizontal & Grayscale).
 */
public class MainScreen extends VBox {
    private File selectedFolder;
    private final PreferencesService preferencesService = new PreferencesService();
    private final TextField folderPathField = new TextField();
    private final Spinner<Integer> timeSpinner = new Spinner<>(5, 3600, 60, 5);
    private final Spinner<Integer> countSpinner = new Spinner<>(1, 500, 10, 1);
    private final Label statusLabel = new Label();

    public MainScreen(Stage stage, Consumer<SessionConfig> onStartSession) {
        setSpacing(24);
        setAlignment(Pos.CENTER);
        setPadding(new Insets(40));

        // Restore saved user preferences
        File lastFolder = preferencesService.getLastFolder();
        if (lastFolder != null) {
            selectedFolder = lastFolder;
            folderPathField.setText(lastFolder.getAbsolutePath());
        }

        int lastTime = preferencesService.getLastSecondsPerImage(60);
        timeSpinner.getValueFactory().setValue(lastTime);

        int lastCount = preferencesService.getLastTotalImages(10);
        countSpinner.getValueFactory().setValue(lastCount);

        // Header Section
        VBox headerBox = new VBox(8);
        headerBox.setAlignment(Pos.CENTER);
        Text title = new Text("Gesture Drawing Studio");
        title.getStyleClass().add("header-title");

        Text subtitle = new Text("Practice timed figure drawing with your reference image collections");
        subtitle.getStyleClass().add("subtitle");
        headerBox.getChildren().addAll(title, subtitle);

        // Configuration Card Panel
        VBox card = new VBox(16);
        card.setMaxWidth(520);
        card.getStyleClass().add("card-panel");

        // Folder Chooser Section
        VBox folderSection = new VBox(8);
        Label folderLabel = new Label("Image Reference Folder");
        folderLabel.getStyleClass().add("label-text");

        HBox folderBox = new HBox(10);
        folderPathField.setPromptText("No folder selected...");
        folderPathField.setEditable(false);
        HBox.setHgrow(folderPathField, Priority.ALWAYS);

        Button browseBtn = new Button("Browse...");
        browseBtn.getStyleClass().add("button-secondary");
        browseBtn.setOnAction(e -> {
            DirectoryChooser chooser = new DirectoryChooser();
            chooser.setTitle("Select Reference Images Folder");
            if (selectedFolder != null && selectedFolder.exists()) {
                chooser.setInitialDirectory(selectedFolder);
            }
            File folder = chooser.showDialog(stage);
            if (folder != null) {
                selectedFolder = folder;
                folderPathField.setText(folder.getAbsolutePath());
                statusLabel.setText("");
                preferencesService.saveLastFolder(folder);
            }
        });
        folderBox.getChildren().addAll(folderPathField, browseBtn);
        folderSection.getChildren().addAll(folderLabel, folderBox);

        // Timer Duration Section
        VBox timeSection = new VBox(10);
        Label timeLabel = new Label("Time per Pose (Seconds)");
        timeLabel.getStyleClass().add("label-text");
        timeSpinner.setEditable(true);

        // Quick Preset Buttons
        HBox presetBox = new HBox(8);
        presetBox.setAlignment(Pos.CENTER_LEFT);

        int[] presets = {30, 60, 120, 300};
        String[] presetLabels = {"30s", "1 min", "2 min", "5 min"};

        for (int i = 0; i < presets.length; i++) {
            final int sec = presets[i];
            Button pBtn = new Button(presetLabels[i]);
            pBtn.getStyleClass().add("preset-button");
            pBtn.setOnAction(e -> timeSpinner.getValueFactory().setValue(sec));
            presetBox.getChildren().add(pBtn);
        }

        timeSection.getChildren().addAll(timeLabel, timeSpinner, presetBox);

        // Total Images Count Section
        VBox countSection = new VBox(8);
        Label countLabel = new Label("Total Poses in Session");
        countLabel.getStyleClass().add("label-text");
        countSpinner.setEditable(true);
        countSection.getChildren().addAll(countLabel, countSpinner);

        // Visual Filters & Options Section
        VBox optionsSection = new VBox(10);
        Label optionsLabel = new Label("Session Display Options");
        optionsLabel.getStyleClass().add("label-text");

        CheckBox alwaysOnTopCheckBox = new CheckBox("Always on Top (Keep window above other apps)");
        alwaysOnTopCheckBox.setStyle("-fx-text-fill: #D1D5DB; -fx-font-weight: 600; -fx-font-size: 13px; -fx-cursor: hand;");
        boolean initialAlwaysOnTop = preferencesService.isAlwaysOnTop(false);
        alwaysOnTopCheckBox.setSelected(initialAlwaysOnTop);
        stage.setAlwaysOnTop(initialAlwaysOnTop);

        alwaysOnTopCheckBox.selectedProperty().addListener((obs, oldVal, newVal) -> {
            stage.setAlwaysOnTop(newVal);
            preferencesService.saveAlwaysOnTop(newVal);
        });

        CheckBox flipCheckBox = new CheckBox("Flip Horizontal by default (H)");
        flipCheckBox.setStyle("-fx-text-fill: #D1D5DB; -fx-font-weight: 600; -fx-font-size: 13px; -fx-cursor: hand;");
        boolean initialFlipped = preferencesService.isDefaultFlipped(false);
        flipCheckBox.setSelected(initialFlipped);
        flipCheckBox.selectedProperty().addListener((obs, oldVal, newVal) -> preferencesService.saveDefaultFlipped(newVal));

        CheckBox grayscaleCheckBox = new CheckBox("Grayscale / Black & White by default (G)");
        grayscaleCheckBox.setStyle("-fx-text-fill: #D1D5DB; -fx-font-weight: 600; -fx-font-size: 13px; -fx-cursor: hand;");
        boolean initialGrayscale = preferencesService.isDefaultGrayscale(false);
        grayscaleCheckBox.setSelected(initialGrayscale);
        grayscaleCheckBox.selectedProperty().addListener((obs, oldVal, newVal) -> preferencesService.saveDefaultGrayscale(newVal));

        optionsSection.getChildren().addAll(optionsLabel, alwaysOnTopCheckBox, flipCheckBox, grayscaleCheckBox);

        // Status Message Label
        statusLabel.setStyle("-fx-text-fill: #EF4444; -fx-font-weight: bold;");

        // Start Button
        Button startButton = new Button("Start Session 🚀");
        startButton.setMaxWidth(Double.MAX_VALUE);
        startButton.getStyleClass().add("button-primary");
        startButton.setOnAction(e -> {
            if (selectedFolder == null) {
                statusLabel.setText("Please select a folder containing images first.");
                return;
            }
            try {
                preferencesService.saveLastFolder(selectedFolder);
                preferencesService.saveLastSessionSettings(timeSpinner.getValue(), countSpinner.getValue());
                SessionConfig config = new SessionConfig(
                        selectedFolder,
                        timeSpinner.getValue(),
                        countSpinner.getValue(),
                        flipCheckBox.isSelected(),
                        grayscaleCheckBox.isSelected()
                );
                onStartSession.accept(config);
            } catch (Exception ex) {
                statusLabel.setText(ex.getMessage());
            }
        });

        card.getChildren().addAll(folderSection, timeSection, countSection, optionsSection, statusLabel, startButton);
        getChildren().addAll(headerBox, card);
    }
}
