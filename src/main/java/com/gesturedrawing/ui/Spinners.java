package com.gesturedrawing.ui;

import javafx.scene.control.Spinner;

/**
 * Helpers for editable integer spinners shared by the setup screens.
 */
final class Spinners {
    private Spinners() {
    }

    /** Editable spinner that commits typed text when it loses focus. */
    static void makeEditable(Spinner<Integer> spinner) {
        spinner.setEditable(true);
        spinner.focusedProperty().addListener((obs, wasFocused, isFocused) -> {
            if (!isFocused) {
                commit(spinner);
            }
        });
    }

    /** Commits typed text (clamped to the spinner range); reverts to the last valid value on bad input. */
    static void commit(Spinner<Integer> spinner) {
        try {
            spinner.commitValue();
        } catch (RuntimeException ex) {
            spinner.cancelEdit();
        }
    }
}
