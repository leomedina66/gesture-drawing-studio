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
import javafx.application.Platform;
import javafx.geometry.Rectangle2D;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.image.Image;
import javafx.stage.Screen;
import javafx.stage.Stage;

import java.net.URL;
import java.util.List;

/**
 * Entry point launcher for Gesture Drawing Studio Desktop Application.
 * Features standardized fixed non-resizable menu screens (MainScreen & SummaryScreen at 600x760)
 * and fully resizable drawing session viewer (SessionScreen).
 */
public class MainApp extends Application {
    private static final double MENU_WIDTH = 600;
    private static final double MENU_HEIGHT = 760;
    private static final double DEFAULT_SESSION_WIDTH = 960;
    private static final double DEFAULT_SESSION_HEIGHT = 680;

    private Stage primaryStage;
    private Scene scene;
    private SessionScreen activeSessionScreen;
    /** Last un-maximized session window bounds, so a maximized session still remembers its normal size. */
    private Rectangle2D normalSessionBounds;
    private final ImageLoaderService imageLoaderService = new ImageLoaderService();
    private final PreferencesService preferencesService = new PreferencesService();

    @Override
    public void start(Stage primaryStage) {
        this.primaryStage = primaryStage;
        primaryStage.setTitle("Gesture Drawing Studio");

        URL iconUrl = getClass().getResource("/app-icon.jpg");
        if (iconUrl != null) {
            primaryStage.getIcons().add(new Image(iconUrl.toExternalForm()));
        }

        primaryStage.setAlwaysOnTop(preferencesService.isAlwaysOnTop(false));

        scene = new Scene(new MainScreen(primaryStage, preferencesService, this::startSession), MENU_WIDTH, MENU_HEIGHT);
        URL cssUrl = getClass().getResource("/styles.css");
        if (cssUrl != null) {
            scene.getStylesheets().add(cssUrl.toExternalForm());
        } else {
            System.err.println("Could not load styles.css");
        }
        primaryStage.setScene(scene);
        primaryStage.setResizable(false);
        restorePosition(MENU_WIDTH, MENU_HEIGHT);

        primaryStage.xProperty().addListener(obs -> trackNormalBounds());
        primaryStage.yProperty().addListener(obs -> trackNormalBounds());
        primaryStage.widthProperty().addListener(obs -> trackNormalBounds());
        primaryStage.heightProperty().addListener(obs -> trackNormalBounds());

        primaryStage.setOnCloseRequest(e -> {
            saveCurrentWindowBounds();
            cleanupActiveSession();
        });

        primaryStage.show();
    }

    private void showMainScreen() {
        leaveSession();
        switchRoot(new MainScreen(primaryStage, preferencesService, this::startSession));
    }

    private void showSummaryScreen(SessionResult result) {
        leaveSession();
        switchRoot(new SummaryScreen(result, this::showMainScreen));
    }

    private void startSession(SessionConfig config) {
        cleanupActiveSession();
        List<GestureImage> images = imageLoaderService.loadImages(config);

        if (images.isEmpty()) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.initOwner(primaryStage);
            alert.setTitle("No Images Found");
            alert.setHeaderText("No valid image files found");
            alert.setContentText("The selected folder or files do not contain supported image files ("
                    + String.join(", ", ImageLoaderService.SUPPORTED_EXTENSIONS.stream().map(ext -> "." + ext).toList())
                    + "). Please select another source.");
            alert.showAndWait();
            return;
        }

        preferencesService.saveWindowPosition(primaryStage.getX(), primaryStage.getY());

        // Enable full resizability for Session Viewer and restore saved session dimensions
        primaryStage.setResizable(true);
        primaryStage.setMinWidth(320);
        primaryStage.setMinHeight(240);

        double w = preferencesService.getWindowWidth(DEFAULT_SESSION_WIDTH);
        double h = preferencesService.getWindowHeight(DEFAULT_SESSION_HEIGHT);
        primaryStage.setWidth(w);
        primaryStage.setHeight(h);
        restorePosition(w, h);
        normalSessionBounds = new Rectangle2D(primaryStage.getX(), primaryStage.getY(), w, h);
        if (preferencesService.isWindowMaximized(false)) {
            primaryStage.setMaximized(true);
        }

        SessionManager sessionManager = new SessionManager(config, images);
        activeSessionScreen = new SessionScreen(primaryStage, config, sessionManager, preferencesService, this::showSummaryScreen);

        switchRoot(activeSessionScreen);
        activeSessionScreen.requestFocus();
    }

    /** Persists session bounds, stops the session and goes back to the fixed-size menu window. */
    private void leaveSession() {
        saveCurrentWindowBounds();
        cleanupActiveSession();

        primaryStage.setMaximized(false);
        primaryStage.setResizable(false);
        primaryStage.setMinWidth(0);
        primaryStage.setMinHeight(0);
        primaryStage.setWidth(MENU_WIDTH);
        primaryStage.setHeight(MENU_HEIGHT);
        restorePosition(MENU_WIDTH, MENU_HEIGHT);
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
        scene.setRoot(newRoot);
    }

    /**
     * Session window: saves its normal size (kept unchanged while maximized, so un-maximizing later
     * doesn't restore a full-screen-sized window). Menu window: saves only its position.
     */
    private void saveCurrentWindowBounds() {
        if (primaryStage.isIconified()) {
            return;
        }
        if (activeSessionScreen == null) {
            preferencesService.saveWindowPosition(primaryStage.getX(), primaryStage.getY());
        } else if (primaryStage.isMaximized() && normalSessionBounds != null) {
            preferencesService.saveWindowBounds(normalSessionBounds.getWidth(), normalSessionBounds.getHeight(),
                    normalSessionBounds.getMinX(), normalSessionBounds.getMinY(), true);
        } else {
            preferencesService.saveWindowBounds(primaryStage.getWidth(), primaryStage.getHeight(),
                    primaryStage.getX(), primaryStage.getY(), false);
        }
    }

    /**
     * Records the session window's normal bounds. Deferred with runLater so that during a maximize
     * (size changes may arrive before the maximized flag) the check sees the final state.
     */
    private void trackNormalBounds() {
        Platform.runLater(() -> {
            if (activeSessionScreen != null && !primaryStage.isMaximized() && !primaryStage.isIconified()) {
                normalSessionBounds = new Rectangle2D(primaryStage.getX(), primaryStage.getY(),
                        primaryStage.getWidth(), primaryStage.getHeight());
            }
        });
    }

    /** Moves the window to the saved position if it is still visible on a connected screen, else centers it. */
    private void restorePosition(double width, double height) {
        double x = preferencesService.getWindowX(Double.NaN);
        double y = preferencesService.getWindowY(Double.NaN);
        if (!Double.isNaN(x) && !Double.isNaN(y) && isTitleBarOnScreen(x, y, width)) {
            primaryStage.setX(x);
            primaryStage.setY(y);
        } else {
            primaryStage.centerOnScreen();
        }
    }

    /** True when enough of the title bar is on some connected screen for the user to grab it. */
    private static boolean isTitleBarOnScreen(double x, double y, double width) {
        Rectangle2D titleBar = new Rectangle2D(x + 40, y + 8, Math.max(1, width - 80), 24);
        return Screen.getScreens().stream().anyMatch(screen -> screen.getVisualBounds().intersects(titleBar));
    }

    public static void main(String[] args) {
        launch(args);
    }
}
