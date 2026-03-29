package com.eu.habbo.gui.controller;

import com.eu.habbo.Emulator;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Insets;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Tab;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Map;
import java.util.TreeMap;

public class ConfigTabController {

    private final ObservableList<Map.Entry<String, String>> configEntries = FXCollections.observableArrayList();
    private final TreeMap<String, String> modifiedEntries = new TreeMap<>();
    private TableView<Map.Entry<String, String>> tableView;
    private TextField searchField;

    public Tab createTab() {
        Tab tab = new Tab("Configuration");
        tab.setClosable(false);

        tableView = new TableView<>();
        tableView.setEditable(true);

        TableColumn<Map.Entry<String, String>, String> keyColumn = new TableColumn<>("Key");
        keyColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getKey()));
        keyColumn.setPrefWidth(350);
        keyColumn.setEditable(false);

        TableColumn<Map.Entry<String, String>, String> valueColumn = new TableColumn<>("Value");
        valueColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getValue()));
        valueColumn.setCellFactory(TextFieldTableCell.forTableColumn());
        valueColumn.setPrefWidth(450);
        valueColumn.setOnEditCommit(event -> {
            Map.Entry<String, String> entry = event.getRowValue();
            String newValue = event.getNewValue();
            modifiedEntries.put(entry.getKey(), newValue);
            entry.setValue(newValue);
        });

        tableView.getColumns().add(keyColumn);
        tableView.getColumns().add(valueColumn);

        searchField = new TextField();
        searchField.setPromptText("Search configuration key...");

        FilteredList<Map.Entry<String, String>> filteredData = new FilteredList<>(configEntries, p -> true);
        searchField.textProperty().addListener((obs, oldVal, newVal) -> {
            filteredData.setPredicate(entry -> {
                if (newVal == null || newVal.isEmpty()) return true;
                String lower = newVal.toLowerCase();
                return entry.getKey().toLowerCase().contains(lower)
                        || entry.getValue().toLowerCase().contains(lower);
            });
        });
        tableView.setItems(filteredData);

        Button reloadButton = new Button("Reload");
        reloadButton.setOnAction(e -> loadConfig());

        Button saveButton = new Button("Save Changes");
        saveButton.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white;");
        saveButton.setOnAction(e -> saveConfig());

        HBox toolbar = new HBox(10, searchField, reloadButton, saveButton);
        toolbar.setPadding(new Insets(5));
        HBox.setHgrow(searchField, Priority.ALWAYS);

        VBox content = new VBox(5, toolbar, tableView);
        content.setPadding(new Insets(5));
        VBox.setVgrow(tableView, Priority.ALWAYS);

        tab.setContent(content);

        // Defer loading until the emulator is ready (database may not be initialized yet)
        Thread.startVirtualThread(() -> {
            while (!Emulator.isReady) {
                try { Thread.sleep(500); } catch (InterruptedException ignored) {}
            }
            loadConfig();
        });

        return tab;
    }

    private void loadConfig() {
        if (!Emulator.isReady) {
            Platform.runLater(() -> {
                Alert alert = new Alert(Alert.AlertType.WARNING);
                alert.setTitle("Warning");
                alert.setContentText("Server is not running. Cannot load configuration.");
                alert.showAndWait();
            });
            return;
        }

        configEntries.clear();
        modifiedEntries.clear();

        try {
            TreeMap<String, String> settings = new TreeMap<>();

            try (Connection conn = Emulator.getDatabase().getDataSource().getConnection();
                 Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT `key`, `value` FROM emulator_settings ORDER BY `key`")) {
                while (rs.next()) {
                    settings.put(rs.getString("key"), rs.getString("value"));
                }
            }

            Platform.runLater(() -> {
                configEntries.setAll(settings.entrySet());
            });
        } catch (Exception e) {
            Platform.runLater(() -> {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Error");
                alert.setContentText("Failed to load configuration: " + e.getMessage());
                alert.showAndWait();
            });
        }
    }

    private void saveConfig() {
        if (!Emulator.isReady) return;
        if (modifiedEntries.isEmpty()) {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Info");
            alert.setContentText("No changes to save.");
            alert.showAndWait();
            return;
        }

        try {
            for (Map.Entry<String, String> entry : modifiedEntries.entrySet()) {
                Emulator.getConfig().register(entry.getKey(), entry.getValue());
            }
            Emulator.getConfig().saveToDatabase();

            int count = modifiedEntries.size();
            modifiedEntries.clear();

            Platform.runLater(() -> {
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Saved");
                alert.setContentText(count + " setting(s) saved successfully.");
                alert.showAndWait();
            });
        } catch (Exception e) {
            Platform.runLater(() -> {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Error");
                alert.setContentText("Failed to save: " + e.getMessage());
                alert.showAndWait();
            });
        }
    }
}
