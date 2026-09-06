package com.eu.habbo.messages.incoming.gamedata;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.gamedata.ExternalTextsWriter;
import com.eu.habbo.habbohotel.permissions.Permission;
import com.eu.habbo.habbohotel.users.Habbo;
import com.eu.habbo.messages.incoming.MessageHandler;
import com.eu.habbo.messages.outgoing.gamedata.ExternalTextUpdatedComposer;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** CUSTOM packet 10091: staff sets one external text (e.g. the name of an effect, key fx_<id>). */
public class ExternalTextUpdateEvent extends MessageHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(ExternalTextUpdateEvent.class);
    private static final int MAX_VALUE_LENGTH = 128;

    @Override
    public void handle() throws Exception {
        Habbo habbo = this.client.getHabbo();
        if (habbo == null) return;

        if (!habbo.hasPermission(Permission.ACC_CATALOGFURNI)) {
            this.client.sendResponse(new ExternalTextUpdatedComposer(false, "", "", "No permission"));
            return;
        }

        String key = this.packet.readString();
        String value = this.packet.readString();

        if (key == null || !key.matches("[A-Za-z0-9_.\\-]{1,64}")) {
            this.client.sendResponse(new ExternalTextUpdatedComposer(false, key == null ? "" : key, "", "Invalid key"));
            return;
        }

        value = value == null ? "" : value.replaceAll("\\p{Cntrl}", " ").trim();
        if (value.length() > MAX_VALUE_LENGTH) value = value.substring(0, MAX_VALUE_LENGTH);
        if (value.isEmpty()) {
            this.client.sendResponse(new ExternalTextUpdatedComposer(false, key, "", "Empty value"));
            return;
        }

        try {
            ExternalTextsWriter.write(key, value);
        } catch (IOException exception) {
            LOGGER.error("Unable to update external text {}", key, exception);
            this.client.sendResponse(new ExternalTextUpdatedComposer(false, key, value, exception.getMessage()));
            return;
        }

        LOGGER.info("External text {} set to \"{}\" by {}", key, value, habbo.getHabboInfo().getUsername());
        Emulator.getGameServer().getGameClientManager().sendBroadcastResponse(new ExternalTextUpdatedComposer(true, key, value, ""));
    }
}
