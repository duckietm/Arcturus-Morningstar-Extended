package com.eu.habbo.messages.outgoing.rooms;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.guilds.Guild;
import com.eu.habbo.habbohotel.rooms.Room;
import com.eu.habbo.habbohotel.rooms.RoomPromotion;
import com.eu.habbo.habbohotel.users.Habbo;
import com.eu.habbo.messages.ServerMessage;
import com.eu.habbo.messages.outgoing.MessageComposer;
import com.eu.habbo.messages.outgoing.Outgoing;
import com.eu.habbo.util.HotelDateTimeUtil;

public class RoomDataComposer extends MessageComposer {
    private final Room room;
    private final Habbo habbo;
    private final boolean roomForward;
    private final boolean enterRoom;

    public RoomDataComposer(Room room, Habbo habbo, boolean roomForward, boolean enterRoom) {
        this.room = room;
        this.habbo = habbo;
        this.roomForward = roomForward;
        this.enterRoom = enterRoom;
    }

    @Override
    protected ServerMessage composeInternal() {
        final RoomPromotion promotion = this.room.getPromotion();
        final boolean hasPromotion = this.room.isPromoted() && (promotion != null);

        this.response.init(Outgoing.RoomDataComposer);
        this.response.appendBoolean(this.enterRoom);
        this.response.appendInt(this.room.getId());
        this.response.appendString(this.room.getName());
        if (this.room.isPublicRoom()) {
            this.response.appendInt(0);
            this.response.appendString("");
        } else {
            this.response.appendInt(this.room.getOwnerId());
            this.response.appendString(this.room.getOwnerName());
        }
        this.response.appendInt(this.room.getState().getState());
        this.response.appendInt(this.room.getUserCount());
        this.response.appendInt(this.room.getUsersMax());
        this.response.appendString(this.room.getDescription());
        this.response.appendInt(this.room.getTradeMode());
        this.response.appendInt(this.room.getScore());
        this.response.appendInt(2);//Top rated room rank
        this.response.appendInt(this.room.getCategory());

        if (!this.room.getTags().isEmpty()) {
            String[] tags = this.room.getTags().split(";");
            this.response.appendInt(tags.length);
            for (String s : tags) {
                this.response.appendString(s);
            }
        } else {
            this.response.appendInt(0);
        }

        int base = 0;

        if (this.room.getGuildId() > 0) {
            base = base | 2;
        }

        if (!this.room.isPublicRoom()) {
            base = base | 8;
        }

        if (hasPromotion) {
            base = base | 4;
        }

        if (this.room.isAllowPets()) {
            base = base | 16;
        }

        this.response.appendInt(base);

        if (this.room.getGuildId() > 0) {
            Guild g = Emulator.getGameEnvironment().getGuildManager().getGuild(this.room.getGuildId());
            if (g != null) {
                this.response.appendInt(g.getId());
                this.response.appendString(g.getName());
                this.response.appendString(g.getBadge());
            } else {
                this.response.appendInt(0);
                this.response.appendString("");
                this.response.appendString("");
            }
        }

        if (hasPromotion) {
            this.response.appendString(promotion.getTitle());
            this.response.appendString(promotion.getDescription());
            this.response.appendInt((promotion.getEndTimestamp() - Emulator.getIntUnixTimestamp()) / 60);
        }

        this.response.appendBoolean(this.roomForward);
        this.response.appendBoolean(this.room.isStaffPromotedRoom()); // staffpicked
        this.response.appendBoolean(this.room.hasGuild() && Emulator.getGameEnvironment().getGuildManager().getGuildMember(this.room.getGuildId(), this.habbo.getHabboInfo().getId()) != null); // is group member
        this.response.appendBoolean(this.room.isMuted()); // isroommuted

        this.response.appendInt(this.room.getMuteOption());
        this.response.appendInt(this.room.getKickOption());
        this.response.appendInt(this.room.getBanOption());

        this.response.appendBoolean(this.room.hasRights(this.habbo)); //mute all button

        this.response.appendInt(this.room.getChatMode());
        this.response.appendInt(this.room.getChatWeight());
        this.response.appendInt(this.room.getChatSpeed());
        this.response.appendInt(this.room.getChatDistance());
        this.response.appendInt(this.room.getChatProtection());
        this.response.appendString(HotelDateTimeUtil.getTimezoneId());
        this.response.appendString(String.valueOf(HotelDateTimeUtil.now().toInstant().toEpochMilli()));
        this.response.appendInt(Room.MAXIMUM_FURNI);


        return this.response;
    }

    public Room getRoom() {
        return room;
    }

    public Habbo getHabbo() {
        return habbo;
    }

    public boolean isRoomForward() {
        return roomForward;
    }

    public boolean isEnterRoom() {
        return enterRoom;
    }
}
