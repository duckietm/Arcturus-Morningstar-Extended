package com.eu.habbo.messages.rcon;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.users.Habbo;
import com.google.gson.Gson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class GivePixels extends RCONMessage<GivePixels.JSONGivePixels> {
    private static final Logger LOGGER = LoggerFactory.getLogger(GivePixels.class);


    public GivePixels() {
        super(JSONGivePixels.class);
    }

    @Override
    public void handle(Gson gson, JSONGivePixels object) {
        Habbo habbo = Emulator.getGameEnvironment().getHabboManager().getHabbo(object.user_id);

        if (habbo != null) {
            habbo.givePixels(object.pixels);
        } else {
            try (Connection connection = Emulator.getDatabase().getDataSource().getConnection(); PreparedStatement statement = connection.prepareStatement("UPDATE users_currency SET users_currency.amount = users_currency.amount + ? WHERE users_currency.user_id = ? AND users_currency.type = 0")) {
                statement.setInt(1, object.pixels);
                statement.setInt(2, object.user_id);
                statement.execute();
            } catch (SQLException e) {
                this.status = RCONMessage.SYSTEM_ERROR;
                LOGGER.error("Caught SQL exception", e);
            }

            this.message = "offline";
        }
    }

    static class JSONGivePixels {

        public int user_id;


        public int pixels;
    }
}
