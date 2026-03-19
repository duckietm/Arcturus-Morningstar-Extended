package com.eu.habbo.messages.incoming.hotelview;

import com.eu.habbo.Emulator;
import com.eu.habbo.messages.incoming.MessageHandler;
import com.eu.habbo.messages.outgoing.hotelview.HotelViewSecondsUntilComposer;
import com.eu.habbo.util.HotelDateTimeUtil;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public class HotelViewRequestSecondsUntilEvent extends MessageHandler {
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    @Override
    public void handle() throws Exception {
        final String date = this.packet.readString();

        try {
            LocalDateTime dt = LocalDateTime.parse(date, formatter);
            int secondsUntil = Math.max(0, (int) HotelDateTimeUtil.toEpochSecond(dt) - Emulator.getIntUnixTimestamp());
            this.client.sendResponse(new HotelViewSecondsUntilComposer(date, secondsUntil));
        } catch (DateTimeParseException ignored) {
        }
    }
}
