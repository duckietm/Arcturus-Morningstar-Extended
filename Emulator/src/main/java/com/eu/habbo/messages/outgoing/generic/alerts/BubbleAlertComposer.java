package com.eu.habbo.messages.outgoing.generic.alerts;

import com.eu.habbo.messages.ServerMessage;
import com.eu.habbo.messages.outgoing.MessageComposer;
import com.eu.habbo.messages.outgoing.Outgoing;
import gnu.trove.map.hash.THashMap;

import java.util.Map;

public class BubbleAlertComposer extends MessageComposer {
    private final String errorKey;
    private final THashMap<String, String> keys;

    public BubbleAlertComposer(String errorKey, THashMap<String, String> keys) {
        this.errorKey = errorKey;
        this.keys = keys;
    }

    public BubbleAlertComposer(String errorKey, String message) {
        this.errorKey = errorKey;
        this.keys = new THashMap<>();
        this.keys.put("message", message);
    }

    public BubbleAlertComposer(String errorKey) {
        this.errorKey = errorKey;
        this.keys = new THashMap<>();
    }

    @Override
    protected ServerMessage composeInternal() {
        this.response.init(Outgoing.BubbleAlertComposer);
        this.response.appendString(this.errorKey);
        this.response.appendInt(this.keys.size());
        for (Map.Entry<String, String> set : this.keys.entrySet()) {
            this.response.appendString(set.getKey());
            this.response.appendString(set.getValue());
        }
        return this.response;
    }
}
