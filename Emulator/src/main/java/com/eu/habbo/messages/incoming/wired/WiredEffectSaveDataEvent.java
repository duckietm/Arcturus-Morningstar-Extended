package com.eu.habbo.messages.incoming.wired;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.items.interactions.InteractionWired;
import com.eu.habbo.habbohotel.items.interactions.InteractionWiredEffect;
import com.eu.habbo.habbohotel.items.interactions.wired.WiredSettings;
import com.eu.habbo.habbohotel.permissions.Permission;
import com.eu.habbo.habbohotel.rooms.Room;
import com.eu.habbo.messages.incoming.MessageHandler;
import com.eu.habbo.messages.outgoing.generic.alerts.UpdateFailedComposer;
import com.eu.habbo.messages.outgoing.wired.WiredSavedComposer;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Optional;

public class WiredEffectSaveDataEvent extends MessageHandler {
    @Override
    public void handle() throws Exception {
        int itemId = this.packet.readInt();

        Room room = this.client.getHabbo().getHabboInfo().getCurrentRoom();

        if (room != null) {
            if (room.hasRights(this.client.getHabbo()) || room.getOwnerId() == this.client.getHabbo().getHabboInfo().getId() || this.client.getHabbo().hasPermission(Permission.ACC_ANYROOMOWNER) || this.client.getHabbo().hasPermission(Permission.ACC_MOVEROTATE)) {
                InteractionWiredEffect effect = room.getRoomSpecialTypes().getEffect(itemId);

                try {
                    if (effect == null)
                        throw new WiredSaveException(String.format("Wired effect with item id %s not found in room", itemId));

                    Optional<Method> saveMethod = Arrays.stream(effect.getClass().getMethods()).filter(x -> x.getName().equals("saveData")).findFirst();

                    if(saveMethod.isPresent()) {
                        if(saveMethod.get().getParameterTypes()[0] == WiredSettings.class) {
                            WiredSettings settings = InteractionWired.readSettings(this.packet, true);
                            if (effect.saveData(settings, this.client)) {
                                this.client.sendResponse(new WiredSavedComposer());
                                effect.needsUpdate(true);
                                Emulator.getThreading().run(effect);
                            }
                        }
                        else {
                            if ((boolean) saveMethod.get().invoke(effect, this.packet, this.client)) {
                                this.client.sendResponse(new WiredSavedComposer());
                                effect.needsUpdate(true);
                                Emulator.getThreading().run(effect);
                            }
                        }
                    } else {
                        this.client.sendResponse(new UpdateFailedComposer("Save method was not found"));
                    }


                }
                catch (WiredSaveException e) {
                    this.client.sendResponse(new UpdateFailedComposer(e.getMessage()));
                }
            }
        }
    }
}
