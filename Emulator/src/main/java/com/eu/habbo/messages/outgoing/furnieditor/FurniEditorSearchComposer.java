package com.eu.habbo.messages.outgoing.furnieditor;

import com.eu.habbo.messages.ServerMessage;
import com.eu.habbo.messages.outgoing.MessageComposer;
import com.eu.habbo.messages.outgoing.Outgoing;

import java.util.List;
import java.util.Map;

public class FurniEditorSearchComposer extends MessageComposer {
    private final List<Map<String, Object>> items;
    private final int total;
    private final int page;

    public FurniEditorSearchComposer(List<Map<String, Object>> items, int total, int page) {
        this.items = items;
        this.total = total;
        this.page = page;
    }

    @Override
    protected ServerMessage composeInternal() {
        this.response.init(Outgoing.FurniEditorSearchComposer);
        this.response.appendInt(this.items.size());

        for (Map<String, Object> item : this.items) {
            this.response.appendInt((int) item.get("id"));
            this.response.appendInt((int) item.get("sprite_id"));
            this.response.appendString((String) item.get("item_name"));
            this.response.appendString((String) item.get("public_name"));
            this.response.appendString((String) item.get("type"));
            this.response.appendInt((int) item.get("width"));
            this.response.appendInt((int) item.get("length"));
            this.response.appendDouble((double) item.get("stack_height"));
            this.response.appendBoolean((boolean) item.get("allow_stack"));
            this.response.appendBoolean((boolean) item.get("allow_walk"));
            this.response.appendBoolean((boolean) item.get("allow_sit"));
            this.response.appendBoolean((boolean) item.get("allow_lay"));
            this.response.appendString((String) item.get("interaction_type"));
            this.response.appendInt((int) item.get("interaction_modes_count"));
        }

        this.response.appendInt(this.total);
        this.response.appendInt(this.page);
        return this.response;
    }
}
