package com.eu.habbo.messages.incoming.furnieditor;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.gameclients.GameClient;
import com.eu.habbo.habbohotel.items.editor.FurniEditorRepository;
import com.eu.habbo.habbohotel.permissions.Permission;
import com.eu.habbo.messages.incoming.MessageHandler;
import com.eu.habbo.messages.outgoing.furnieditor.FurniEditorCrackableComposer;
import com.eu.habbo.messages.outgoing.furnieditor.FurniEditorResultComposer;

/** Furni editor: read the items_crackable configuration of a base item (crack count, prizes with weights ...). */
public class FurniEditorCrackableEvent extends MessageHandler {

    @Override
    public void handle() throws Exception {
        if (!this.client.getHabbo().hasPermission(Permission.ACC_CATALOGFURNI)) {
            this.client.sendResponse(new FurniEditorResultComposer(false, "No permission"));
            return;
        }

        int id = this.packet.readInt();

        if (id <= 0) {
            this.client.sendResponse(new FurniEditorResultComposer(false, "Invalid item ID"));
            return;
        }

        sendCrackable(this.client, id);
    }

    public static void sendCrackable(GameClient client, int itemId) throws Exception {
        FurniEditorRepository.Crackable crackable =
                new FurniEditorRepository(Emulator.getDatabase().getDataSource()).findCrackable(itemId);

        client.sendResponse(new FurniEditorCrackableComposer(itemId, crackable));
    }
}
