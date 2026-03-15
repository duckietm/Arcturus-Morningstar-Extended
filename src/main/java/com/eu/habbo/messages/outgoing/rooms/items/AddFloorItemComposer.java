package com.eu.habbo.messages.outgoing.rooms.items;

import com.eu.habbo.habbohotel.items.interactions.*;
import com.eu.habbo.habbohotel.users.HabboItem;
import com.eu.habbo.messages.ServerMessage;
import com.eu.habbo.messages.outgoing.MessageComposer;
import com.eu.habbo.messages.outgoing.Outgoing;

public class AddFloorItemComposer extends MessageComposer {
    private final HabboItem item;
    private final String itemOwnerName;

    public AddFloorItemComposer(HabboItem item, String itemOwnerName) {
        this.item = item;
        this.itemOwnerName = itemOwnerName == null ? "" : itemOwnerName;
    }

    @Override
    protected ServerMessage composeInternal() {
        this.response.init(Outgoing.AddFloorItemComposer);
        this.item.serializeFloorData(this.response);
        this.response.appendInt(this.item instanceof InteractionGift ? ((((InteractionGift) this.item).getColorId() * 1000) + ((InteractionGift) this.item).getRibbonId()) : (this.item instanceof InteractionMusicDisc ? ((InteractionMusicDisc) this.item).getSongId() : 1));
        this.item.serializeExtradata(this.response);
        this.response.appendInt(-1);
        this.response.appendInt(this.item instanceof InteractionTeleport || this.item instanceof InteractionSwitch || this.item instanceof InteractionSwitchRemoteControl || this.item instanceof InteractionVendingMachine || this.item instanceof InteractionInformationTerminal || this.item instanceof InteractionPostIt|| this.item instanceof InteractionPuzzleBox ? 2 : this.item.isUsable() ? 1 : 0);
        this.response.appendInt(this.item.getUserId());
        this.response.appendString(this.itemOwnerName);
        return this.response;
    }

    public HabboItem getItem() {
        return item;
    }

    public String getItemOwnerName() {
        return itemOwnerName;
    }
}
