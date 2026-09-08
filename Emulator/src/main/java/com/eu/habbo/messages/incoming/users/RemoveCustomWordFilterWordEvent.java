package com.eu.habbo.messages.incoming.users;

import com.eu.habbo.habbohotel.users.UserWordFilter;
import com.eu.habbo.messages.incoming.MessageHandler;
import com.eu.habbo.messages.outgoing.users.CustomWordFilterModifyResultComposer;

/** RemoveCustomFilterWord (1996). */
public class RemoveCustomWordFilterWordEvent extends MessageHandler {
    @Override
    public void handle() throws Exception {
        String word = this.packet.readString();
        word = UserWordFilter.normalize(word);
        if (word == null) {
            this.client.sendResponse(
                    new CustomWordFilterModifyResultComposer(CustomWordFilterModifyResultComposer.FAILED, ""));
            return;
        }

        boolean removed = this.client.getHabbo().getHabboStats().removeCustomFilterWord(word);
        this.client.sendResponse(new CustomWordFilterModifyResultComposer(
                removed ? CustomWordFilterModifyResultComposer.REMOVED : CustomWordFilterModifyResultComposer.FAILED,
                word));
    }
}
