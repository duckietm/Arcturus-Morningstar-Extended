package com.eu.habbo.messages.incoming.wired;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.items.interactions.InteractionWired;
import com.eu.habbo.habbohotel.items.interactions.InteractionWiredEffect;
import com.eu.habbo.habbohotel.items.interactions.InteractionWiredExtra;
import com.eu.habbo.habbohotel.items.interactions.wired.WiredSettings;
import com.eu.habbo.habbohotel.permissions.Permission;
import com.eu.habbo.habbohotel.rooms.Room;
import com.eu.habbo.habbohotel.wired.core.WiredManager;
import com.eu.habbo.messages.incoming.MessageHandler;
import com.eu.habbo.messages.outgoing.generic.alerts.UpdateFailedComposer;
import com.eu.habbo.messages.outgoing.wired.WiredSavedComposer;

public class WiredEffectSaveDataEvent extends MessageHandler {
    @Override
    public void handle() throws Exception {
        int itemId = this.packet.readInt();

        Room room = this.client.getHabbo().getHabboInfo().getCurrentRoom();

        if (room != null) {
            if (room.hasRights(this.client.getHabbo()) || room.getOwnerId() == this.client.getHabbo().getHabboInfo().getId() || this.client.getHabbo().hasPermission(Permission.ACC_ANYROOMOWNER) || this.client.getHabbo().hasPermission(Permission.ACC_MOVEROTATE)) {
                InteractionWiredEffect effect = room.getRoomSpecialTypes().getEffect(itemId);
                InteractionWiredExtra extra = room.getRoomSpecialTypes().getExtra(itemId);

                try {
                    if (effect == null && extra == null)
                        throw new WiredSaveException(String.format("Wired effect/extra with item id %s not found in room", itemId));

                    WiredSettings settings = InteractionWired.readSettings(this.packet, true);
                    boolean saved;

                    if (effect != null) {
                        saved = effect.saveData(settings, this.client);
                    } else {
                        saved = extra.saveData(settings, this.client);
                    }

                    if (saved) {
                        this.client.sendResponse(new WiredSavedComposer());
                        if (effect != null) {
                            effect.needsUpdate(true);
                            Emulator.getThreading().run(effect);
                        } else {
                            extra.needsUpdate(true);
                            Emulator.getThreading().run(extra);
                        }

                        // Invalidate wired cache when effect is saved
                        WiredManager.invalidateRoom(room);
                    } else {
                        this.client.sendResponse(new UpdateFailedComposer("There was an error while saving that effect"));
                    }
                }
                catch (WiredSaveException e) {
                    this.client.sendResponse(new UpdateFailedComposer(e.getMessage()));
                }
            }
        }
    }
}
