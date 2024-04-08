package com.eu.habbo.messages.rcon;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.users.Habbo;
import com.google.gson.Gson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class GivePoints extends RCONMessage<GivePoints.JSONGivePoints> {
    private static final Logger LOGGER = LoggerFactory.getLogger(GivePoints.class);


    public GivePoints() {
        super(JSONGivePoints.class);
    }

    @Override
    public void handle(Gson gson, JSONGivePoints object) {
        Habbo habbo = Emulator.getGameEnvironment().getHabboManager().getHabbo(object.user_id);

        if (habbo != null) {
            habbo.givePoints(object.type, object.points);
        } else {
            try (Connection connection = Emulator.getDatabase().getDataSource().getConnection(); PreparedStatement statement = connection.prepareStatement("INSERT INTO users_currency (`user_id`, `type`, `amount`) VALUES (?, ?, ?) ON DUPLICATE KEY UPDATE amount = amount + ?")) {
                statement.setInt(1, object.user_id);
                statement.setInt(2, object.type);
                statement.setInt(3, object.points);
                statement.setInt(4, object.points);
                statement.execute();
            } catch (SQLException e) {
                this.status = RCONMessage.SYSTEM_ERROR;
                LOGGER.error("Caught SQL exception", e);
            }

            this.message = "offline";
        }
    }

    static class JSONGivePoints {

        public int user_id;


        public int points;


        public int type;
    }
}
