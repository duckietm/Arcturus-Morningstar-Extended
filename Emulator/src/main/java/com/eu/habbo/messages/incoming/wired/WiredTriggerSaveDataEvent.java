package com.eu.habbo.messages.incoming.wired;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.items.interactions.InteractionWired;
import com.eu.habbo.habbohotel.items.interactions.InteractionWiredTrigger;
import com.eu.habbo.habbohotel.items.interactions.wired.WiredSettings;
import com.eu.habbo.habbohotel.permissions.Permission;
import com.eu.habbo.habbohotel.rooms.Room;
import com.eu.habbo.messages.incoming.MessageHandler;
import com.eu.habbo.messages.outgoing.generic.alerts.UpdateFailedComposer;
import com.eu.habbo.messages.outgoing.wired.WiredSavedComposer;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Optional;

public class WiredTriggerSaveDataEvent extends MessageHandler {
    @Override
    public void handle() throws Exception {
        int itemId = this.packet.readInt();

        Room room = this.client.getHabbo().getHabboInfo().getCurrentRoom();

        if (room != null) {
            if (room.hasRights(this.client.getHabbo()) || room.getOwnerId() == this.client.getHabbo().getHabboInfo().getId() || this.client.getHabbo().hasPermission(Permission.ACC_ANYROOMOWNER) || this.client.getHabbo().hasPermission(Permission.ACC_MOVEROTATE)) {
                InteractionWiredTrigger trigger = room.getRoomSpecialTypes().getTrigger(itemId);

                if (trigger != null) {

                    Optional<Method> saveMethod = Arrays.stream(trigger.getClass().getMethods()).filter(x -> x.getName().equals("saveData")).findFirst();

                    if(saveMethod.isPresent()) {
                        if (saveMethod.get().getParameterTypes()[0] == WiredSettings.class) {
                            WiredSettings settings = InteractionWired.readSettings(this.packet, false);

                            if (trigger.saveData(settings)) {
                                this.client.sendResponse(new WiredSavedComposer());

                                trigger.needsUpdate(true);

                                Emulator.getThreading().run(trigger);
                            } else {
                                this.client.sendResponse(new UpdateFailedComposer("There was an error while saving that trigger"));
                            }
                        } else {
                            if ((boolean) saveMethod.get().invoke(trigger, this.packet)) {
                                this.client.sendResponse(new WiredSavedComposer());
                                trigger.needsUpdate(true);
                                Emulator.getThreading().run(trigger);
                            } else {
                                this.client.sendResponse(new UpdateFailedComposer("There was an error while saving that trigger"));
                            }
                        }
                    }
                    else {
                        this.client.sendResponse(new UpdateFailedComposer("Save method was not found"));
                    }
                }
            }
        }
    }
}
