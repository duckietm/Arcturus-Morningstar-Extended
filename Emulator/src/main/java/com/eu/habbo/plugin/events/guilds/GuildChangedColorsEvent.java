package com.eu.habbo.plugin.events.guilds;

import com.eu.habbo.habbohotel.guilds.Guild;

public class GuildChangedColorsEvent extends GuildEvent {

    public int colorOne;


    public int colorTwo;


    public GuildChangedColorsEvent(Guild guild, int colorOne, int colorTwo) {
        super(guild);

        this.colorOne = colorOne;
        this.colorTwo = colorTwo;
    }
}
