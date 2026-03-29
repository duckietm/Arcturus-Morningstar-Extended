package com.eu.habbo.gui.controller;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.rooms.Room;
import com.eu.habbo.habbohotel.users.HabboInfo;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public class RoomsTabController {

    private final ObservableList<RoomEntry> rooms = FXCollections.observableArrayList();
    private TextField searchField;
    private Label countLabel;
    private Timeline refreshTimeline;

    public Tab createTab() {
        Tab tab = new Tab("Rooms");
        tab.setClosable(false);

        TableView<RoomEntry> table = new TableView<>();
        table.setPlaceholder(new Label("No active rooms"));

        TableColumn<RoomEntry, Number> idCol = new TableColumn<>("ID");
        idCol.setCellValueFactory(d -> new SimpleIntegerProperty(d.getValue().id));
        idCol.setPrefWidth(50);

        TableColumn<RoomEntry, String> nameCol = new TableColumn<>("Name");
        nameCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().name));
        nameCol.setPrefWidth(170);

        TableColumn<RoomEntry, String> ownerCol = new TableColumn<>("Owner");
        ownerCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().owner));
        ownerCol.setPrefWidth(110);

        TableColumn<RoomEntry, String> usersCol = new TableColumn<>("Users");
        usersCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().users + " / " + d.getValue().maxUsers));
        usersCol.setPrefWidth(70);

        TableColumn<RoomEntry, String> stateCol = new TableColumn<>("State");
        stateCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().state));
        stateCol.setPrefWidth(80);

        TableColumn<RoomEntry, Void> actionsCol = new TableColumn<>("Actions");
        actionsCol.setPrefWidth(240);
        actionsCol.setCellFactory(col -> new TableCell<>() {
            private final Button closeBtn = new Button("Close");
            private final Button ownerBtn = new Button("Owner");
            private final Button maxBtn = new Button("Max Users");
            private final HBox box = new HBox(5, closeBtn, ownerBtn, maxBtn);

            {
                String smallStyle = "-fx-font-size: 10px; -fx-padding: 2 6;";
                closeBtn.getStyleClass().add("danger");
                closeBtn.setStyle(smallStyle);
                ownerBtn.setStyle(smallStyle);
                maxBtn.setStyle(smallStyle);

                closeBtn.setOnAction(e -> getEntry().ifPresent(r -> closeRoom(r.id)));
                ownerBtn.setOnAction(e -> getEntry().ifPresent(r -> changeOwner(r.id)));
                maxBtn.setOnAction(e -> getEntry().ifPresent(r -> setMaxUsers(r.id)));
            }

            private Optional<RoomEntry> getEntry() {
                if (getIndex() >= 0 && getIndex() < getTableView().getItems().size()) {
                    return Optional.of(getTableView().getItems().get(getIndex()));
                }
                return Optional.empty();
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : box);
            }
        });

        table.getColumns().addAll(idCol, nameCol, ownerCol, usersCol, stateCol, actionsCol);

        searchField = new TextField();
        searchField.setPromptText("Search room...");

        FilteredList<RoomEntry> filtered = new FilteredList<>(rooms, p -> true);
        searchField.textProperty().addListener((obs, oldVal, newVal) -> {
            filtered.setPredicate(entry -> {
                if (newVal == null || newVal.isEmpty()) return true;
                String lower = newVal.toLowerCase();
                return entry.name.toLowerCase().contains(lower)
                        || entry.owner.toLowerCase().contains(lower)
                        || String.valueOf(entry.id).contains(lower);
            });
        });
        table.setItems(filtered);

        countLabel = new Label("Active: 0");
        countLabel.setStyle("-fx-font-weight: bold;");

        Button refreshBtn = new Button("Refresh");
        refreshBtn.setOnAction(e -> refreshRooms());

        HBox toolbar = new HBox(10, searchField, countLabel, refreshBtn);
        toolbar.setPadding(new Insets(5));
        toolbar.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(searchField, Priority.ALWAYS);

        VBox content = new VBox(5, toolbar, table);
        content.setPadding(new Insets(5));
        VBox.setVgrow(table, Priority.ALWAYS);

        tab.setContent(content);

        refreshTimeline = new Timeline(new KeyFrame(Duration.seconds(5), e -> refreshRooms()));
        refreshTimeline.setCycleCount(Timeline.INDEFINITE);
        refreshTimeline.play();

        tab.setOnClosed(e -> refreshTimeline.stop());

        return tab;
    }

    private void refreshRooms() {
        if (!Emulator.isReady) return;

        try {
            ArrayList<Room> activeRooms = Emulator.getGameEnvironment().getRoomManager().getActiveRooms();
            List<RoomEntry> list = new java.util.ArrayList<>();

            for (Room room : activeRooms) {
                list.add(new RoomEntry(
                        room.getId(),
                        room.getName(),
                        room.getOwnerName(),
                        room.getUserCount(),
                        room.getUsersMax(),
                        room.getState().name()
                ));
            }

            list.sort(Comparator.comparingInt((RoomEntry r) -> r.users).reversed());

            Platform.runLater(() -> {
                rooms.setAll(list);
                countLabel.setText("Active: " + list.size());
            });
        } catch (Exception ignored) {
        }
    }

    private void closeRoom(int roomId) {
        if (!Emulator.isReady) return;
        Thread.startVirtualThread(() -> {
            Room room = Emulator.getGameEnvironment().getRoomManager().loadRoom(roomId);
            if (room != null) {
                Emulator.getGameEnvironment().getRoomManager().unloadRoom(room);
                Platform.runLater(this::refreshRooms);
            }
        });
    }

    private void changeOwner(int roomId) {
        if (!Emulator.isReady) return;

        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Change Owner");
        dialog.setHeaderText("Change room owner");
        dialog.setContentText("New owner username:");

        dialog.showAndWait().ifPresent(username -> {
            if (username.isBlank()) return;
            Thread.startVirtualThread(() -> {
                HabboInfo info = Emulator.getGameEnvironment().getHabboManager().getHabboInfo(
                        Emulator.getGameEnvironment().getHabboManager().getHabbo(username) != null
                                ? Emulator.getGameEnvironment().getHabboManager().getHabbo(username).getHabboInfo().getId()
                                : -1);
                if (info == null) {
                    info = com.eu.habbo.habbohotel.users.HabboManager.getOfflineHabboInfo(username);
                }
                if (info != null) {
                    Room room = Emulator.getGameEnvironment().getRoomManager().loadRoom(roomId);
                    if (room != null) {
                        room.setOwnerId(info.getId());
                        room.setOwnerName(info.getUsername());
                        room.setNeedsUpdate(true);
                        room.save();
                        Platform.runLater(this::refreshRooms);
                    }
                }
            });
        });
    }

    private void setMaxUsers(int roomId) {
        if (!Emulator.isReady) return;

        TextInputDialog dialog = new TextInputDialog("25");
        dialog.setTitle("Set Max Users");
        dialog.setHeaderText("Set maximum users for room");
        dialog.setContentText("Max users:");

        dialog.showAndWait().ifPresent(maxStr -> {
            try {
                int max = Integer.parseInt(maxStr.trim());
                Thread.startVirtualThread(() -> {
                    Room room = Emulator.getGameEnvironment().getRoomManager().loadRoom(roomId);
                    if (room != null) {
                        room.setUsersMax(max);
                        room.setNeedsUpdate(true);
                        room.save();
                        Platform.runLater(this::refreshRooms);
                    }
                });
            } catch (NumberFormatException ignored) {
            }
        });
    }

    private record RoomEntry(int id, String name, String owner, int users, int maxUsers, String state) {}
}
