package com.eu.habbo.gui.controller;

import com.eu.habbo.Emulator;
import com.eu.habbo.gui.notifications.DesktopNotifications;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.util.Duration;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class DashboardTabController {

    private static final int MAX_DATA_POINTS = 60;
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm:ss");

    private Label usersOnlineLabel;
    private Label activeRoomsLabel;
    private Label uptimeLabel;
    private Label ramUsageLabel;
    private Label threadsLabel;
    private Label totalMemoryLabel;
    private Label cpuCoresLabel;
    private Label serverStatusLabel;
    private Circle statusCircle;

    private Button startButton;
    private Button stopButton;
    private Button restartButton;

    // Charts
    private XYChart.Series<String, Number> usersSeries;
    private XYChart.Series<String, Number> ramSeries;
    private XYChart.Series<String, Number> roomsSeries;

    // Auto-restart
    private CheckBox autoRestartCheck;
    private Spinner<Integer> restartHourSpinner;
    private Spinner<Integer> restartMinuteSpinner;
    private Label nextRestartLabel;
    private ScheduledExecutorService restartScheduler;
    private ScheduledFuture<?> restartTask;

    private Timeline refreshTimeline;
    private volatile boolean serverRunning = false;

    public Tab createTab() {
        Tab tab = new Tab("Dashboard");
        tab.setClosable(false);

        Label title = new Label("Arcturus Morningstar - Server Dashboard");
        title.setFont(Font.font("Segoe UI", FontWeight.BOLD, 18));

        // Status indicator
        statusCircle = new Circle(8);
        statusCircle.setFill(Color.web("#f39c12"));
        serverStatusLabel = new Label("Starting...");
        serverStatusLabel.getStyleClass().add("status-starting");
        HBox statusBox = new HBox(8, statusCircle, serverStatusLabel);
        statusBox.setAlignment(Pos.CENTER_LEFT);

        // Control buttons
        startButton = new Button("Start");
        startButton.getStyleClass().add("success");
        startButton.setDisable(true);
        startButton.setOnAction(e -> startServer());

        stopButton = new Button("Stop");
        stopButton.getStyleClass().add("danger");
        stopButton.setDisable(true);
        stopButton.setOnAction(e -> stopServer());

        restartButton = new Button("Restart");
        restartButton.getStyleClass().add("warning");
        restartButton.setDisable(true);
        restartButton.setOnAction(e -> restartServer());

        HBox controlBox = new HBox(10, startButton, stopButton, restartButton);
        controlBox.setAlignment(Pos.CENTER_LEFT);

        HBox topBar = new HBox(30, statusBox, controlBox);
        topBar.setAlignment(Pos.CENTER_LEFT);
        topBar.setPadding(new Insets(5, 0, 5, 0));

        // Stats grid
        GridPane statsGrid = new GridPane();
        statsGrid.setHgap(20);
        statsGrid.setVgap(10);
        statsGrid.setPadding(new Insets(10, 0, 0, 0));

        usersOnlineLabel = createValueLabel("0");
        activeRoomsLabel = createValueLabel("0");
        uptimeLabel = createValueLabel("0s");
        ramUsageLabel = createValueLabel("0 MB");
        threadsLabel = createValueLabel("0");
        totalMemoryLabel = createValueLabel("0 MB");
        cpuCoresLabel = createValueLabel(String.valueOf(Runtime.getRuntime().availableProcessors()));

        int row = 0;
        statsGrid.add(createHeaderLabel("Users Online"), 0, row);
        statsGrid.add(usersOnlineLabel, 1, row);
        statsGrid.add(createHeaderLabel("Active Rooms"), 2, row);
        statsGrid.add(activeRoomsLabel, 3, row++);
        statsGrid.add(createHeaderLabel("Uptime"), 0, row);
        statsGrid.add(uptimeLabel, 1, row);
        statsGrid.add(createHeaderLabel("RAM Usage"), 2, row);
        statsGrid.add(ramUsageLabel, 3, row++);
        statsGrid.add(createHeaderLabel("Total Memory"), 0, row);
        statsGrid.add(totalMemoryLabel, 1, row);
        statsGrid.add(createHeaderLabel("Active Threads"), 2, row);
        statsGrid.add(threadsLabel, 3, row++);
        statsGrid.add(createHeaderLabel("CPU Cores"), 0, row);
        statsGrid.add(cpuCoresLabel, 1, row++);

        // Charts
        HBox chartsBox = new HBox(10, createUsersChart(), createRamChart(), createRoomsChart());
        chartsBox.setPadding(new Insets(5, 0, 0, 0));
        HBox.setHgrow(chartsBox.getChildren().get(0), Priority.ALWAYS);
        HBox.setHgrow(chartsBox.getChildren().get(1), Priority.ALWAYS);
        HBox.setHgrow(chartsBox.getChildren().get(2), Priority.ALWAYS);

        // Auto-restart section
        HBox autoRestartBox = createAutoRestartSection();

        VBox content = new VBox(8, title, topBar, new Separator(), statsGrid, chartsBox, new Separator(), autoRestartBox);
        content.setPadding(new Insets(15));
        content.setAlignment(Pos.TOP_LEFT);
        VBox.setVgrow(chartsBox, Priority.ALWAYS);

        ScrollPane scrollPane = new ScrollPane(content);
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);
        tab.setContent(scrollPane);
        startRefreshTimer();
        tab.setOnClosed(e -> {
            stopRefreshTimer();
            if (restartScheduler != null) restartScheduler.shutdownNow();
        });

        return tab;
    }

    private LineChart<String, Number> createUsersChart() {
        CategoryAxis xAxis = new CategoryAxis();
        NumberAxis yAxis = new NumberAxis();
        yAxis.setLabel("Users");
        LineChart<String, Number> chart = new LineChart<>(xAxis, yAxis);
        chart.setTitle("Users Online");
        chart.setLegendVisible(false);
        chart.setCreateSymbols(false);
        chart.setAnimated(false);
        chart.setPrefHeight(200);
        chart.getStyleClass().add("dark-chart");
        usersSeries = new XYChart.Series<>();
        chart.getData().add(usersSeries);
        return chart;
    }

    private LineChart<String, Number> createRamChart() {
        CategoryAxis xAxis = new CategoryAxis();
        NumberAxis yAxis = new NumberAxis();
        yAxis.setLabel("MB");
        LineChart<String, Number> chart = new LineChart<>(xAxis, yAxis);
        chart.setTitle("RAM Usage");
        chart.setLegendVisible(false);
        chart.setCreateSymbols(false);
        chart.setAnimated(false);
        chart.setPrefHeight(200);
        chart.getStyleClass().add("dark-chart");
        ramSeries = new XYChart.Series<>();
        chart.getData().add(ramSeries);
        return chart;
    }

    private LineChart<String, Number> createRoomsChart() {
        CategoryAxis xAxis = new CategoryAxis();
        NumberAxis yAxis = new NumberAxis();
        yAxis.setLabel("Rooms");
        LineChart<String, Number> chart = new LineChart<>(xAxis, yAxis);
        chart.setTitle("Active Rooms");
        chart.setLegendVisible(false);
        chart.setCreateSymbols(false);
        chart.setAnimated(false);
        chart.setPrefHeight(200);
        chart.getStyleClass().add("dark-chart");
        roomsSeries = new XYChart.Series<>();
        chart.getData().add(roomsSeries);
        return chart;
    }

    private HBox createAutoRestartSection() {
        autoRestartCheck = new CheckBox("Auto-Restart");
        restartHourSpinner = new Spinner<>(0, 23, 4);
        restartHourSpinner.setPrefWidth(70);
        restartHourSpinner.setEditable(true);
        restartMinuteSpinner = new Spinner<>(0, 59, 0);
        restartMinuteSpinner.setPrefWidth(70);
        restartMinuteSpinner.setEditable(true);
        nextRestartLabel = new Label("");
        nextRestartLabel.setStyle("-fx-font-style: italic;");

        autoRestartCheck.setOnAction(e -> toggleAutoRestart());
        restartHourSpinner.valueProperty().addListener((o, ov, nv) -> updateNextRestartLabel());
        restartMinuteSpinner.valueProperty().addListener((o, ov, nv) -> updateNextRestartLabel());

        restartHourSpinner.setDisable(true);
        restartMinuteSpinner.setDisable(true);

        HBox box = new HBox(10, autoRestartCheck, new Label("at"), restartHourSpinner, new Label(":"), restartMinuteSpinner, nextRestartLabel);
        box.setAlignment(Pos.CENTER_LEFT);
        box.setPadding(new Insets(5, 0, 0, 0));
        return box;
    }

    private void toggleAutoRestart() {
        boolean enabled = autoRestartCheck.isSelected();
        restartHourSpinner.setDisable(!enabled);
        restartMinuteSpinner.setDisable(!enabled);

        if (enabled) {
            updateNextRestartLabel();
            startAutoRestartScheduler();
        } else {
            nextRestartLabel.setText("");
            stopAutoRestartScheduler();
        }
    }

    private void updateNextRestartLabel() {
        if (autoRestartCheck.isSelected()) {
            nextRestartLabel.setText(String.format("Next restart: %02d:%02d",
                    restartHourSpinner.getValue(), restartMinuteSpinner.getValue()));
        }
    }

    private void startAutoRestartScheduler() {
        stopAutoRestartScheduler();
        restartScheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "AutoRestartChecker");
            t.setDaemon(true);
            return t;
        });
        restartTask = restartScheduler.scheduleAtFixedRate(() -> {
            if (!autoRestartCheck.isSelected() || !serverRunning) return;
            LocalTime now = LocalTime.now();
            if (now.getHour() == restartHourSpinner.getValue() && now.getMinute() == restartMinuteSpinner.getValue()) {
                Platform.runLater(this::restartServer);
            }
        }, 0, 60, TimeUnit.SECONDS);
    }

    private void stopAutoRestartScheduler() {
        if (restartTask != null) restartTask.cancel(false);
        if (restartScheduler != null) restartScheduler.shutdownNow();
    }

    private void startServer() {
        startButton.setDisable(true);
        updateStatus("Starting...", "#f39c12", "status-starting");

        Thread.startVirtualThread(() -> {
            try {
                Emulator.startServer();
            } catch (Exception e) {
                Platform.runLater(() -> updateStatus("Error", "#e74c3c", "status-offline"));
            }
        });
    }

    private void stopServer() {
        stopButton.setDisable(true);
        restartButton.setDisable(true);
        updateStatus("Stopping...", "#f39c12", "status-starting");

        Thread.startVirtualThread(() -> {
            try {
                var disposeMethod = Emulator.class.getDeclaredMethod("dispose");
                disposeMethod.setAccessible(true);
                disposeMethod.invoke(null);

                serverRunning = false;
                Platform.runLater(() -> {
                    updateStatus("Offline", "#e74c3c", "status-offline");
                    startButton.setDisable(false);
                    stopButton.setDisable(true);
                    restartButton.setDisable(true);
                });
                DesktopNotifications.notify("Server Offline", "Arcturus Morningstar has been stopped.", java.awt.TrayIcon.MessageType.WARNING);
            } catch (Exception e) {
                Platform.runLater(() -> updateStatus("Error", "#e74c3c", "status-offline"));
            }
        });
    }

    private void restartServer() {
        restartButton.setDisable(true);
        stopButton.setDisable(true);
        updateStatus("Restarting...", "#f39c12", "status-starting");

        Thread.startVirtualThread(() -> {
            try {
                var disposeMethod = Emulator.class.getDeclaredMethod("dispose");
                disposeMethod.setAccessible(true);
                disposeMethod.invoke(null);

                serverRunning = false;
                Thread.sleep(2000);

                Emulator.startServer();
            } catch (Exception e) {
                Platform.runLater(() -> updateStatus("Error", "#e74c3c", "status-offline"));
            }
        });
    }

    private void updateStatus(String text, String color, String styleClass) {
        Platform.runLater(() -> {
            statusCircle.setFill(Color.web(color));
            serverStatusLabel.setText(text);
            serverStatusLabel.getStyleClass().removeAll("status-online", "status-offline", "status-starting");
            serverStatusLabel.getStyleClass().add(styleClass);
        });
    }

    private void startRefreshTimer() {
        refreshTimeline = new Timeline(new KeyFrame(Duration.seconds(3), e -> refreshStats()));
        refreshTimeline.setCycleCount(Timeline.INDEFINITE);
        refreshTimeline.play();
    }

    private void stopRefreshTimer() {
        if (refreshTimeline != null) {
            refreshTimeline.stop();
        }
    }

    private void refreshStats() {
        if (!Emulator.isReady) {
            if (!serverRunning) return;
            return;
        }

        if (!serverRunning) {
            serverRunning = true;
            Platform.runLater(() -> {
                updateStatus("Online", "#2ecc71", "status-online");
                startButton.setDisable(true);
                stopButton.setDisable(false);
                restartButton.setDisable(false);
            });
            DesktopNotifications.notify("Server Online", "Arcturus Morningstar is ready.", java.awt.TrayIcon.MessageType.INFO);
        }

        try {
            Runtime rt = Emulator.getRuntime();
            long usedMB = (rt.totalMemory() - rt.freeMemory()) / (1024 * 1024);
            long totalMB = rt.maxMemory() / (1024 * 1024);
            int onlineUsers = Emulator.getGameEnvironment().getHabboManager().getOnlineCount();
            int activeRooms = Emulator.getGameEnvironment().getRoomManager().getActiveRooms().size();
            int activeThreads = Thread.activeCount();
            String uptime = formatUptime(Emulator.getOnlineTime());
            String timeNow = LocalTime.now().format(TIME_FMT);

            Platform.runLater(() -> {
                usersOnlineLabel.setText(String.valueOf(onlineUsers));
                activeRoomsLabel.setText(String.valueOf(activeRooms));
                uptimeLabel.setText(uptime);
                ramUsageLabel.setText(usedMB + " / " + totalMB + " MB");
                threadsLabel.setText(String.valueOf(activeThreads));
                totalMemoryLabel.setText(totalMB + " MB");

                // Update charts
                addChartData(usersSeries, timeNow, onlineUsers);
                addChartData(ramSeries, timeNow, usedMB);
                addChartData(roomsSeries, timeNow, activeRooms);
            });
        } catch (Exception ignored) {
        }
    }

    private void addChartData(XYChart.Series<String, Number> series, String time, Number value) {
        series.getData().add(new XYChart.Data<>(time, value));
        if (series.getData().size() > MAX_DATA_POINTS) {
            series.getData().remove(0);
        }
    }

    private String formatUptime(int seconds) {
        long days = TimeUnit.SECONDS.toDays(seconds);
        long hours = TimeUnit.SECONDS.toHours(seconds) - (days * 24);
        long minutes = TimeUnit.SECONDS.toMinutes(seconds) - (TimeUnit.SECONDS.toHours(seconds) * 60);
        long secs = seconds - (TimeUnit.SECONDS.toMinutes(seconds) * 60);

        StringBuilder sb = new StringBuilder();
        if (days > 0) sb.append(days).append("d ");
        if (hours > 0) sb.append(hours).append("h ");
        if (minutes > 0) sb.append(minutes).append("m ");
        sb.append(secs).append("s");
        return sb.toString();
    }

    private Label createHeaderLabel(String text) {
        Label label = new Label(text);
        label.setFont(Font.font("Segoe UI", FontWeight.BOLD, 13));
        label.setMinWidth(120);
        return label;
    }

    private Label createValueLabel(String text) {
        Label label = new Label(text);
        label.setFont(Font.font("Segoe UI", FontWeight.NORMAL, 13));
        return label;
    }
}
