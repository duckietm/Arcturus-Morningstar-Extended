package com.eu.habbo.messages.rcon;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.items.Item;
import com.eu.habbo.habbohotel.users.Habbo;
import com.eu.habbo.habbohotel.users.HabboItem;
import com.eu.habbo.messages.outgoing.inventory.InventoryRefreshComposer;
import com.google.gson.Gson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class SendGift extends RCONMessage<SendGift.SendGiftJSON> {
    private static final Logger LOGGER = LoggerFactory.getLogger(SendGift.class);

    public SendGift() {
        super(SendGiftJSON.class);
    }

    @Override
    public void handle(Gson gson, SendGiftJSON json) {
        if (json.user_id < 0) {
            this.status = RCONMessage.STATUS_ERROR;
            this.message = Emulator.getTexts().getValue("commands.error.cmd_gift.user_not_found").replace("%username%", json.user_id + "");
            return;
        }

        if (json.itemid < 0) {
            this.status = RCONMessage.STATUS_ERROR;
            this.message = Emulator.getTexts().getValue("commands.error.cmd_gift.not_a_number");
            return;
        }

        Item baseItem = Emulator.getGameEnvironment().getItemManager().getItem(json.itemid);
        if (baseItem == null) {
            this.status = RCONMessage.STATUS_ERROR;
            this.message = Emulator.getTexts().getValue("commands.error.cmd_gift.not_found").replace("%itemid%", json.itemid + "");
            return;
        }

        boolean userFound;
        Habbo habbo;

        habbo = Emulator.getGameEnvironment().getHabboManager().getHabbo(json.user_id);

        userFound = habbo != null;
        String username = "";
        if (!userFound) {
            try (Connection connection = Emulator.getDatabase().getDataSource().getConnection(); PreparedStatement statement = connection.prepareStatement("SELECT * FROM users WHERE id = ? LIMIT 1")) {
                statement.setInt(1, json.user_id);
                try (ResultSet set = statement.executeQuery()) {
                    if (set.next()) {
                        username = set.getString("username");
                        userFound = true;
                    }
                }
            } catch (SQLException e) {
                LOGGER.error("Caught SQL exception", e);
            }
        } else {
            username = habbo.getHabboInfo().getUsername();
        }

        if (!userFound) {
            this.status = RCONMessage.STATUS_ERROR;
            this.message = Emulator.getTexts().getValue("commands.error.cmd_gift.user_not_found").replace("%username%", username);
            return;
        }

        HabboItem item = Emulator.getGameEnvironment().getItemManager().createItem(0, baseItem, 0, 0, "");
        Item giftItem = Emulator.getGameEnvironment().getItemManager().getItem((Integer) Emulator.getGameEnvironment().getCatalogManager().giftFurnis.values().toArray()[Emulator.getRandom().nextInt(Emulator.getGameEnvironment().getCatalogManager().giftFurnis.size())]);

        String extraData = "1\t" + item.getId();
        extraData += "\t0\t0\t0\t" + json.message + "\t0\t0";

        Emulator.getGameEnvironment().getItemManager().createGift(username, giftItem, extraData, 0, 0);

        this.message = Emulator.getTexts().getValue("commands.succes.cmd_gift").replace("%username%", username).replace("%itemname%", item.getBaseItem().getName());

        if (habbo != null) {
            habbo.getClient().sendResponse(new InventoryRefreshComposer());
        }
    }

    static class SendGiftJSON {

        public int user_id = -1;


        public int itemid = -1;


        public String message = "";
    }
}