package com.eu.habbo.messages.incoming.furnieditor;

import java.util.List;
import com.eu.habbo.messages.outgoing.furniture.FurnitureDataReloadComposer;
import com.eu.habbo.habbohotel.users.Habbo;
import com.eu.habbo.habbohotel.items.FurnitureType;
import com.eu.habbo.habbohotel.items.FurnitureTextProvider;
import com.eu.habbo.habbohotel.items.FurnidataTypeMover;
import com.eu.habbo.habbohotel.items.FurnidataLock;
import com.eu.habbo.habbohotel.items.FurnidataEntry;
import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.items.editor.FurniEditorLiveRefresh;
import com.eu.habbo.habbohotel.items.editor.FurniEditorRepository;
import com.eu.habbo.habbohotel.permissions.Permission;
import com.eu.habbo.messages.incoming.MessageHandler;
import com.eu.habbo.messages.outgoing.furnieditor.FurniEditorResultComposer;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class FurniEditorUpdateEvent extends MessageHandler {

    @Override
    public void handle() throws Exception {
        if (!this.client.getHabbo().hasPermission(Permission.ACC_CATALOGFURNI)) {
            this.client.sendResponse(new FurniEditorResultComposer(false, "No permission"));
            return;
        }

        int id = this.packet.readInt();
        String jsonFieldsStr = this.packet.readString();

        if (id <= 0) {
            this.client.sendResponse(new FurniEditorResultComposer(false, "Invalid item ID"));
            return;
        }

        JsonObject json;
        try {
            json = JsonParser.parseString(jsonFieldsStr).getAsJsonObject();
        } catch (Exception e) {
            this.client.sendResponse(new FurniEditorResultComposer(false, "Invalid JSON data"));
            return;
        }

        FurniEditorUpdatePayload payload = FurniEditorUpdatePayload.validate(json);
        if (!payload.valid()) {
            this.client.sendResponse(new FurniEditorResultComposer(false, payload.error));
            return;
        }

        String requestedType = json.has("type") && json.get("type").isJsonPrimitive()
                ? json.get("type").getAsString()
                : null;

        if (!new FurniEditorRepository(Emulator.getDatabase().getDataSource())
                .updateItem(id, payload.setClauses, payload.values)) {
            this.client.sendResponse(new FurniEditorResultComposer(false, "Item not found: " + id));
            return;
        }

        // The tiles the current footprint covers, before the definition changes under the live furni.
        java.util.Map<Integer, java.util.Set<com.eu.habbo.habbohotel.rooms.RoomTile>> previousFootprints =
                FurniEditorLiveRefresh.snapshotFootprints(id);

        // Reload emulator item definitions
        Emulator.getGameEnvironment().getItemManager().loadItems();

        // Apply the change to the furni that are already alive in loaded rooms and online inventories
        // (interaction class, special-type registry, tiles old and new, sprite) — no room reload needed.
        FurniEditorLiveRefresh.Result refresh = FurniEditorLiveRefresh.apply(id, previousFootprints);

        if (requestedType != null) {
            syncFurnidataPlacementType(id, requestedType);
        }

        this.client.sendResponse(new FurniEditorResultComposer(true, "Item updated" + refresh.describe(), id));
    }

    // FURNI_EDITOR_TYPE_SYNC_V1
    private void syncFurnidataPlacementType(int itemId, String requestedType) throws Exception {
        if (!"s".equals(requestedType) && !"i".equals(requestedType)) return;

        FurnitureTextProvider provider =
                Emulator.getGameEnvironment().getFurnitureTextProvider();

        if (provider == null || provider.getSource() == null) return;

        String classname = new FurniEditorRepository(Emulator.getDatabase().getDataSource())
                .findClassname(itemId)
                .orElse(null);

        if (classname == null || classname.isBlank()) return;

        FurnitureType target =
                "i".equals(requestedType) ? FurnitureType.WALL : FurnitureType.FLOOR;

        // The current hotel uses a single FurnitureData.json source. If a future
        // deployment switches to split-tier furnidata, do not guess which tier to mutate.
        if (provider.isSourceDirectory()) return;

        List<FurnidataEntry> delta = List.of();
        boolean moved;

        FurnidataLock.LOCK.lock();
        try {
            moved = FurnidataTypeMover.move(
                    provider.getSource(),
                    provider.getMaxBytes(),
                    classname,
                    target);

            if (moved) delta = provider.reindexFromSource();
        } finally {
            FurnidataLock.LOCK.unlock();
        }

        if (!moved || delta.isEmpty()) return;

        int deltaCap = Integer.parseInt(
                Emulator.getConfig().getValue("items.furnidata.delta.cap", "500"));

        FurnitureDataReloadComposer composer =
                delta.size() > deltaCap
                        ? new FurnitureDataReloadComposer(
                                FurnitureDataReloadComposer.MODE_RELOAD_HINT,
                                List.of())
                        : new FurnitureDataReloadComposer(
                                FurnitureDataReloadComposer.MODE_DELTA,
                                delta);

        for (Habbo habbo : Emulator.getGameEnvironment()
                .getHabboManager()
                .getOnlineHabbos()
                .values()) {
            if (habbo.getClient() != null) {
                habbo.getClient().sendResponse(composer);
            }
        }
    }

}
