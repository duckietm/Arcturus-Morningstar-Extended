package com.eu.habbo.messages.incoming.users;

import com.eu.habbo.habbohotel.users.UserWordFilter;
import com.eu.habbo.messages.incoming.MessageHandler;
import com.eu.habbo.messages.outgoing.users.CustomWordFilterModifyResultComposer;

/** AddCustomFilterWord (68). */
public class AddCustomWordFilterWordEvent extends MessageHandler {
    @Override
    public void handle() throws Exception {
        String word = this.packet.readString();
        word = UserWordFilter.normalize(word);
        if (word == null) {
            this.client.sendResponse(
                    new CustomWordFilterModifyResultComposer(CustomWordFilterModifyResultComposer.FAILED, ""));
            return;
        }

        boolean added = this.client.getHabbo().getHabboStats().addCustomFilterWord(word);
        this.client.sendResponse(new CustomWordFilterModifyResultComposer(
                added ? CustomWordFilterModifyResultComposer.ADDED : CustomWordFilterModifyResultComposer.FAILED,
                word));
    }
}
