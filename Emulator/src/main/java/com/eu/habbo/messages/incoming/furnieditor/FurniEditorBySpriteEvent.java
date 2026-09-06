package com.eu.habbo.messages.incoming.furnieditor;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.items.editor.FurniEditorRepository;
import com.eu.habbo.habbohotel.permissions.Permission;
import com.eu.habbo.messages.incoming.MessageHandler;
import com.eu.habbo.messages.outgoing.furnieditor.FurniEditorResultComposer;

public class FurniEditorBySpriteEvent extends MessageHandler {

    @Override
    public void handle() throws Exception {
        if (!this.client.getHabbo().hasPermission(Permission.ACC_CATALOGFURNI)) {
            this.client.sendResponse(new FurniEditorResultComposer(false, "No permission"));
            return;
        }

        int spriteId = this.packet.readInt();

        if (spriteId <= 0) {
            this.client.sendResponse(new FurniEditorResultComposer(false, "Invalid sprite ID"));
            return;
        }

        FurniEditorRepository repository = new FurniEditorRepository(Emulator.getDatabase().getDataSource());

        // RoomObjectVariable.FURNITURE_TYPE_ID is the items_base ID, not a
        // sprite ID. The inspector historically labelled it as a sprite and
        // performed only the latter lookup. That leaves custom/large-ID furni
        // such as clothing_nftshoulderdragon1 stuck with no detail response.
        // Prefer the stable base-item ID, while retaining sprite lookup for
        // the search/editor paths which really pass a sprite_id.
        int itemId = repository.findDetail(spriteId).item() != null
                ? spriteId
                : repository.findItemIdBySprite(spriteId).orElse(-1);

        if (itemId <= 0) {
            this.client.sendResponse(new FurniEditorResultComposer(false, "No item found with sprite_id: " + spriteId));
            return;
        }

        // Delegate to the detail response builder
        FurniEditorDetailEvent.sendDetailResponse(this.client, itemId);
    }
}
