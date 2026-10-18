package com.gesturedrawing.ui;

import com.gesturedrawing.domain.SessionPhase;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Spinner;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

/**
 * Editor for a phased session plan, e.g. "2 × 1 min, 5 × 2 min, 1 × 10 min".
 * Offers one-click presets, one row per phase (pose count + duration) and a live total.
 */
public class PhaseEditor extends VBox {
    public static final int MAX_PHASES = 10;

    /** Common pose durations offered in each phase's duration picker. */
    private static final List<Integer> DURATIONS = List.of(
            15, 30, 45, 60, 90, 120, 180, 300, 600, 900, 1200, 1800, 2700, 3600);

    private static final Map<String, List<SessionPhase>> PRESETS = new LinkedHashMap<>();

    static {
        PRESETS.put("Warm-up", List.of(new SessionPhase(5, 30), new SessionPhase(3, 60), new SessionPhase(1, 300)));
        PRESETS.put("Classic", List.of(new SessionPhase(2, 60), new SessionPhase(5, 120), new SessionPhase(1, 600)));
        PRESETS.put("Long Study", List.of(new SessionPhase(4, 120), new SessionPhase(2, 300), new SessionPhase(1, 1200)));
    }

    public static final List<SessionPhase> DEFAULT_PLAN = PRESETS.get("Classic");

    private static final StringConverter<Integer> DURATION_CONVERTER = new StringConverter<>() {
        @Override
        public String toString(Integer seconds) {
            return seconds == null ? "" : SessionPhase.formatDuration(seconds);
        }

        @Override
        public Integer fromString(String text) {
            throw new UnsupportedOperationException("Duration picker is not editable");
        }
    };

    private final VBox rows = new VBox(6);
    private final Button addButton = new Button("+ Add Phase");
    private final Label totalLabel = new Label();

    public PhaseEditor(List<SessionPhase> initialPlan) {
        setSpacing(8);

        HBox presetBox = new HBox(6);
        presetBox.setAlignment(Pos.CENTER_LEFT);
        for (Map.Entry<String, List<SessionPhase>> preset : PRESETS.entrySet()) {
            Button presetBtn = new Button(preset.getKey());
            presetBtn.getStyleClass().add("preset-button");
            presetBtn.setOnAction(e -> setPhases(preset.getValue()));
            presetBox.getChildren().add(presetBtn);
        }

        ScrollPane scroll = new ScrollPane(rows);
        scroll.setFitToWidth(true);
        scroll.setPrefViewportHeight(126);
        scroll.setMaxHeight(130);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.getStyleClass().add("gallery-scroll");

        addButton.getStyleClass().add("button-secondary");
        addButton.setOnAction(e -> {
            List<SessionPhase> phases = getPhases();
            SessionPhase last = phases.isEmpty() ? new SessionPhase(1, 60) : phases.get(phases.size() - 1);
            addRow(new SessionPhase(1, last.secondsPerPose()));
            refresh();
        });

        totalLabel.getStyleClass().add("muted-text");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox footer = new HBox(8, addButton, spacer, totalLabel);
        footer.setAlignment(Pos.CENTER_LEFT);

        getChildren().addAll(presetBox, scroll, footer);
        setPhases(initialPlan == null || initialPlan.isEmpty() ? DEFAULT_PLAN : initialPlan);
    }

    /** Current plan, in play order. */
    public List<SessionPhase> getPhases() {
        List<SessionPhase> phases = new ArrayList<>();
        for (var node : rows.getChildren()) {
            phases.add(((PhaseRow) node).toPhase());
        }
        return phases;
    }

    public void setPhases(List<SessionPhase> phases) {
        rows.getChildren().clear();
        phases.stream().limit(MAX_PHASES).forEach(this::addRow);
        refresh();
    }

    /** Commits any count typed into a spinner but not yet confirmed. */
    public void commitEdits() {
        for (var node : rows.getChildren()) {
            Spinners.commit(((PhaseRow) node).count);
        }
    }

    private void addRow(SessionPhase phase) {
        rows.getChildren().add(new PhaseRow(phase));
    }

    private void refresh() {
        for (int i = 0; i < rows.getChildren().size(); i++) {
            PhaseRow row = (PhaseRow) rows.getChildren().get(i);
            row.title.setText("Phase " + (i + 1));
            row.removeBtn.setDisable(rows.getChildren().size() == 1);
        }
        addButton.setDisable(rows.getChildren().size() >= MAX_PHASES);

        // Uses the text being typed so the total updates live, before the spinner commits
        int poses = 0;
        int seconds = 0;
        for (var node : rows.getChildren()) {
            PhaseRow row = (PhaseRow) node;
            poses += row.liveCount();
            seconds += row.liveCount() * row.duration.getValue();
        }
        boolean tooMany = poses > SessionPhase.MAX_POSES;
        totalLabel.setText(poses + (poses == 1 ? " pose" : " poses") + " · " + SessionPhase.formatDuration(seconds) + " total"
                + (tooMany ? " (max " + SessionPhase.MAX_POSES + " poses)" : ""));
        totalLabel.getStyleClass().setAll(tooMany ? "status-error" : "muted-text");
    }

    /** One editable phase: "Phase N  [count] poses of [duration]  ✕". */
    private final class PhaseRow extends HBox {
        private final Label title = new Label();
        private final Spinner<Integer> count;
        private final ComboBox<Integer> duration = new ComboBox<>();
        private final Button removeBtn = new Button("✕");

        PhaseRow(SessionPhase phase) {
            super(8);
            setAlignment(Pos.CENTER_LEFT);
            getStyleClass().add("phase-row");

            title.getStyleClass().add("phase-title");
            title.setMinWidth(62);

            count = new Spinner<>(1, SessionPhase.MAX_POSES, phase.poseCount(), 1);
            count.setPrefWidth(90);
            Spinners.makeEditable(count);
            count.valueProperty().addListener((obs, oldVal, newVal) -> refresh());
            count.getEditor().textProperty().addListener((obs, oldText, newText) -> refresh());

            TreeSet<Integer> options = new TreeSet<>(DURATIONS);
            options.add(phase.secondsPerPose()); // keep custom saved values selectable
            duration.getItems().setAll(options);
            duration.setConverter(DURATION_CONVERTER);
            duration.setValue(phase.secondsPerPose());
            duration.setPrefWidth(130);
            duration.valueProperty().addListener((obs, oldVal, newVal) -> refresh());

            Label of = new Label("poses of");
            of.getStyleClass().add("muted-text");

            removeBtn.getStyleClass().add("icon-button");
            removeBtn.setOnAction(e -> {
                rows.getChildren().remove(this);
                refresh();
            });

            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);
            getChildren().addAll(title, count, of, duration, spacer, removeBtn);
        }

        /** Pose count as currently typed (clamped to the spinner range), falling back to the committed value. */
        int liveCount() {
            try {
                return Math.max(1, Math.min(SessionPhase.MAX_POSES, Integer.parseInt(count.getEditor().getText().trim())));
            } catch (NumberFormatException ex) {
                return count.getValue();
            }
        }

        SessionPhase toPhase() {
            return new SessionPhase(count.getValue(), duration.getValue());
        }
    }
}
