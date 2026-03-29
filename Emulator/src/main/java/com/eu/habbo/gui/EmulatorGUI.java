package com.eu.habbo.gui;

import com.eu.habbo.Emulator;
import com.eu.habbo.gui.controller.*;
import com.eu.habbo.gui.logging.GUILogAppender;
import com.eu.habbo.gui.notifications.DesktopNotifications;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.TabPane;
import javafx.scene.control.ToggleButton;
import javafx.scene.image.Image;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;

public class EmulatorGUI extends Application {

    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(EmulatorGUI.class);
    private Stage primaryStage;
    private Scene scene;
    private TrayIcon trayIcon;
    private boolean darkMode = true;

    @Override
    public void start(Stage primaryStage) {
        this.primaryStage = primaryStage;

        Platform.setImplicitExit(false);
        registerGUILogAppender();

        DashboardTabController dashboardTab = new DashboardTabController();
        PlayersTabController playersTab = new PlayersTabController();
        RoomsTabController roomsTab = new RoomsTabController();
        ConsoleTabController consoleTab = new ConsoleTabController();
        ConfigTabController configTab = new ConfigTabController();
        PerformanceTabController performanceTab = new PerformanceTabController();

        TabPane tabPane = new TabPane();
        tabPane.getTabs().addAll(
                dashboardTab.createTab(),
                playersTab.createTab(),
                roomsTab.createTab(),
                consoleTab.createTab(),
                configTab.createTab(),
                performanceTab.createTab()
        );

        // Theme toggle
        ToggleButton themeToggle = new ToggleButton("Light Mode");
        themeToggle.setSelected(false);
        themeToggle.setStyle("-fx-font-size: 11px;");
        themeToggle.setOnAction(e -> {
            darkMode = !themeToggle.isSelected();
            themeToggle.setText(darkMode ? "Light Mode" : "Dark Mode");
            applyTheme();
        });

        HBox topBar = new HBox(themeToggle);
        topBar.setAlignment(Pos.CENTER_RIGHT);
        topBar.setPadding(new Insets(4, 8, 4, 8));
        topBar.setStyle("-fx-background-color: transparent;");

        BorderPane root = new BorderPane();
        root.setTop(topBar);
        root.setCenter(tabPane);

        scene = new Scene(root, 960, 680);
        applyTheme();

        primaryStage.setTitle("Arcturus Morningstar " + Emulator.MAJOR + "." + Emulator.MINOR + "." + Emulator.BUILD);
        primaryStage.setScene(scene);
        primaryStage.setMinWidth(750);
        primaryStage.setMinHeight(500);

        primaryStage.getIcons().add(createWindowIcon());
        setupSystemTray();

        primaryStage.setOnCloseRequest(e -> {
            e.consume();
            exitApplication();
        });

        primaryStage.show();

        // Initialize and start the emulator server on a background thread
        Thread.startVirtualThread(() -> {
            try {
                Emulator.initEmulator();
                Emulator.startServer();
            } catch (Exception e) {
                LOGGER.error("Failed to start emulator server", e);
            }
        });
    }

    private void applyTheme() {
        scene.getStylesheets().clear();
        String cssFile = darkMode ? "/gui/style.css" : "/gui/style-light.css";
        String css = getClass().getResource(cssFile) != null
                ? getClass().getResource(cssFile).toExternalForm()
                : "";
        if (!css.isEmpty()) {
            scene.getStylesheets().add(css);
        }
    }

    private Image createWindowIcon() {
        WritableImage img = new WritableImage(32, 32);
        var pw = img.getPixelWriter();
        Color blue = Color.web("#2980b9");
        Color darkBlue = Color.web("#1a5276");

        for (int x = 0; x < 32; x++) {
            for (int y = 0; y < 32; y++) {
                double dist = Math.sqrt(Math.pow(x - 16, 2) + Math.pow(y - 16, 2));
                if (dist < 14) {
                    pw.setColor(x, y, blue);
                } else if (dist < 16) {
                    pw.setColor(x, y, darkBlue);
                } else {
                    pw.setColor(x, y, Color.TRANSPARENT);
                }
            }
        }

        Color white = Color.WHITE;
        for (int y = 6; y < 26; y++) {
            int half = (y - 6) / 2;
            int left = 16 - half - 1;
            int right = 16 + half;
            if (left >= 2 && left < 30) pw.setColor(left, y, white);
            if (right >= 2 && right < 30) pw.setColor(right, y, white);
            if (left + 1 >= 2 && left + 1 < 30) pw.setColor(left + 1, y, white);
            if (right - 1 >= 2 && right - 1 < 30) pw.setColor(right - 1, y, white);
        }
        for (int x = 10; x < 22; x++) {
            pw.setColor(x, 18, white);
            pw.setColor(x, 19, white);
        }

        return img;
    }

    private void setupSystemTray() {
        if (!SystemTray.isSupported()) return;

        try {
            BufferedImage trayImage = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = trayImage.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(new java.awt.Color(41, 128, 185));
            g.fillOval(0, 0, 16, 16);
            g.setColor(java.awt.Color.WHITE);
            g.setFont(new Font("Arial", Font.BOLD, 11));
            g.drawString("A", 3, 13);
            g.dispose();

            PopupMenu popup = new PopupMenu();

            MenuItem showItem = new MenuItem("Show");
            showItem.addActionListener(e -> Platform.runLater(() -> {
                primaryStage.show();
                primaryStage.toFront();
            }));

            MenuItem exitItem = new MenuItem("Exit");
            exitItem.addActionListener(e -> Platform.runLater(this::exitApplication));

            popup.add(showItem);
            popup.addSeparator();
            popup.add(exitItem);

            trayIcon = new TrayIcon(trayImage, "Arcturus Morningstar", popup);
            trayIcon.setImageAutoSize(true);

            trayIcon.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    if (e.getClickCount() == 2) {
                        Platform.runLater(() -> {
                            primaryStage.show();
                            primaryStage.toFront();
                        });
                    }
                }
            });

            SystemTray.getSystemTray().add(trayIcon);
            DesktopNotifications.setTrayIcon(trayIcon);
        } catch (Exception e) {
            LOGGER.warn("Failed to setup system tray", e);
        }
    }

    private void exitApplication() {
        LOGGER.info("Shutdown requested from GUI...");

        if (trayIcon != null) {
            try {
                SystemTray.getSystemTray().remove(trayIcon);
            } catch (Exception ignored) {}
        }

        Thread.startVirtualThread(() -> {
            try {
                if (Emulator.isReady) {
                    var disposeMethod = Emulator.class.getDeclaredMethod("dispose");
                    disposeMethod.setAccessible(true);
                    disposeMethod.invoke(null);
                }
            } catch (Exception e) {
                LOGGER.error("Error during shutdown", e);
            } finally {
                Platform.runLater(() -> {
                    Platform.exit();
                    System.exit(0);
                });
            }
        });
    }

    private void registerGUILogAppender() {
        LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
        GUILogAppender appender = new GUILogAppender();
        appender.setContext(loggerContext);
        appender.setName("GUI");
        appender.start();

        Logger rootLogger = loggerContext.getLogger(Logger.ROOT_LOGGER_NAME);
        rootLogger.addAppender(appender);
    }

    public static void launchGUI(String[] args) {
        Application.launch(EmulatorGUI.class, args);
    }
}
