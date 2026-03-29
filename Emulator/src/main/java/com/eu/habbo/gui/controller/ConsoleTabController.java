package com.eu.habbo.gui.controller;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import com.eu.habbo.core.consolecommands.ConsoleCommand;
import com.eu.habbo.gui.logging.GUILogAppender;
import com.eu.habbo.gui.notifications.DesktopNotifications;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Tab;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

import java.io.File;
import java.io.FileWriter;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public class ConsoleTabController {

    private static final int MAX_LOG_LINES = 5000;
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");

    private final TextArea logArea;
    private final TextField commandInput;
    private final CheckBox showDebug;
    private final CheckBox showInfo;
    private final CheckBox showWarn;
    private final CheckBox showError;
    private final CheckBox autoScroll;
    private int lineCount = 0;

    public ConsoleTabController() {
        this.logArea = new TextArea();
        this.logArea.setEditable(false);
        this.logArea.setWrapText(true);
        this.logArea.setStyle("-fx-font-family: 'Consolas', 'Monaco', monospace; -fx-font-size: 12px; "
                + "-fx-control-inner-background: #1e1e1e; -fx-text-fill: #cccccc;");

        this.commandInput = new TextField();
        this.commandInput.setPromptText("Enter command (stop, info, interactions, rconcommands, test, thankyou)...");
        this.commandInput.setStyle("-fx-font-family: 'Consolas', 'Monaco', monospace; -fx-font-size: 12px;");

        this.showDebug = new CheckBox("DEBUG");
        this.showInfo = new CheckBox("INFO");
        this.showWarn = new CheckBox("WARN");
        this.showError = new CheckBox("ERROR");
        this.autoScroll = new CheckBox("Auto-scroll");
        this.showInfo.setSelected(true);
        this.showWarn.setSelected(true);
        this.showError.setSelected(true);
        this.autoScroll.setSelected(true);

        this.commandInput.setOnAction(e -> executeCommand());

        GUILogAppender.addListener(this::onLogEvent);
    }

    public Tab createTab() {
        Tab tab = new Tab("Console & Log");
        tab.setClosable(false);

        Button sendButton = new Button("Send");
        sendButton.setOnAction(e -> executeCommand());

        Button clearButton = new Button("Clear");
        clearButton.setOnAction(e -> {
            logArea.clear();
            lineCount = 0;
        });

        Button exportButton = new Button("Export");
        exportButton.setOnAction(e -> exportLogs());

        HBox filterBox = new HBox(10, showDebug, showInfo, showWarn, showError, autoScroll, clearButton, exportButton);
        filterBox.setPadding(new Insets(5));

        HBox inputBox = new HBox(5, commandInput, sendButton);
        inputBox.setPadding(new Insets(5));
        HBox.setHgrow(commandInput, Priority.ALWAYS);

        VBox content = new VBox(5, filterBox, logArea, inputBox);
        content.setPadding(new Insets(5));
        VBox.setVgrow(logArea, Priority.ALWAYS);

        tab.setContent(content);
        return tab;
    }

    private void executeCommand() {
        String command = commandInput.getText().trim();
        if (!command.isEmpty()) {
            appendLog("[CMD] > " + command + "\n");
            ConsoleCommand.handle(command);
            commandInput.clear();
        }
    }

    private void onLogEvent(ILoggingEvent event) {
        Level level = event.getLevel();

        if (level == Level.DEBUG && !showDebug.isSelected()) return;
        if (level == Level.INFO && !showInfo.isSelected()) return;
        if (level == Level.WARN && !showWarn.isSelected()) return;
        if (level == Level.ERROR && !showError.isSelected()) return;

        String time = Instant.ofEpochMilli(event.getTimeStamp())
                .atZone(ZoneId.systemDefault())
                .format(TIME_FORMAT);

        String logLine = String.format("%s [%-5s] %s - %s%n",
                time,
                level.toString(),
                event.getLoggerName().substring(Math.max(0, event.getLoggerName().lastIndexOf('.') + 1)),
                event.getFormattedMessage());

        Platform.runLater(() -> appendLog(logLine));

        // Notify on errors when window might not be focused
        if (level == Level.ERROR) {
            DesktopNotifications.notify("Emulator Error",
                    event.getFormattedMessage(),
                    java.awt.TrayIcon.MessageType.ERROR);
        }
    }

    private void appendLog(String text) {
        logArea.appendText(text);
        lineCount++;

        if (autoScroll.isSelected()) {
            logArea.setScrollTop(Double.MAX_VALUE);
        }

        if (lineCount > MAX_LOG_LINES) {
            String content = logArea.getText();
            int cutIndex = content.indexOf('\n', content.length() / 4);
            if (cutIndex > 0) {
                logArea.setText(content.substring(cutIndex + 1));
                lineCount = lineCount * 3 / 4;
            }
        }
    }

    private void exportLogs() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Export Logs");
        chooser.setInitialFileName("emulator-log.txt");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Text Files", "*.txt"));
        File file = chooser.showSaveDialog(logArea.getScene().getWindow());
        if (file != null) {
            try (FileWriter writer = new FileWriter(file)) {
                writer.write(logArea.getText());
            } catch (Exception ignored) {
            }
        }
    }
}
