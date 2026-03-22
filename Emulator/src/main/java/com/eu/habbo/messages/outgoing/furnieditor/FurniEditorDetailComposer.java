package com.eu.habbo.messages.outgoing.furnieditor;

import com.eu.habbo.messages.ServerMessage;
import com.eu.habbo.messages.outgoing.MessageComposer;
import com.eu.habbo.messages.outgoing.Outgoing;

import java.util.List;
import java.util.Map;

public class FurniEditorDetailComposer extends MessageComposer {
    private final Map<String, Object> item;
    private final int usageCount;
    private final List<Map<String, Object>> catalogItems;
    private final String furniDataJson;

    public FurniEditorDetailComposer(Map<String, Object> item, int usageCount, List<Map<String, Object>> catalogItems, String furniDataJson) {
        this.item = item;
        this.usageCount = usageCount;
        this.catalogItems = catalogItems;
        this.furniDataJson = furniDataJson;
    }

    @Override
    protected ServerMessage composeInternal() {
        this.response.init(Outgoing.FurniEditorDetailComposer);

        this.response.appendInt((int) this.item.get("id"));
        this.response.appendInt((int) this.item.get("sprite_id"));
        this.response.appendString((String) this.item.get("item_name"));
        this.response.appendString((String) this.item.get("public_name"));
        this.response.appendString((String) this.item.get("type"));
        this.response.appendInt((int) this.item.get("width"));
        this.response.appendInt((int) this.item.get("length"));
        this.response.appendDouble((double) this.item.get("stack_height"));
        this.response.appendBoolean((boolean) this.item.get("allow_stack"));
        this.response.appendBoolean((boolean) this.item.get("allow_walk"));
        this.response.appendBoolean((boolean) this.item.get("allow_sit"));
        this.response.appendBoolean((boolean) this.item.get("allow_lay"));
        this.response.appendString((String) this.item.get("interaction_type"));
        this.response.appendInt((int) this.item.get("interaction_modes_count"));

        // Extended fields
        this.response.appendBoolean((boolean) this.item.get("allow_gift"));
        this.response.appendBoolean((boolean) this.item.get("allow_trade"));
        this.response.appendBoolean((boolean) this.item.get("allow_recycle"));
        this.response.appendBoolean((boolean) this.item.get("allow_marketplace_sell"));
        this.response.appendBoolean((boolean) this.item.get("allow_inventory_stack"));
        this.response.appendString((String) this.item.getOrDefault("vending_ids", ""));
        this.response.appendString((String) this.item.getOrDefault("customparams", ""));
        this.response.appendInt((int) this.item.getOrDefault("effect_id_male", 0));
        this.response.appendInt((int) this.item.getOrDefault("effect_id_female", 0));
        this.response.appendString((String) this.item.getOrDefault("clothing_on_walk", ""));
        this.response.appendString((String) this.item.getOrDefault("multiheight", ""));
        this.response.appendString((String) this.item.getOrDefault("description", ""));
        this.response.appendInt(this.usageCount);

        // Catalog items
        this.response.appendInt(this.catalogItems.size());
        for (Map<String, Object> cat : this.catalogItems) {
            this.response.appendInt((int) cat.get("id"));
            this.response.appendString((String) cat.get("catalog_name"));
            this.response.appendInt((int) cat.get("cost_credits"));
            this.response.appendInt((int) cat.get("cost_points"));
            this.response.appendInt((int) cat.get("points_type"));
            this.response.appendInt((int) cat.get("page_id"));
            this.response.appendString((String) cat.getOrDefault("page_caption", ""));
        }

        // FurniData JSON
        this.response.appendString(this.furniDataJson != null ? this.furniDataJson : "");

        return this.response;
    }
}
