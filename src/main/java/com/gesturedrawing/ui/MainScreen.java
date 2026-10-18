package com.gesturedrawing.ui;

import com.gesturedrawing.domain.SessionConfig;
import com.gesturedrawing.domain.SessionPhase;
import com.gesturedrawing.service.ImageLoaderService;
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
    private final PreferencesService preferencesService;
    private final TextField selectionPathField = new TextField();
    private final Spinner<Integer> timeSpinner = new Spinner<>(5, 3600, 60, 5);
    private final Spinner<Integer> countSpinner = new Spinner<>(1, 500, 10, 1);
    private final Label statusLabel = new Label();
    private final PhaseEditor phaseEditor;

    public MainScreen(Stage stage, PreferencesService preferencesService, Consumer<SessionConfig> onStartSession) {
        this.preferencesService = preferencesService;
        setSpacing(16);
        setAlignment(Pos.CENTER);
        setPadding(new Insets(20, 24, 20, 24));
        getStyleClass().add("screen-root");

        // Restore saved folder preference
        File lastFolder = preferencesService.getLastFolder();
        if (lastFolder != null) {
            selectedFolder = lastFolder;
            selectionPathField.setText("Folder: " + lastFolder.getAbsolutePath());
        }

        timeSpinner.getValueFactory().setValue(preferencesService.getLastSecondsPerImage(60));
        countSpinner.getValueFactory().setValue(preferencesService.getLastTotalImages(10));

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
            if (selectedFolder != null && selectedFolder.isDirectory()) {
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

        Button browseFilesBtn = new Button("Select Files 🖼");
        browseFilesBtn.getStyleClass().add("button-secondary");
        browseFilesBtn.setOnAction(e -> {
            FileChooser chooser = new FileChooser();
            chooser.setTitle("Select Specific Reference Images");
            chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Image Files",
                    ImageLoaderService.SUPPORTED_EXTENSIONS.stream().map(ext -> "*." + ext).toList()));
            if (selectedFolder != null && selectedFolder.isDirectory()) {
                chooser.setInitialDirectory(selectedFolder);
            }
            List<File> files = chooser.showOpenMultipleDialog(stage);
            if (files != null && !files.isEmpty()) {
                selectedFiles = files;
                selectedFolder = files.get(0).getParentFile();
                selectionPathField.setText(files.size() + " files selected (" + files.get(0).getName() + "...)");
                statusLabel.setText("");
                preferencesService.saveLastFolder(selectedFolder);
            }
        });

        btnBox.getChildren().addAll(browseFolderBtn, browseFilesBtn);
        sourceSection.getChildren().addAll(sourceLabel, selectionPathField, btnBox);

        // Timer Duration Section
        VBox timeSection = new VBox(6);
        Label timeLabel = new Label("Time per Pose (Seconds)");
        timeLabel.getStyleClass().add("label-text");
        Spinners.makeEditable(timeSpinner);

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
        Spinners.makeEditable(countSpinner);
        countSection.getChildren().addAll(countLabel, countSpinner);

        // Session Type: Simple (one duration for every pose) or Phases (e.g. 2 × 1 min, 5 × 2 min, 1 × 10 min)
        VBox modeSection = new VBox(6);
        Label modeLabel = new Label("Session Type");
        modeLabel.getStyleClass().add("label-text");

        ToggleGroup modeGroup = new ToggleGroup();
        ToggleButton simpleToggle = new ToggleButton("Simple");
        ToggleButton phasesToggle = new ToggleButton("Phases");
        for (ToggleButton toggle : List.of(simpleToggle, phasesToggle)) {
            toggle.setToggleGroup(modeGroup);
            toggle.getStyleClass().add("segment-button");
        }
        // JavaFX CSS has no :first-child/:last-child, so the rounded ends get their own classes
        simpleToggle.getStyleClass().add("segment-left");
        phasesToggle.getStyleClass().add("segment-right");
        HBox modeBox = new HBox(0, simpleToggle, phasesToggle);
        modeSection.getChildren().addAll(modeLabel, modeBox);

        VBox simplePane = new VBox(12, timeSection, countSection);
        phaseEditor = new PhaseEditor(preferencesService.getPhases(PhaseEditor.DEFAULT_PLAN));
        Label phasesLabel = new Label("Session Phases (played in order)");
        phasesLabel.getStyleClass().add("label-text");
        VBox phasesPane = new VBox(6, phasesLabel, phaseEditor);

        // Only the active mode's pane takes up space in the fixed-size card
        phasesPane.visibleProperty().bind(phasesToggle.selectedProperty());
        phasesPane.managedProperty().bind(phasesToggle.selectedProperty());
        simplePane.visibleProperty().bind(phasesToggle.selectedProperty().not());
        simplePane.managedProperty().bind(phasesToggle.selectedProperty().not());

        modeGroup.selectedToggleProperty().addListener((obs, oldToggle, newToggle) -> {
            if (newToggle == null) {
                oldToggle.setSelected(true); // a segmented control always keeps one option on
            } else {
                preferencesService.savePhasedMode(newToggle == phasesToggle);
            }
        });
        (preferencesService.isPhasedMode(false) ? phasesToggle : simpleToggle).setSelected(true);

        // Visual Filters & Display Options Section
        VBox optionsSection = new VBox(6);
        Label optionsLabel = new Label("Session Display Options");
        optionsLabel.getStyleClass().add("label-text");

        CheckBox alwaysOnTopCheckBox = createOption("Always on Top (Keep window above other apps)", stage.isAlwaysOnTop());
        alwaysOnTopCheckBox.selectedProperty().addListener((obs, oldVal, newVal) -> {
            stage.setAlwaysOnTop(newVal);
            preferencesService.saveAlwaysOnTop(newVal);
        });

        CheckBox flipCheckBox = createOption("Flip Horizontal by default (H)", preferencesService.isDefaultFlipped(false));
        flipCheckBox.selectedProperty().addListener((obs, oldVal, newVal) -> preferencesService.saveDefaultFlipped(newVal));

        CheckBox grayscaleCheckBox = createOption("Grayscale / Black & White by default (G)", preferencesService.isDefaultGrayscale(false));
        grayscaleCheckBox.selectedProperty().addListener((obs, oldVal, newVal) -> preferencesService.saveDefaultGrayscale(newVal));

        optionsSection.getChildren().addAll(optionsLabel, alwaysOnTopCheckBox, flipCheckBox, grayscaleCheckBox);

        // Status Message Label
        statusLabel.getStyleClass().add("status-error");

        // Start Button
        Button startButton = new Button("Start Session 🚀");
        startButton.setMaxWidth(Double.MAX_VALUE);
        startButton.getStyleClass().add("button-primary");
        startButton.setDefaultButton(true);
        startButton.setOnAction(e -> {
            if (selectedFiles == null && selectedFolder == null) {
                statusLabel.setText("Please select a folder or image files first.");
                return;
            }
            List<SessionPhase> phases;
            if (phasesToggle.isSelected()) {
                phaseEditor.commitEdits();
                phases = phaseEditor.getPhases();
            } else {
                Spinners.commit(timeSpinner);
                Spinners.commit(countSpinner);
                phases = List.of(new SessionPhase(countSpinner.getValue(), timeSpinner.getValue()));
            }
            try {
                SessionConfig config = selectedFiles != null && !selectedFiles.isEmpty()
                        ? new SessionConfig(selectedFiles, phases, flipCheckBox.isSelected(), grayscaleCheckBox.isSelected())
                        : new SessionConfig(selectedFolder, phases, flipCheckBox.isSelected(), grayscaleCheckBox.isSelected());
                if (phasesToggle.isSelected()) {
                    preferencesService.savePhases(phases);
                } else {
                    preferencesService.saveLastSessionSettings(timeSpinner.getValue(), countSpinner.getValue());
                }
                onStartSession.accept(config);
            } catch (IllegalArgumentException ex) {
                statusLabel.setText(ex.getMessage());
            }
        });

        card.getChildren().addAll(sourceSection, modeSection, simplePane, phasesPane, optionsSection, statusLabel, startButton);
        getChildren().addAll(headerBox, card);
    }

    private static CheckBox createOption(String text, boolean selected) {
        CheckBox checkBox = new CheckBox(text);
        checkBox.getStyleClass().add("option-checkbox");
        checkBox.setSelected(selected);
        return checkBox;
    }
}
