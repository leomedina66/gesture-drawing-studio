package com.gesturedrawing;

import com.gesturedrawing.domain.GestureImage;
import com.gesturedrawing.domain.SessionConfig;
import com.gesturedrawing.domain.SessionResult;
import com.gesturedrawing.service.ImageLoaderService;
import com.gesturedrawing.service.PreferencesService;
import com.gesturedrawing.service.SessionManager;
import com.gesturedrawing.ui.MainScreen;
import com.gesturedrawing.ui.SessionScreen;
import com.gesturedrawing.ui.SummaryScreen;
import javafx.application.Application;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.stage.Stage;

import java.util.List;

/**
 * Entry point launcher for Gesture Drawing Studio Desktop Application.
 * Features standardized fixed non-resizable menu screens (MainScreen & SummaryScreen at 600x760)
 * and fully resizable drawing session viewer (SessionScreen).
 */
public class MainApp extends Application {
    private static final double MENU_WIDTH = 600;
    private static final double MENU_HEIGHT = 760;

    private Stage primaryStage;
    private Scene scene;
    private SessionScreen activeSessionScreen;
    private final ImageLoaderService imageLoaderService = new ImageLoaderService();
    private final PreferencesService preferencesService = new PreferencesService();

    @Override
    public void start(Stage primaryStage) {
        this.primaryStage = primaryStage;
        primaryStage.setTitle("Gesture Drawing Studio");

        // Load custom application window and taskbar icon
        try {
            String iconUrl = getClass().getResource("/app-icon.jpg").toExternalForm();
            primaryStage.getIcons().add(new javafx.scene.image.Image(iconUrl));
        } catch (Exception ex) {
            System.err.println("Could not load app icon: " + ex.getMessage());
        }

        // Restore saved position and always-on-top preference
        double x = preferencesService.getWindowX(-1);
        double y = preferencesService.getWindowY(-1);
        boolean alwaysOnTop = preferencesService.isAlwaysOnTop(false);

        primaryStage.setAlwaysOnTop(alwaysOnTop);

        // Main Menu is non-resizable with standardized fixed dimensions (600x760)
        primaryStage.setResizable(false);

        MainScreen mainScreen = new MainScreen(primaryStage, this::startSession);
        scene = new Scene(mainScreen, MENU_WIDTH, MENU_HEIGHT);
        applyStylesheet(scene);

        primaryStage.setScene(scene);

        if (x >= 0 && y >= 0) {
            primaryStage.setX(x);
            primaryStage.setY(y);
        }

        // Save window position and size on exit and cleanup session if active
        primaryStage.setOnCloseRequest(e -> {
            cleanupActiveSession();
            saveCurrentWindowBounds();
        });

        primaryStage.show();
    }

    private void showMainScreen() {
        if (activeSessionScreen != null) {
            saveCurrentWindowBounds();
        }
        cleanupActiveSession();

        // Lock resizability and set standardized fixed menu dimensions
        primaryStage.setMaximized(false);
        primaryStage.setResizable(false);
        primaryStage.setWidth(MENU_WIDTH);
        primaryStage.setHeight(MENU_HEIGHT);

        MainScreen mainScreen = new MainScreen(primaryStage, this::startSession);
        switchRoot(mainScreen);
    }

    private void startSession(SessionConfig config) {
        cleanupActiveSession();
        List<GestureImage> images = imageLoaderService.loadImagesFromFiles(config.getSelectedFiles(), config.getTotalImages());

        if (images.isEmpty()) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("No Images Found");
            alert.setHeaderText("No valid image files found");
            alert.setContentText("The selected folder or files do not contain supported image files (.jpg, .jpeg, .png, .webp). Please select another source.");
            alert.showAndWait();
            return;
        }

        // Enable full resizability for Session Viewer and restore saved session dimensions
        primaryStage.setResizable(true);
        primaryStage.setMinWidth(320);
        primaryStage.setMinHeight(240);

        double w = preferencesService.getWindowWidth(960);
        double h = preferencesService.getWindowHeight(680);
        boolean isMaximized = preferencesService.isWindowMaximized(false);

        primaryStage.setWidth(w);
        primaryStage.setHeight(h);
        if (isMaximized) {
            primaryStage.setMaximized(true);
        }

        SessionManager sessionManager = new SessionManager(config, images);
        activeSessionScreen = new SessionScreen(primaryStage, config, sessionManager, this::showSummaryScreen);

        switchRoot(activeSessionScreen);
        activeSessionScreen.requestFocus();
    }

    private void showSummaryScreen(SessionResult result) {
        if (activeSessionScreen != null) {
            saveCurrentWindowBounds();
        }
        cleanupActiveSession();

        // Lock resizability and set standardized fixed menu dimensions (600x760)
        primaryStage.setMaximized(false);
        primaryStage.setResizable(false);
        primaryStage.setWidth(MENU_WIDTH);
        primaryStage.setHeight(MENU_HEIGHT);

        SummaryScreen summaryScreen = new SummaryScreen(result, this::showMainScreen);
        switchRoot(summaryScreen);
    }

    private void cleanupActiveSession() {
        if (activeSessionScreen != null) {
            activeSessionScreen.cleanup();
            activeSessionScreen = null;
        }
    }

    /**
     * Seamless root switching that preserves current scene configuration.
     */
    private void switchRoot(Parent newRoot) {
        if (scene != null) {
            scene.setRoot(newRoot);
        }
    }

    private void saveCurrentWindowBounds() {
        if (primaryStage != null && primaryStage.isResizable()) {
            preferencesService.saveWindowBounds(
                    primaryStage.getWidth(),
                    primaryStage.getHeight(),
                    primaryStage.getX(),
                    primaryStage.getY(),
                    primaryStage.isMaximized()
            );
        }
    }

    private void applyStylesheet(Scene scene) {
        try {
            String cssPath = getClass().getResource("/styles.css").toExternalForm();
            scene.getStylesheets().add(cssPath);
        } catch (Exception ex) {
            System.err.println("Could not load styles.css: " + ex.getMessage());
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
