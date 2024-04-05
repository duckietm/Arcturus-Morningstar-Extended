package com.eu.habbo.messages.rcon;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.rooms.RoomChatMessageBubbles;
import com.eu.habbo.habbohotel.users.Habbo;
import com.eu.habbo.habbohotel.users.HabboInfo;
import com.eu.habbo.habbohotel.users.subscriptions.Subscription;
import com.eu.habbo.messages.outgoing.rooms.users.RoomUserDataComposer;
import com.eu.habbo.messages.outgoing.users.MeMenuSettingsComposer;
import com.eu.habbo.messages.outgoing.users.UpdateUserLookComposer;
import com.google.gson.Gson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class ModifyUserSubscription extends RCONMessage<ModifyUserSubscription.JSON> {
    private static final Logger LOGGER = LoggerFactory.getLogger(ModifyUserSubscription.class);

    public ModifyUserSubscription() {
        super(ModifyUserSubscription.JSON.class);
    }

    @Override
    public void handle(Gson gson, JSON json) {
        try {

            if(json.user_id <= 0) {
                this.status = RCONMessage.HABBO_NOT_FOUND;
                this.message = "User not found";
                return;
            }

            if (!Emulator.getGameEnvironment().getSubscriptionManager().types.containsKey(json.type)) {
                this.status = RCONMessage.STATUS_ERROR;
                this.message = "%subscription% is not a valid subscription type".replace("%subscription%", json.type);
                return;
            }

            HabboInfo habbo = Emulator.getGameEnvironment().getHabboManager().getHabboInfo(json.user_id);

            if (habbo == null) {
                this.status = RCONMessage.HABBO_NOT_FOUND;
                this.message = "User not found";
                return;
            }

            if (json.action.equalsIgnoreCase("add") || json.action.equalsIgnoreCase("+") || json.action.equalsIgnoreCase("a")) {
                if (json.duration < 1) {
                    this.status = RCONMessage.STATUS_ERROR;
                    this.message = "duration must be > 0";
                    return;
                }

                habbo.getHabboStats().createSubscription(json.type, json.duration);
                this.status = RCONMessage.STATUS_OK;
                this.message = "Successfully added %time% seconds to %subscription% on %user%".replace("%time%", json.duration + "").replace("%user%", habbo.getUsername()).replace("%subscription%", json.type);
            } else if (json.action.equalsIgnoreCase("remove") || json.action.equalsIgnoreCase("-") || json.action.equalsIgnoreCase("r")) {
                Subscription s = habbo.getHabboStats().getSubscription(json.type);

                if (s == null) {
                    this.status = RCONMessage.STATUS_ERROR;
                    this.message = "%user% does not have the %subscription% subscription".replace("%user%", habbo.getUsername()).replace("%subscription%", json.type);
                    return;
                }

                if (json.duration != -1) {
                    if (json.duration < 1) {
                        this.status = RCONMessage.STATUS_ERROR;
                        this.message = "duration must be > 0 or -1 to remove all time";
                        return;
                    }

                    s.addDuration(-json.duration);
                    this.status = RCONMessage.STATUS_OK;
                    this.message = "Successfully removed %time% seconds from %subscription% on %user%".replace("%time%", json.duration + "").replace("%user%", habbo.getUsername()).replace("%subscription%", json.type);
                } else {
                    s.addDuration(-s.getRemaining());
                    this.status = RCONMessage.STATUS_OK;
                    this.message = "Successfully removed %subscription% sub from %user%".replace("%user%", habbo.getUsername()).replace("%subscription%", json.type);
                }
            }
            else {
                this.status = RCONMessage.STATUS_ERROR;
                this.message = "Invalid action specified. Must be add, +, remove or -";
            }
        }
        catch (Exception e) {
            this.status = RCONMessage.SYSTEM_ERROR;
            this.message = "Exception occurred";
            LOGGER.error("Exception occurred", e);
        }
    }

    static class JSON {

        public int user_id;

        public String type = ""; // Subscription type e.g. HABBO_CLUB

        public String action = ""; // Can be add or remove

        public int duration = -1; // Time to add/remove in seconds. -1 means remove subscription entirely

    }
}