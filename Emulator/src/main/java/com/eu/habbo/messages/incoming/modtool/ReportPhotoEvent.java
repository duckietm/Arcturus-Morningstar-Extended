package com.eu.habbo.messages.incoming.modtool;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.items.interactions.InteractionExternalImage;
import com.eu.habbo.habbohotel.modtool.CfhTopic;
import com.eu.habbo.habbohotel.modtool.ModToolIssue;
import com.eu.habbo.habbohotel.modtool.ModToolTicketType;
import com.eu.habbo.habbohotel.rooms.Room;
import com.eu.habbo.habbohotel.users.HabboInfo;
import com.eu.habbo.habbohotel.users.HabboItem;
import com.eu.habbo.messages.incoming.MessageHandler;
import com.eu.habbo.messages.outgoing.modtool.ModToolReportReceivedAlertComposer;
import com.eu.habbo.threading.runnables.InsertModToolIssue;
import com.google.gson.JsonParser;

public class ReportPhotoEvent extends MessageHandler {
    @Override
    public void handle() throws Exception {
        boolean hasExtradataId = this.packet.readShort() != 0;

        this.packet.getBuffer().resetReaderIndex();

        if (hasExtradataId) {
            String extradataId = this.packet.readString();
        }

        int roomId = this.packet.readInt();
        int reportedUserId = this.packet.readInt();
        int topicId = this.packet.readInt();
        int itemId = this.packet.readInt();

        CfhTopic topic = Emulator.getGameEnvironment().getModToolManager().getCfhTopic(topicId);

        if (topic == null) return;

        Room room = Emulator.getGameEnvironment().getRoomManager().getRoom(roomId);

        if (room == null) return;

        HabboItem item = room.getHabboItem(itemId);

        if (item == null || !(item instanceof InteractionExternalImage)) return;

        HabboInfo photoOwner = Emulator.getGameEnvironment().getHabboManager().getHabboInfo(item.getUserId());

        if (photoOwner == null) return;

        ModToolIssue issue = new ModToolIssue(this.client.getHabbo().getHabboInfo().getId(), this.client.getHabbo().getHabboInfo().getUsername(), photoOwner.getId(), photoOwner.getUsername(), roomId, "", ModToolTicketType.PHOTO);
        issue.photoItem = item;

        new InsertModToolIssue(issue).run();

        this.client.sendResponse(new ModToolReportReceivedAlertComposer(ModToolReportReceivedAlertComposer.REPORT_RECEIVED, ""));
        Emulator.getGameEnvironment().getModToolManager().addTicket(issue);
        Emulator.getGameEnvironment().getModToolManager().updateTicketToMods(issue);
    }
}
