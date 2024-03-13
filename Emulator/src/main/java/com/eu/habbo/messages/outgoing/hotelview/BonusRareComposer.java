package com.eu.habbo.messages.outgoing.hotelview;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.users.Habbo;
import com.eu.habbo.messages.ServerMessage;
import com.eu.habbo.messages.outgoing.MessageComposer;
import com.eu.habbo.messages.outgoing.Outgoing;

public class BonusRareComposer extends MessageComposer {
    private final Habbo habbo;

    public BonusRareComposer(Habbo habbo) {
        this.habbo = habbo;
    }

    @Override
    protected ServerMessage composeInternal() {
        this.response.init(Outgoing.BonusRareComposer);
        this.response.appendString(Emulator.getConfig().getValue("hotelview.promotional.reward.name", "prizetrophy_breed_gold")); //Furniture Name. Note: Image is in external_variables.txt
        this.response.appendInt(Emulator.getConfig().getInt("hotelview.promotional.reward.id", 0)); //Furniture ID
        this.response.appendInt(Emulator.getConfig().getInt("hotelview.promotional.points", 120)); //Total Required
        //this.response.appendInt(this.habbo.getHabboInfo().getBonusRarePoints() >= Emulator.getConfig().getInt("hotelview.promotinal.points", 120) ? Emulator.getConfig().getInt("hotelview.promotinal.points", 120) : this.habbo.getHabboInfo().getBonusRarePoints() ); //Total To Gain
        int points = Emulator.getConfig().getInt("hotelview.promotional.points", 120) - this.habbo.getHabboInfo().getBonusRarePoints();
        this.response.appendInt(points < 0 ? 0 : points);

        return this.response;
    }
}
