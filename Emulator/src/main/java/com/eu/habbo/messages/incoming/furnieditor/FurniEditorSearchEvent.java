package com.eu.habbo.messages.incoming.furnieditor;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.permissions.Permission;
import com.eu.habbo.messages.incoming.MessageHandler;
import com.eu.habbo.messages.outgoing.furnieditor.FurniEditorResultComposer;
import com.eu.habbo.messages.outgoing.furnieditor.FurniEditorSearchComposer;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class FurniEditorSearchEvent extends MessageHandler {
    private static final org.slf4j.Logger LOGGER = org.slf4j.LoggerFactory.getLogger(FurniEditorSearchEvent.class);

    @Override
    public void handle() throws Exception {
        LOGGER.info("[FurniEditorSearch] Received packet from user {}", this.client.getHabbo().getHabboInfo().getUsername());

        if (!this.client.getHabbo().hasPermission(Permission.ACC_CATALOGFURNI)) {
            LOGGER.warn("[FurniEditorSearch] No permission for user {}", this.client.getHabbo().getHabboInfo().getUsername());
            this.client.sendResponse(new FurniEditorResultComposer(false, "No permission"));
            return;
        }

        String query = this.packet.readString();
        String type = this.packet.readString();
        int page = this.packet.readInt();
        int limit = 20;
        int offset = (page - 1) * limit;

        List<Object> params = new ArrayList<>();
        List<Object> countParams = new ArrayList<>();
        StringBuilder where = new StringBuilder(" WHERE 1=1");

        if (!query.isEmpty()) {
            try {
                int numericQuery = Integer.parseInt(query);
                where.append(" AND (id = ? OR sprite_id = ?)");
                params.add(numericQuery);
                params.add(numericQuery);
                countParams.add(numericQuery);
                countParams.add(numericQuery);
            } catch (NumberFormatException e) {
                where.append(" AND (item_name LIKE ? OR public_name LIKE ?)");
                String like = "%" + query + "%";
                params.add(like);
                params.add(like);
                countParams.add(like);
                countParams.add(like);
            }
        }

        if (!type.isEmpty()) {
            where.append(" AND type = ?");
            params.add(type);
            countParams.add(type);
        }

        try (Connection connection = Emulator.getDatabase().getDataSource().getConnection()) {
            int total = 0;
            try (PreparedStatement stmt = connection.prepareStatement("SELECT COUNT(*) as cnt FROM items_base" + where)) {
                setParams(stmt, countParams);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) total = rs.getInt("cnt");
                }
            }

            List<Map<String, Object>> items = new ArrayList<>();
            try (PreparedStatement stmt = connection.prepareStatement("SELECT * FROM items_base" + where + " ORDER BY id DESC LIMIT ? OFFSET ?")) {
                List<Object> allParams = new ArrayList<>(params);
                allParams.add(limit);
                allParams.add(offset);
                setParams(stmt, allParams);
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        items.add(FurniEditorHelper.readBasicItem(rs));
                    }
                }
            }

            this.client.sendResponse(new FurniEditorSearchComposer(items, total, page));
        }
    }

    private void setParams(PreparedStatement stmt, List<Object> params) throws Exception {
        for (int i = 0; i < params.size(); i++) {
            Object p = params.get(i);
            if (p instanceof Integer) stmt.setInt(i + 1, (Integer) p);
            else stmt.setString(i + 1, (String) p);
        }
    }
}
