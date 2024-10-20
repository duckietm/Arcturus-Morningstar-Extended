package com.eu.habbo.messages.outgoing.modtool;

import com.eu.habbo.habbohotel.modtool.ModToolChatLog;
import com.eu.habbo.habbohotel.modtool.ModToolRoomVisit;
import com.eu.habbo.messages.ServerMessage;
import com.eu.habbo.messages.outgoing.MessageComposer;
import com.eu.habbo.messages.outgoing.Outgoing;

import java.text.SimpleDateFormat;
import java.util.ArrayList;

public class ModToolUserChatlogComposer extends MessageComposer {
    public static SimpleDateFormat format = new SimpleDateFormat("HH:mm");
    private final ArrayList<ModToolRoomVisit> set;
    private final int userId;
    private final String username;

    public ModToolUserChatlogComposer(ArrayList<ModToolRoomVisit> set, int userId, String username) {
        this.set = set;
        this.userId = userId;
        this.username = username;
    }

    @Override
    protected ServerMessage composeInternal() {
        this.response.init(Outgoing.ModToolUserChatlogComposer);
        this.response.appendInt(this.userId);
        this.response.appendString(this.username);
        this.response.appendInt(this.set.size());

        for (ModToolRoomVisit visit : this.set) {
            this.response.appendByte(1);
            this.response.appendShort(2);
            this.response.appendString("roomName");
            this.response.appendByte(2);
            this.response.appendString(visit.roomName);
            this.response.appendString("roomId");
            this.response.appendByte(1);
            this.response.appendInt(visit.roomId);

            this.response.appendShort(visit.chat.size());
            for (ModToolChatLog chatLog : visit.chat) {
                this.response.appendString(format.format(chatLog.timestamp * 1000L));
                this.response.appendInt(chatLog.habboId);
                this.response.appendString(chatLog.username);
                this.response.appendString(chatLog.message);
                this.response.appendBoolean(false);
            }
        }
        return this.response;
    }

    public ArrayList<ModToolRoomVisit> getSet() {
        return set;
    }

    public int getUserId() {
        return userId;
    }

    public String getUsername() {
        return username;
    }
}
