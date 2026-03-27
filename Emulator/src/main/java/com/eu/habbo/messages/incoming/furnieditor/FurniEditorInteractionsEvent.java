package com.eu.habbo.messages.incoming.furnieditor;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.permissions.Permission;
import com.eu.habbo.messages.incoming.MessageHandler;
import com.eu.habbo.messages.outgoing.furnieditor.FurniEditorInteractionsComposer;
import com.eu.habbo.messages.outgoing.furnieditor.FurniEditorResultComposer;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class FurniEditorInteractionsEvent extends MessageHandler {

    private static List<String> cachedInteractions = null;

    @Override
    public void handle() throws Exception {
        if (!this.client.getHabbo().hasPermission(Permission.ACC_CATALOGFURNI)) {
            this.client.sendResponse(new FurniEditorResultComposer(false, "No permission"));
            return;
        }

        if (cachedInteractions == null) {
            synchronized (FurniEditorInteractionsEvent.class) {
                if (cachedInteractions == null) {
                    List<String> list = new ArrayList<>();
                    try (Connection connection = Emulator.getDatabase().getDataSource().getConnection();
                         Statement stmt = connection.createStatement();
                         ResultSet set = stmt.executeQuery("SELECT DISTINCT interaction_type FROM items_base WHERE interaction_type != '' ORDER BY interaction_type ASC")) {
                        while (set.next()) {
                            list.add(set.getString("interaction_type"));
                        }
                    }
                    cachedInteractions = Collections.unmodifiableList(list);
                }
            }
        }

        this.client.sendResponse(new FurniEditorInteractionsComposer(cachedInteractions));
    }
}
