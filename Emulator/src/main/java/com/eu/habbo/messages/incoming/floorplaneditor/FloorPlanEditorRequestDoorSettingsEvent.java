package com.eu.habbo.messages.incoming.floorplaneditor;

import com.eu.habbo.messages.incoming.MessageHandler;
import com.eu.habbo.messages.outgoing.floorplaneditor.FloorPlanEditorDoorSettingsComposer;
import com.eu.habbo.messages.outgoing.rooms.RoomFloorThicknessUpdatedComposer;

public class FloorPlanEditorRequestDoorSettingsEvent extends MessageHandler {
    @Override
    public void handle() throws Exception {
        if (this.client.getHabbo().getHabboInfo().getCurrentRoom() == null)
            return;

        this.client.sendResponse(new FloorPlanEditorDoorSettingsComposer(this.client.getHabbo().getHabboInfo().getCurrentRoom()));
        this.client.sendResponse(new RoomFloorThicknessUpdatedComposer(this.client.getHabbo().getHabboInfo().getCurrentRoom()));
    }
}
