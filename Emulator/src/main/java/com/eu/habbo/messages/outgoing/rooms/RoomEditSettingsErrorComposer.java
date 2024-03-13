package com.eu.habbo.messages.outgoing.rooms;

import com.eu.habbo.messages.ServerMessage;
import com.eu.habbo.messages.outgoing.MessageComposer;
import com.eu.habbo.messages.outgoing.Outgoing;

public class RoomEditSettingsErrorComposer extends MessageComposer {
    public final static int PASSWORD_REQUIRED = 5;
    public final static int ROOM_NAME_MISSING = 7;
    public final static int ROOM_NAME_BADWORDS = 8;
    public final static int ROOM_DESCRIPTION_BADWORDS = 10;
    public final static int ROOM_TAGS_BADWWORDS = 11;
    public final static int RESTRICTED_TAGS = 12;
    public final static int TAGS_TOO_LONG = 13;

    private final int roomId;
    private final int errorCode;
    private final String info;

    public RoomEditSettingsErrorComposer(int roomId, int errorCode, String info) {
        this.roomId = roomId;
        this.errorCode = errorCode;
        this.info = info;
    }

    @Override
    protected ServerMessage composeInternal() {
        this.response.init(Outgoing.RoomEditSettingsErrorComposer);
        this.response.appendInt(this.roomId);
        this.response.appendInt(this.errorCode);
        this.response.appendString(this.info);
        return this.response;
    }
}