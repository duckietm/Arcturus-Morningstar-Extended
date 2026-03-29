package com.eu.habbo.gui.controller;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.modtool.ModToolBanType;
import com.eu.habbo.habbohotel.rooms.Room;
import com.eu.habbo.habbohotel.users.Habbo;
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
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class PlayersTabController {

    private final ObservableList<PlayerEntry> players = FXCollections.observableArrayList();
    private TextField searchField;
    private Label countLabel;
    private Timeline refreshTimeline;

    public Tab createTab() {
        Tab tab = new Tab("Players");
        tab.setClosable(false);

        TableView<PlayerEntry> table = new TableView<>();
        table.setPlaceholder(new Label("No players online"));

        TableColumn<PlayerEntry, Number> idCol = new TableColumn<>("ID");
        idCol.setCellValueFactory(d -> new SimpleIntegerProperty(d.getValue().id));
        idCol.setPrefWidth(50);

        TableColumn<PlayerEntry, String> nameCol = new TableColumn<>("Username");
        nameCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().username));
        nameCol.setPrefWidth(120);

        TableColumn<PlayerEntry, String> mottoCol = new TableColumn<>("Motto");
        mottoCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().motto));
        mottoCol.setPrefWidth(140);

        TableColumn<PlayerEntry, String> roomCol = new TableColumn<>("Room");
        roomCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().room));
        roomCol.setPrefWidth(140);

        TableColumn<PlayerEntry, String> ipCol = new TableColumn<>("IP");
        ipCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().ip));
        ipCol.setPrefWidth(100);

        TableColumn<PlayerEntry, Void> actionsCol = new TableColumn<>("Actions");
        actionsCol.setPrefWidth(340);
        actionsCol.setCellFactory(col -> new TableCell<>() {
            private final Button kickBtn = new Button("Kick");
            private final Button muteBtn = new Button("Mute");
            private final Button alertBtn = new Button("Alert");
            private final Button banBtn = new Button("Ban");
            private final Button creditsBtn = new Button("Credits");
            private final Button badgeBtn = new Button("Badge");
            private final Button rankBtn = new Button("Rank");
            private final HBox box = new HBox(3, kickBtn, muteBtn, alertBtn, banBtn, creditsBtn, badgeBtn, rankBtn);

            {
                String smallStyle = "-fx-font-size: 10px; -fx-padding: 2 5;";
                kickBtn.getStyleClass().add("danger");
                kickBtn.setStyle(smallStyle);
                muteBtn.setStyle(smallStyle);
                alertBtn.setStyle(smallStyle);
                banBtn.getStyleClass().add("danger");
                banBtn.setStyle(smallStyle);
                creditsBtn.getStyleClass().add("success");
                creditsBtn.setStyle(smallStyle);
                badgeBtn.setStyle(smallStyle);
                rankBtn.getStyleClass().add("warning");
                rankBtn.setStyle(smallStyle);

                kickBtn.setOnAction(e -> getEntry().ifPresent(p -> kickPlayer(p.id)));
                muteBtn.setOnAction(e -> getEntry().ifPresent(p -> mutePlayer(p.id)));
                alertBtn.setOnAction(e -> getEntry().ifPresent(p -> alertPlayer(p.id)));
                banBtn.setOnAction(e -> getEntry().ifPresent(p -> banPlayer(p.id, p.username)));
                creditsBtn.setOnAction(e -> getEntry().ifPresent(p -> giveCredits(p.id, p.username)));
                badgeBtn.setOnAction(e -> getEntry().ifPresent(p -> giveBadge(p.id, p.username)));
                rankBtn.setOnAction(e -> getEntry().ifPresent(p -> changeRank(p.id, p.username)));
            }

            private Optional<PlayerEntry> getEntry() {
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

        table.getColumns().addAll(idCol, nameCol, mottoCol, roomCol, ipCol, actionsCol);

        searchField = new TextField();
        searchField.setPromptText("Search player...");

        FilteredList<PlayerEntry> filtered = new FilteredList<>(players, p -> true);
        searchField.textProperty().addListener((obs, oldVal, newVal) -> {
            filtered.setPredicate(entry -> {
                if (newVal == null || newVal.isEmpty()) return true;
                String lower = newVal.toLowerCase();
                return entry.username.toLowerCase().contains(lower)
                        || entry.room.toLowerCase().contains(lower)
                        || String.valueOf(entry.id).contains(lower);
            });
        });
        table.setItems(filtered);

        countLabel = new Label("Online: 0");
        countLabel.setStyle("-fx-font-weight: bold;");

        Button refreshBtn = new Button("Refresh");
        refreshBtn.setOnAction(e -> refreshPlayers());

        HBox toolbar = new HBox(10, searchField, countLabel, refreshBtn);
        toolbar.setPadding(new Insets(5));
        toolbar.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        HBox.setHgrow(searchField, Priority.ALWAYS);

        VBox content = new VBox(5, toolbar, table);
        content.setPadding(new Insets(5));
        VBox.setVgrow(table, Priority.ALWAYS);

        tab.setContent(content);

        refreshTimeline = new Timeline(new KeyFrame(Duration.seconds(5), e -> refreshPlayers()));
        refreshTimeline.setCycleCount(Timeline.INDEFINITE);
        refreshTimeline.play();

        tab.setOnClosed(e -> refreshTimeline.stop());

        return tab;
    }

    private void refreshPlayers() {
        if (!Emulator.isReady) return;

        try {
            ConcurrentHashMap<Integer, Habbo> onlineHabbos =
                    Emulator.getGameEnvironment().getHabboManager().getOnlineHabbos();

            var list = new java.util.ArrayList<PlayerEntry>();
            for (Habbo habbo : onlineHabbos.values()) {
                HabboInfo info = habbo.getHabboInfo();
                if (info == null) continue;

                String roomName = "";
                Room currentRoom = info.getCurrentRoom();
                if (currentRoom != null) {
                    roomName = currentRoom.getName();
                }

                list.add(new PlayerEntry(
                        info.getId(),
                        info.getUsername(),
                        info.getMotto(),
                        roomName,
                        info.getIpLogin()
                ));
            }

            Platform.runLater(() -> {
                players.setAll(list);
                countLabel.setText("Online: " + list.size());
            });
        } catch (Exception ignored) {
        }
    }

    private void kickPlayer(int userId) {
        if (!Emulator.isReady) return;
        Habbo habbo = Emulator.getGameEnvironment().getHabboManager().getHabbo(userId);
        if (habbo != null) {
            habbo.disconnect();
        }
    }

    private void mutePlayer(int userId) {
        if (!Emulator.isReady) return;
        Habbo habbo = Emulator.getGameEnvironment().getHabboManager().getHabbo(userId);
        if (habbo != null) {
            habbo.mute(300, false);
        }
    }

    private void alertPlayer(int userId) {
        if (!Emulator.isReady) return;

        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Alert Player");
        dialog.setHeaderText("Send alert message");
        dialog.setContentText("Message:");

        dialog.showAndWait().ifPresent(message -> {
            if (!message.isBlank()) {
                Habbo habbo = Emulator.getGameEnvironment().getHabboManager().getHabbo(userId);
                if (habbo != null) {
                    habbo.alert(message);
                }
            }
        });
    }

    private void banPlayer(int userId, String username) {
        if (!Emulator.isReady) return;

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Ban " + username);
        dialog.setHeaderText("Ban player: " + username);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(10));

        TextField reasonField = new TextField();
        reasonField.setPromptText("Reason");
        Spinner<Integer> durationSpinner = new Spinner<>(1, 525600, 60);
        durationSpinner.setEditable(true);

        grid.add(new Label("Reason:"), 0, 0);
        grid.add(reasonField, 1, 0);
        grid.add(new Label("Duration (min):"), 0, 1);
        grid.add(durationSpinner, 1, 1);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.showAndWait().ifPresent(result -> {
            if (result == ButtonType.OK) {
                String reason = reasonField.getText().isBlank() ? "Banned via GUI" : reasonField.getText();
                int durationSeconds = durationSpinner.getValue() * 60;
                int expireTimestamp = Emulator.getIntUnixTimestamp() + durationSeconds;
                Thread.startVirtualThread(() -> {
                    try {
                        Emulator.getGameEnvironment().getModToolManager().ban(
                                userId, null, reason, expireTimestamp, ModToolBanType.ACCOUNT, -1);
                    } catch (Exception ignored) {
                    }
                });
            }
        });
    }

    private void giveCredits(int userId, String username) {
        if (!Emulator.isReady) return;

        TextInputDialog dialog = new TextInputDialog("1000");
        dialog.setTitle("Give Credits");
        dialog.setHeaderText("Give credits to " + username);
        dialog.setContentText("Amount:");

        dialog.showAndWait().ifPresent(amountStr -> {
            try {
                int amount = Integer.parseInt(amountStr.trim());
                Habbo habbo = Emulator.getGameEnvironment().getHabboManager().getHabbo(userId);
                if (habbo != null) {
                    habbo.giveCredits(amount);
                }
            } catch (NumberFormatException ignored) {
            }
        });
    }

    private void giveBadge(int userId, String username) {
        if (!Emulator.isReady) return;

        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Give Badge");
        dialog.setHeaderText("Give badge to " + username);
        dialog.setContentText("Badge code:");

        dialog.showAndWait().ifPresent(code -> {
            if (!code.isBlank()) {
                Habbo habbo = Emulator.getGameEnvironment().getHabboManager().getHabbo(userId);
                if (habbo != null) {
                    habbo.addBadge(code.trim());
                }
            }
        });
    }

    private void changeRank(int userId, String username) {
        if (!Emulator.isReady) return;

        TextInputDialog dialog = new TextInputDialog("1");
        dialog.setTitle("Change Rank");
        dialog.setHeaderText("Change rank for " + username);
        dialog.setContentText("Rank ID:");

        dialog.showAndWait().ifPresent(rankStr -> {
            try {
                int rankId = Integer.parseInt(rankStr.trim());
                Thread.startVirtualThread(() -> {
                    try {
                        Emulator.getGameEnvironment().getHabboManager().setRank(userId, rankId);
                    } catch (Exception ignored) {
                    }
                });
            } catch (NumberFormatException ignored) {
            }
        });
    }

    private record PlayerEntry(int id, String username, String motto, String room, String ip) {}
}
