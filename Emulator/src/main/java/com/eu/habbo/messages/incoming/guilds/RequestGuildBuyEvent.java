package com.eu.habbo.messages.incoming.guilds;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.guilds.Guild;
import com.eu.habbo.habbohotel.modtool.ScripterManager;
import com.eu.habbo.habbohotel.permissions.Permission;
import com.eu.habbo.habbohotel.rooms.Room;
import com.eu.habbo.habbohotel.users.Habbo;
import com.eu.habbo.messages.incoming.MessageHandler;
import com.eu.habbo.messages.outgoing.catalog.AlertPurchaseFailedComposer;
import com.eu.habbo.messages.outgoing.catalog.PurchaseOKComposer;
import com.eu.habbo.messages.outgoing.guilds.GuildBoughtComposer;
import com.eu.habbo.messages.outgoing.guilds.GuildEditFailComposer;
import com.eu.habbo.messages.outgoing.guilds.GuildInfoComposer;
import com.eu.habbo.plugin.events.guilds.GuildPurchasedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RequestGuildBuyEvent extends MessageHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(RequestGuildBuyEvent.class);

    @Override
    public void handle() throws Exception {
        String name = this.packet.readString();
        String description = this.packet.readString();

        if(name.length() > 29 || description.length() > 254)
            return;

        if (Emulator.getConfig().getBoolean("catalog.guild.hc_required", true) && !this.client.getHabbo().getHabboStats().hasActiveClub()) {
            this.client.sendResponse(new GuildEditFailComposer(GuildEditFailComposer.HC_REQUIRED));
            return;
        }

        if (!this.client.getHabbo().hasPermission(Permission.ACC_INFINITE_CREDITS)) {
            int guildPrice = Emulator.getConfig().getInt("catalog.guild.price");
            if (this.client.getHabbo().getHabboInfo().getCredits() >= guildPrice) {
                this.client.getHabbo().giveCredits(-guildPrice);
            } else {
                this.client.sendResponse(new AlertPurchaseFailedComposer(AlertPurchaseFailedComposer.SERVER_ERROR));
                return;
            }
        }

        int roomId = this.packet.readInt();

        Room r = Emulator.getGameEnvironment().getRoomManager().getRoom(roomId);

        if (r != null) {
            if (r.hasGuild()) {
                this.client.sendResponse(new GuildEditFailComposer(GuildEditFailComposer.ROOM_ALREADY_IN_USE));
                return;
            }

            if (r.getOwnerId() == this.client.getHabbo().getHabboInfo().getId()) {
                if (r.getGuildId() == 0) {
                    int colorOne = this.packet.readInt();
                    int colorTwo = this.packet.readInt();

                    int count = this.packet.readInt();

                    String badge = "";

                    byte base = 1;

                    while (base < count) {
                        int id = this.packet.readInt();
                        int color = this.packet.readInt();
                        int pos = this.packet.readInt();

                        if (base == 1) {
                            badge += "b";
                        } else {
                            badge += "s";
                        }

                        badge += (id < 100 ? "0" : "") + (id < 10 ? "0" : "") + id + (color < 10 ? "0" : "") + color + "" + pos;

                        base += 3;
                    }

                    if(name.length() > 29){
                        this.client.sendResponse(new GuildEditFailComposer(GuildEditFailComposer.INVALID_GUILD_NAME));
                        return;
                    }
                    if(description.length() > 254){
                        return;
                    }
                    Guild guild = Emulator.getGameEnvironment().getGuildManager().createGuild(this.client.getHabbo(), roomId, r.getName(), name, description, badge, colorOne, colorTwo);

                    r.setGuild(guild.getId());
                    r.removeAllRights();
                    r.setNeedsUpdate(true);

                    if (Emulator.getConfig().getBoolean("imager.internal.enabled")) {
                        Emulator.getBadgeImager().generate(guild);
                    }

                    this.client.sendResponse(new PurchaseOKComposer());
                    this.client.sendResponse(new GuildBoughtComposer(guild));
                    for (Habbo habbo : r.getHabbos()) {
                        habbo.getClient().sendResponse(new GuildInfoComposer(guild, habbo.getClient(), false, null));
                    }
                    r.refreshGuild(guild);

                    Emulator.getPluginManager().fireEvent(new GuildPurchasedEvent(guild, this.client.getHabbo()));

                    Emulator.getGameEnvironment().getGuildManager().addGuild(guild);
                }
            } else {
                String message = Emulator.getTexts().getValue("scripter.warning.guild.buy.owner").replace("%username%", this.client.getHabbo().getHabboInfo().getUsername()).replace("%roomname%", r.getName().replace("%owner%", r.getOwnerName()));
                ScripterManager.scripterDetected(this.client, message);
                LOGGER.info(message);
            }
        }
    }
}
