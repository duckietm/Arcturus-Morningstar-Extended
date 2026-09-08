package com.eu.habbo.messages.outgoing.users;

import com.eu.habbo.messages.ServerMessage;
import com.eu.habbo.messages.outgoing.MessageComposer;
import com.eu.habbo.messages.outgoing.Outgoing;
import java.util.List;

/** CustomFilterResult (3883): the user's personal word filter list. */
public class CustomWordFilterWordsComposer extends MessageComposer {
    private final List<String> words;

    public CustomWordFilterWordsComposer(List<String> words) {
        this.words = words;
    }

    @Override
    protected ServerMessage composeInternal() {
        this.response.init(Outgoing.CustomWordFilterWordsComposer);
        this.response.appendInt(this.words.size());
        for (String word : this.words) {
            this.response.appendString(word);
        }
        return this.response;
    }
}
