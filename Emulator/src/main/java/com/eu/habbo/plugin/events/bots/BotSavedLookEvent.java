package com.eu.habbo.plugin.events.bots;

import com.eu.habbo.habbohotel.bots.Bot;
import com.eu.habbo.habbohotel.users.HabboGender;

public class BotSavedLookEvent extends BotEvent {

    public HabboGender gender;


    public String newLook;


    public int effect;


    public BotSavedLookEvent(Bot bot, HabboGender gender, String newLook, int effect) {
        super(bot);

        this.gender = gender;
        this.newLook = newLook;
        this.effect = effect;
    }
}
