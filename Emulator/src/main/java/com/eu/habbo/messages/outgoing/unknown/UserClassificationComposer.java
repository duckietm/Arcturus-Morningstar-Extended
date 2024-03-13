package com.eu.habbo.messages.outgoing.unknown;

import com.eu.habbo.messages.ServerMessage;
import com.eu.habbo.messages.outgoing.MessageComposer;
import com.eu.habbo.messages.outgoing.Outgoing;
import org.apache.commons.math3.util.Pair;

import java.util.List;

public class UserClassificationComposer extends MessageComposer {
    private final List<Pair<Integer, Pair<String, String>>> info;

    public UserClassificationComposer(List<Pair<Integer, Pair<String, String>>> info) {
        this.info = info;
    }

    @Override
    protected ServerMessage composeInternal() {
        this.response.init(Outgoing.UserClassificationComposer);
        this.response.appendInt(this.info.size());
        for (Pair<Integer, Pair<String, String>> set : this.info) {
            this.response.appendInt(set.getKey());
            this.response.appendString(set.getValue().getKey());
            this.response.appendString(set.getValue().getValue());
        }
        return this.response;
    }
}