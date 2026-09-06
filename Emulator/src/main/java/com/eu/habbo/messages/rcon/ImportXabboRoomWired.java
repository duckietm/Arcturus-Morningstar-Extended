package com.eu.habbo.messages.rcon;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.items.interactions.InteractionWired;
import com.eu.habbo.habbohotel.items.interactions.InteractionWiredCondition;
import com.eu.habbo.habbohotel.items.interactions.InteractionWiredEffect;
import com.eu.habbo.habbohotel.items.interactions.InteractionWiredExtra;
import com.eu.habbo.habbohotel.items.interactions.InteractionWiredTrigger;
import com.eu.habbo.habbohotel.items.interactions.wired.WiredSettings;
import com.eu.habbo.habbohotel.rooms.Room;
import com.eu.habbo.habbohotel.wired.core.WiredManager;
import com.google.gson.Gson;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.util.Locale;

/**
 * Applies one WIRED record exported in Xabbo BssRoomCloner.RoomSnapshot format.
 *
 * <p>The CMS creates the new, unloaded room and remaps Xabbo source furniture IDs
 * to the newly inserted Polaris IDs. This command deliberately uses the normal
 * WIRED saveData methods so every interaction remains responsible for its own
 * current database serialization.</p>
 */
public final class ImportXabboRoomWired extends RCONMessage<ImportXabboRoomWired.JSON> {
    public ImportXabboRoomWired() {
        super(JSON.class);
    }

    @Override
    public void handle(Gson gson, JSON json) {
        Room room = null;
        try {
            room = Emulator.getGameEnvironment().getRoomManager().loadRoom(json.room_id, false);
            if (room == null) {
                this.status = ROOM_NOT_FOUND;
                this.message = "imported room not found";
                return;
            }

            int[] intParams = json.int_params == null ? new int[0] : json.int_params;
            int[] selectedIds = json.selected_ids == null ? new int[0] : json.selected_ids;
            WiredSettings settings = new WiredSettings(
                    intParams,
                    json.string_param == null ? "" : json.string_param,
                    selectedIds,
                    json.selection_code,
                    json.delay);

            InteractionWired wired;
            boolean saved;
            switch (json.kind.trim().toLowerCase(Locale.ROOT)) {
                case "trigger" -> {
                    InteractionWiredTrigger trigger = room.getRoomSpecialTypes().getTrigger(json.item_id);
                    if (trigger == null) {
                        throw new IllegalArgumentException("WIRED trigger not found in imported room");
                    }
                    wired = trigger;
                    saved = trigger.saveData(settings, null);
                }
                case "condition" -> {
                    InteractionWiredCondition condition = room.getRoomSpecialTypes().getCondition(json.item_id);
                    if (condition == null) {
                        throw new IllegalArgumentException("WIRED condition not found in imported room");
                    }
                    wired = condition;
                    saved = condition.saveData(settings);
                }
                case "action" -> {
                    InteractionWiredEffect effect = room.getRoomSpecialTypes().getEffect(json.item_id);
                    InteractionWiredExtra extra = room.getRoomSpecialTypes().getExtra(json.item_id);
                    if (effect != null) {
                        wired = effect;
                        saved = effect.saveData(settings, null);
                    } else if (extra != null) {
                        wired = extra;
                        saved = extra.saveData(settings, null);
                    } else {
                        throw new IllegalArgumentException("WIRED action/extra not found in imported room");
                    }
                }
                default -> throw new IllegalArgumentException("invalid Xabbo WIRED kind");
            }

            if (!saved) {
                this.status = STATUS_ERROR;
                this.message = "WIRED rejected the imported settings";
                return;
            }

            wired.needsUpdate(true);
            wired.run();
            WiredManager.invalidateRoom(room);
            this.message = "imported Xabbo WIRED item " + json.item_id;
        } catch (Exception exception) {
            this.status = STATUS_ERROR;
            this.message = exception.getMessage() == null ? "failed to import Xabbo WIRED" : exception.getMessage();
        } finally {
            if (json.finalize && room != null) {
                Emulator.getGameEnvironment().getRoomManager().unloadRoom(room);
            }
        }
    }

    public static final class JSON {
        @Positive(message = "invalid room")
        public int room_id;

        @Positive(message = "invalid WIRED item")
        public int item_id;

        @NotBlank(message = "invalid WIRED kind")
        public String kind;

        @Size(max = 256, message = "too many WIRED integer parameters")
        public int[] int_params;

        @Size(max = 4096, message = "WIRED string parameter is too long")
        public String string_param;

        public int delay;
        public int selection_code;

        @Size(max = 256, message = "too many WIRED selected items")
        public int[] selected_ids;

        public boolean finalize;
    }
}
