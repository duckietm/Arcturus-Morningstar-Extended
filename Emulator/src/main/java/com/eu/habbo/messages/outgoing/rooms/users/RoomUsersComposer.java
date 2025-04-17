package com.eu.habbo.messages.outgoing.rooms.users;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.bots.Bot;
import com.eu.habbo.habbohotel.guilds.Guild;
import com.eu.habbo.habbohotel.users.Habbo;
import com.eu.habbo.messages.ServerMessage;
import com.eu.habbo.messages.outgoing.MessageComposer;
import com.eu.habbo.messages.outgoing.Outgoing;

import java.util.Collection;

public class RoomUsersComposer extends MessageComposer {
    private Habbo habbo;
    private Collection<Habbo> habbos;
    private Bot bot;
    private Collection<Bot> bots;

    public RoomUsersComposer(Habbo habbo) {
        this.habbo = habbo;
    }

    public RoomUsersComposer(Collection<Habbo> habbos) {
        this.habbos = habbos;
    }

    public RoomUsersComposer(Bot bot) {
        this.bot = bot;
    }

    public RoomUsersComposer(Collection<Bot> bots, boolean isBot) {
        this.bots = bots;
    }

    @Override
    protected ServerMessage composeInternal() {
        this.response.init(Outgoing.RoomUsersComposer);
        if (this.habbo != null) {
            this.response.appendInt(1);
            this.response.appendInt(this.habbo.getHabboInfo().getId());
            this.response.appendString(this.habbo.getHabboInfo().getUsername());
            this.response.appendString(this.habbo.getHabboInfo().getMotto());
            this.response.appendInt(this.habbo.getHabboInfo().getInfostandBg());
            this.response.appendInt(this.habbo.getHabboInfo().getInfostandStand());
            this.response.appendInt(this.habbo.getHabboInfo().getInfostandOverlay());
            this.response.appendString(this.habbo.getHabboInfo().getLook());
            this.response.appendInt(this.habbo.getRoomUnit().getId()); //Room Unit ID
            this.response.appendInt(this.habbo.getRoomUnit().getX());
            this.response.appendInt(this.habbo.getRoomUnit().getY());
            this.response.appendString(this.habbo.getRoomUnit().getZ() + "");
            this.response.appendInt(this.habbo.getRoomUnit().getBodyRotation().getValue());
            this.response.appendInt(1);
            this.response.appendString(this.habbo.getHabboInfo().getGender().name().toUpperCase());
            this.response.appendInt(this.habbo.getHabboStats().guild != 0 ? this.habbo.getHabboStats().guild : -1);
            this.response.appendInt(this.habbo.getHabboStats().guild != 0 ? 1 : -1);

            String name = "";
            if (this.habbo.getHabboStats().guild != 0) {
                Guild g = Emulator.getGameEnvironment().getGuildManager().getGuild(this.habbo.getHabboStats().guild);

                if (g != null)
                    name = g.getName();
            }
            this.response.appendString(name);

            this.response.appendString("");
            this.response.appendInt(this.habbo.getHabboStats().getAchievementScore());
            this.response.appendBoolean(true);
        } else if (this.habbos != null) {
            this.response.appendInt(this.habbos.size());
            for (Habbo habbo : this.habbos) {
                if (habbo != null) {
                    this.response.appendInt(habbo.getHabboInfo().getId());
                    this.response.appendString(habbo.getHabboInfo().getUsername());
                    this.response.appendString(habbo.getHabboInfo().getMotto());
                    this.response.appendInt(habbo.getHabboInfo().getInfostandBg());
                    this.response.appendInt(habbo.getHabboInfo().getInfostandStand());
                    this.response.appendInt(habbo.getHabboInfo().getInfostandOverlay());
                    this.response.appendString(habbo.getHabboInfo().getLook());
                    this.response.appendInt(habbo.getRoomUnit().getId()); //Room Unit ID
                    this.response.appendInt(habbo.getRoomUnit().getX());
                    this.response.appendInt(habbo.getRoomUnit().getY());
                    this.response.appendString(habbo.getRoomUnit().getZ() + "");
                    this.response.appendInt(habbo.getRoomUnit().getBodyRotation().getValue());
                    this.response.appendInt(1);
                    this.response.appendString(habbo.getHabboInfo().getGender().name().toUpperCase());
                    this.response.appendInt(habbo.getHabboStats().guild != 0 ? habbo.getHabboStats().guild : -1);
                    this.response.appendInt(habbo.getHabboStats().guild != 0 ? habbo.getHabboStats().guild : -1);
                    String name = "";
                    if (habbo.getHabboStats().guild != 0) {
                        Guild g = Emulator.getGameEnvironment().getGuildManager().getGuild(habbo.getHabboStats().guild);

                        if (g != null)
                            name = g.getName();
                    }
                    this.response.appendString(name);
                    this.response.appendString("");
                    this.response.appendInt(habbo.getHabboStats().getAchievementScore());
                    this.response.appendBoolean(true);
                }
            }
        } else if (this.bot != null) {
            this.response.appendInt(1);
            this.response.appendInt(0 - this.bot.getId());
            this.response.appendString(this.bot.getName());
            this.response.appendString(this.bot.getMotto());
            this.response.appendInt(0);
            this.response.appendInt(0);
            this.response.appendInt(0);
            this.response.appendString(this.bot.getFigure());
            this.response.appendInt(this.bot.getRoomUnit().getId());
            this.response.appendInt(this.bot.getRoomUnit().getX());
            this.response.appendInt(this.bot.getRoomUnit().getY());
            this.response.appendString(this.bot.getRoomUnit().getZ() + "");
            this.response.appendInt(this.bot.getRoomUnit().getBodyRotation().getValue());
            this.response.appendInt(4);
            this.response.appendString(this.bot.getGender().name().toUpperCase());
            this.response.appendInt(this.bot.getOwnerId());
            this.response.appendString(this.bot.getOwnerName());
            this.response.appendInt(10);
            this.response.appendShort(0);
            this.response.appendShort(1);
            this.response.appendShort(2);
            this.response.appendShort(3);
            this.response.appendShort(4);
            this.response.appendShort(5);
            this.response.appendShort(6);
            this.response.appendShort(7);
            this.response.appendShort(8);
            this.response.appendShort(9);
        } else if (this.bots != null) {
            this.response.appendInt(this.bots.size());
            for (Bot bot : this.bots) {
                this.response.appendInt(0 - bot.getId());
                this.response.appendString(bot.getName());
                this.response.appendString(bot.getMotto());
                this.response.appendInt(0);
                this.response.appendInt(0);
                this.response.appendInt(0);
                this.response.appendString(bot.getFigure());
                this.response.appendInt(bot.getRoomUnit().getId());
                this.response.appendInt(bot.getRoomUnit().getX());
                this.response.appendInt(bot.getRoomUnit().getY());
                this.response.appendString(bot.getRoomUnit().getZ() + "");
                this.response.appendInt(bot.getRoomUnit().getBodyRotation().getValue());
                this.response.appendInt(4);
                this.response.appendString(bot.getGender().name().toUpperCase());
                this.response.appendInt(bot.getOwnerId());
                this.response.appendString(bot.getOwnerName());
                this.response.appendInt(10);
                this.response.appendShort(0);
                this.response.appendShort(1);
                this.response.appendShort(2);
                this.response.appendShort(3);
                this.response.appendShort(4);
                this.response.appendShort(5);
                this.response.appendShort(6);
                this.response.appendShort(7);
                this.response.appendShort(8);
                this.response.appendShort(9);
            }
        }
        return this.response;
    }
}
