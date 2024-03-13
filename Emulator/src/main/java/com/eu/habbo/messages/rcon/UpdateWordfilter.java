package com.eu.habbo.messages.rcon;

import com.eu.habbo.Emulator;
import com.google.gson.Gson;

public class UpdateWordfilter extends RCONMessage<UpdateWordfilter.WordFilterJSON> {

    public UpdateWordfilter() {
        super(WordFilterJSON.class);
    }

    @Override
    public void handle(Gson gson, WordFilterJSON object) {
        Emulator.getGameEnvironment().getWordFilter().reload();
    }

    static class WordFilterJSON {
    }
}