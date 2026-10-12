package com.gesturedrawing.ui;

import com.gesturedrawing.domain.SessionConfig;
import com.gesturedrawing.service.PreferencesService;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.Text;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.util.List;
import java.util.function.Consumer;

/**
 * Initial setup menu screen. Designed with comfortable fixed layout (600x760) without scrollbars.
 */
public class MainScreen extends VBox {
    private File selectedFolder;
    private List<File> selectedFiles;
    private final PreferencesService preferencesService = new PreferencesService();
    private final TextField selectionPathField = new TextField();
    private final Spinner<Integer> timeSpinner = new Spinner<>(5, 3600, 60, 5);
    private final Spinner<Integer> countSpinner = new Spinner<>(1, 500, 10, 1);
    private final Label statusLabel = new Label();

    public MainScreen(Stage stage, Consumer<SessionConfig> onStartSession) {
        setSpacing(16);
        setAlignment(Pos.CENTER);
        setPadding(new Insets(20, 24, 20, 24));
        setStyle("-fx-background-color: #0F1117;");

        // Restore saved folder preference
        File lastFolder = preferencesService.getLastFolder();
        if (lastFolder != null) {
            selectedFolder = lastFolder;
            selectionPathField.setText("Folder: " + lastFolder.getAbsolutePath());
        }

        int lastTime = preferencesService.getLastSecondsPerImage(60);
        timeSpinner.getValueFactory().setValue(lastTime);

        int lastCount = preferencesService.getLastTotalImages(10);
        countSpinner.getValueFactory().setValue(lastCount);

        // Header Section
        VBox headerBox = new VBox(4);
        headerBox.setAlignment(Pos.CENTER);
        Text title = new Text("Gesture Drawing Studio");
        title.getStyleClass().add("header-title");

        Text subtitle = new Text("Practice timed figure drawing with your reference image collections");
        subtitle.getStyleClass().add("subtitle");
        headerBox.getChildren().addAll(title, subtitle);

        // Configuration Card Panel
        VBox card = new VBox(12);
        card.setMaxWidth(540);
        card.getStyleClass().add("card-panel");

        // Image Selection Source Section (Folder OR Files)
        VBox sourceSection = new VBox(6);
        Label sourceLabel = new Label("Reference Image Source");
        sourceLabel.getStyleClass().add("label-text");

        selectionPathField.setPromptText("No folder or files selected...");
        selectionPathField.setEditable(false);

        HBox btnBox = new HBox(8);
        btnBox.setAlignment(Pos.CENTER_LEFT);

        Button browseFolderBtn = new Button("Select Folder 📁");
        browseFolderBtn.getStyleClass().add("button-secondary");
        browseFolderBtn.setOnAction(e -> {
            DirectoryChooser chooser = new DirectoryChooser();
            chooser.setTitle("Select Reference Images Folder");
            if (selectedFolder != null && selectedFolder.exists()) {
                chooser.setInitialDirectory(selectedFolder);
            }
            File folder = chooser.showDialog(stage);
            if (folder != null) {
                selectedFolder = folder;
                selectedFiles = null;
                selectionPathField.setText("Folder: " + folder.getAbsolutePath());
                statusLabel.setText("");
                preferencesService.saveLastFolder(folder);
            }
        });

        Button browseFilesBtn = new Button("Select Files 🖼️");
        browseFilesBtn.getStyleClass().add("button-secondary");
        browseFilesBtn.setOnAction(e -> {
            FileChooser chooser = new FileChooser();
            chooser.setTitle("Select Specific Reference Images");
            chooser.getExtensionFilters().add(
                    new FileChooser.ExtensionFilter("Image Files", "*.jpg", "*.jpeg", "*.png", "*.webp", "*.gif", "*.bmp")
            );
            if (selectedFolder != null && selectedFolder.exists()) {
                chooser.setInitialDirectory(selectedFolder);
            }
            List<File> files = chooser.showOpenMultipleDialog(stage);
            if (files != null && !files.isEmpty()) {
                selectedFiles = files;
                selectedFolder = files.get(0).getParentFile();
                selectionPathField.setText(files.size() + " files selected (" + files.get(0).getName() + "...)");
                statusLabel.setText("");
                if (selectedFolder != null) {
                    preferencesService.saveLastFolder(selectedFolder);
                }
            }
        });

        btnBox.getChildren().addAll(browseFolderBtn, browseFilesBtn);
        sourceSection.getChildren().addAll(sourceLabel, selectionPathField, btnBox);

        // Timer Duration Section
        VBox timeSection = new VBox(6);
        Label timeLabel = new Label("Time per Pose (Seconds)");
        timeLabel.getStyleClass().add("label-text");
        timeSpinner.setEditable(true);

        // Quick Preset Buttons
        HBox presetBox = new HBox(6);
        presetBox.setAlignment(Pos.CENTER_LEFT);

        int[] presets = {30, 60, 120, 300};
        String[] presetLabels = {"30s", "1m", "2m", "5m"};

        for (int i = 0; i < presets.length; i++) {
            final int sec = presets[i];
            Button pBtn = new Button(presetLabels[i]);
            pBtn.getStyleClass().add("preset-button");
            pBtn.setOnAction(e -> timeSpinner.getValueFactory().setValue(sec));
            presetBox.getChildren().add(pBtn);
        }

        timeSection.getChildren().addAll(timeLabel, timeSpinner, presetBox);

        // Total Images Count Section
        VBox countSection = new VBox(6);
        Label countLabel = new Label("Total Poses in Session");
        countLabel.getStyleClass().add("label-text");
        countSpinner.setEditable(true);
        countSection.getChildren().addAll(countLabel, countSpinner);

        // Visual Filters & Display Options Section
        VBox optionsSection = new VBox(6);
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
            if (selectedFiles == null && selectedFolder == null) {
                statusLabel.setText("Please select a folder or image files first.");
                return;
            }
            try {
                preferencesService.saveLastSessionSettings(timeSpinner.getValue(), countSpinner.getValue());
                SessionConfig config;
                if (selectedFiles != null && !selectedFiles.isEmpty()) {
                    config = new SessionConfig(
                            selectedFiles,
                            timeSpinner.getValue(),
                            countSpinner.getValue(),
                            flipCheckBox.isSelected(),
                            grayscaleCheckBox.isSelected()
                    );
                } else {
                    config = new SessionConfig(
                            selectedFolder,
                            timeSpinner.getValue(),
                            countSpinner.getValue(),
                            flipCheckBox.isSelected(),
                            grayscaleCheckBox.isSelected()
                    );
                }
                onStartSession.accept(config);
            } catch (Exception ex) {
                statusLabel.setText(ex.getMessage());
            }
        });

        card.getChildren().addAll(sourceSection, timeSection, countSection, optionsSection, statusLabel, startButton);
        getChildren().addAll(headerBox, card);
    }
}
