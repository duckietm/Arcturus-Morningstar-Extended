package com.eu.habbo.messages.rcon;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.users.Habbo;
import com.google.gson.Gson;
import lombok.extern.slf4j.Slf4j;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

@Slf4j
public class MuteUser extends RCONMessage<MuteUser.JSON> {
    public MuteUser() {
        super(MuteUser.JSON.class);
    }

    @Override
    public void handle(Gson gson, JSON json) {
        Habbo habbo = Emulator.getGameEnvironment().getHabboManager().getHabbo(json.user_id);

        if (habbo != null) {
            if (json.duration == 0) {
                habbo.unMute();
            } else {
                habbo.mute(json.duration, false);
            }
        } else {
            try (Connection connection = Emulator.getDatabase().getDataSource().getConnection(); PreparedStatement statement = connection.prepareStatement("UPDATE users_settings SET mute_end_timestamp = ? WHERE user_id = ? LIMIT 1")) {
                statement.setInt(1, Emulator.getIntUnixTimestamp() + json.duration);
                statement.setInt(2, json.user_id);
                if (statement.executeUpdate() == 0) {
                    this.status = HABBO_NOT_FOUND;
                }
            } catch (SQLException e) {
                log.error("Caught SQL exception", e);
            }
        }
    }

    static class JSON {

        public int user_id;
        public int duration;
    }
}
