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
 */
public class MainApp extends Application {
    private Stage primaryStage;
    private Scene scene;
    private SessionScreen activeSessionScreen;
    private final ImageLoaderService imageLoaderService = new ImageLoaderService();
    private final PreferencesService preferencesService = new PreferencesService();

    @Override
    public void start(Stage primaryStage) {
        this.primaryStage = primaryStage;
        primaryStage.setTitle("Gesture Drawing Studio");
        primaryStage.setMinWidth(800);
        primaryStage.setMinHeight(600);

        // Load custom application window and taskbar icon
        try {
            String iconUrl = getClass().getResource("/app-icon.jpg").toExternalForm();
            primaryStage.getIcons().add(new javafx.scene.image.Image(iconUrl));
        } catch (Exception ex) {
            System.err.println("Could not load app icon: " + ex.getMessage());
        }

        // Restore saved window dimensions, screen location, maximized state, and always-on-top preference
        double w = preferencesService.getWindowWidth(960);
        double h = preferencesService.getWindowHeight(680);
        double x = preferencesService.getWindowX(-1);
        double y = preferencesService.getWindowY(-1);
        boolean isMaximized = preferencesService.isWindowMaximized(false);
        boolean alwaysOnTop = preferencesService.isAlwaysOnTop(false);

        primaryStage.setAlwaysOnTop(alwaysOnTop);

        MainScreen mainScreen = new MainScreen(primaryStage, this::startSession);
        scene = new Scene(mainScreen, w, h);
        applyStylesheet(scene);

        primaryStage.setScene(scene);

        if (x >= 0 && y >= 0) {
            primaryStage.setX(x);
            primaryStage.setY(y);
        }
        if (isMaximized) {
            primaryStage.setMaximized(true);
        }

        // Save window position and size on exit and cleanup session if active
        primaryStage.setOnCloseRequest(e -> {
            cleanupActiveSession();
            saveCurrentWindowBounds();
        });

        primaryStage.show();
    }

    private void showMainScreen() {
        cleanupActiveSession();
        MainScreen mainScreen = new MainScreen(primaryStage, this::startSession);
        switchRoot(mainScreen);
    }

    private void startSession(SessionConfig config) {
        cleanupActiveSession();
        List<GestureImage> images = imageLoaderService.loadImagesFromDirectory(config.getImageDirectory(), config.getTotalImages());

        if (images.isEmpty()) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("No Images Found");
            alert.setHeaderText("No valid image files in folder");
            alert.setContentText("The selected folder does not contain supported image files (.jpg, .jpeg, .png, .webp). Please choose another folder.");
            alert.showAndWait();
            return;
        }

        SessionManager sessionManager = new SessionManager(config, images);
        activeSessionScreen = new SessionScreen(primaryStage, sessionManager, this::showSummaryScreen);

        switchRoot(activeSessionScreen);
        activeSessionScreen.requestFocus();
    }

    private void showSummaryScreen(SessionResult result) {
        cleanupActiveSession();
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
     * Seamless root switching that preserves exact window size, position, and maximized state.
     */
    private void switchRoot(Parent newRoot) {
        if (scene != null) {
            scene.setRoot(newRoot);
        }
    }

    private void saveCurrentWindowBounds() {
        if (primaryStage != null) {
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
