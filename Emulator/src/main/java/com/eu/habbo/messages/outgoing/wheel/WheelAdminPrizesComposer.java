package com.eu.habbo.messages.outgoing.wheel;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.items.Item;
import com.eu.habbo.habbohotel.wheel.WheelPrize;
import com.eu.habbo.messages.ServerMessage;
import com.eu.habbo.messages.outgoing.MessageComposer;
import com.eu.habbo.messages.outgoing.Outgoing;

import java.util.List;

// Raw editable prize list for the in-client admin editor (sends value/amount/
// pointsType as stored, unlike WheelDataComposer which resolves icons for players).
public class WheelAdminPrizesComposer extends MessageComposer {
    private final List<WheelPrize> prizes;

    public WheelAdminPrizesComposer(List<WheelPrize> prizes) {
        this.prizes = prizes;
    }

    @Override
    protected ServerMessage composeInternal() {
        this.response.init(Outgoing.WheelAdminPrizesComposer);
        this.response.appendInt(this.prizes.size());
        for (WheelPrize prize : this.prizes) {
            this.response.appendInt(prize.id);
            this.response.appendString(prize.type);
            this.response.appendString(prize.value == null ? "" : prize.value);
            this.response.appendInt(prize.amount);
            this.response.appendInt(prize.pointsType);
            this.response.appendInt(prize.weight);
            this.response.appendString(prize.label == null ? "" : prize.label);
            this.response.appendInt(prize.spriteId);
            this.response.appendString(itemName(prize));
        }
        return this.response;
    }

    private static String itemName(WheelPrize prize) {
        if (!"item".equals(prize.type) || prize.value == null) return "";
        try {
            Item item = Emulator.getGameEnvironment().getItemManager().getItem(Integer.parseInt(prize.value.trim()));
            return item == null ? "" : (item.getFullName() == null || item.getFullName().isBlank() ? item.getName() : item.getFullName());
        } catch (NumberFormatException e) {
            return "";
        }
    }
}
