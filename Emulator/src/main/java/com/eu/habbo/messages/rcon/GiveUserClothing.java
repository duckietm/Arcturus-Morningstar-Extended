package com.eu.habbo.messages.rcon;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.gameclients.GameClient;
import com.eu.habbo.habbohotel.users.Habbo;
import com.eu.habbo.messages.outgoing.generic.alerts.BubbleAlertComposer;
import com.eu.habbo.messages.outgoing.generic.alerts.BubbleAlertKeys;
import com.eu.habbo.messages.outgoing.users.UserClothesComposer;
import com.google.gson.Gson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class GiveUserClothing extends RCONMessage<GiveUserClothing.JSONGiveUserClothing> {
    private static final Logger LOGGER = LoggerFactory.getLogger(GiveUserClothing.class);

    public GiveUserClothing() {
        super(GiveUserClothing.JSONGiveUserClothing.class);
    }

    @Override
    public void handle(Gson gson, GiveUserClothing.JSONGiveUserClothing object) {
        Habbo habbo = Emulator.getGameEnvironment().getHabboManager().getHabbo(object.user_id);

        try (Connection connection = Emulator.getDatabase().getDataSource().getConnection(); PreparedStatement statement = connection.prepareStatement("INSERT INTO users_clothing (user_id, clothing_id) VALUES (?, ?)")) {
            statement.setInt(1, object.user_id);
            statement.setInt(2, object.clothing_id);
            statement.execute();
        } catch (SQLException e) {
            LOGGER.error("Caught SQL exception", e);
        }

        if (habbo != null) {
            GameClient client = habbo.getClient();

            if (client != null) {
                habbo.getInventory().getWardrobeComponent().getClothing().add(object.clothing_id);
                client.sendResponse(new UserClothesComposer(habbo));
                client.sendResponse(new BubbleAlertComposer(BubbleAlertKeys.FIGURESET_REDEEMED.key));
            }
        }
    }

    static class JSONGiveUserClothing {
        public int user_id;
        public int clothing_id;
    }
}
