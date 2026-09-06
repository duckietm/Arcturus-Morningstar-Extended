package com.eu.habbo.messages.incoming.furnieditor;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.items.editor.FurniEditorRepository;
import com.eu.habbo.habbohotel.permissions.Permission;
import com.eu.habbo.messages.incoming.MessageHandler;
import com.eu.habbo.messages.outgoing.furnieditor.FurniEditorResultComposer;
import com.eu.habbo.messages.outgoing.furnieditor.FurniEditorSearchComposer;
import java.util.List;

public class FurniEditorSearchEvent extends MessageHandler {

    // CATALOG_BULK_OFFERS_V2
    private static final int PAGE_SIZE = 200;

    @Override
    public void handle() throws Exception {
        if (!this.client.getHabbo().hasPermission(Permission.ACC_CATALOGFURNI)
                && !this.client.getHabbo().hasPermission("acc_wheeladmin")) {
            this.client.sendResponse(new FurniEditorResultComposer(false, "No permission"));
            return;
        }

        String query = this.packet.readString();
        String type = this.packet.readString();
        int page = this.packet.readInt();
        String sortField = this.packet.readString();
        String sortDir = this.packet.readString();

        // Input validation
        if (query.length() > 100) {
            query = query.substring(0, 100);
        }

        if (page < 1) page = 1;

        int offset = (page - 1) * PAGE_SIZE;
        List<String> furnidataClassnames = query.isEmpty()
                ? List.of()
                : Emulator.getGameEnvironment().getFurnitureTextProvider().findClassnamesByName(query);
        // "rare" is a pseudo type: only furni whose sprite has a rare value (wheel / crackable prize pickers).
        List<Integer> spriteIds = List.of();
        if ("rare".equals(type)) {
            spriteIds = new java.util.ArrayList<>(Emulator.getGameEnvironment().getCatalogManager().furnitureValues.keySet());
            if (spriteIds.isEmpty()) spriteIds = List.of(-1);
            type = "";
        }
        var result = new FurniEditorRepository(Emulator.getDatabase().getDataSource())
                .search(new FurniEditorRepository.SearchRequest(
                        query,
                        type == null ? "" : type,
                        sortField == null ? "" : sortField,
                        sortDir == null ? "" : sortDir,
                        PAGE_SIZE,
                        offset,
                        furnidataClassnames,
                        spriteIds));
        this.client.sendResponse(new FurniEditorSearchComposer(result.items(), result.total(), page));
    }
}
