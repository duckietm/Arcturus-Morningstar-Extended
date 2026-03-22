package com.eu.habbo.messages.incoming.furnieditor;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

public class FurniEditorHelper {

    public static Map<String, Object> readBasicItem(ResultSet rs) throws SQLException {
        Map<String, Object> item = new HashMap<>();
        item.put("id", rs.getInt("id"));
        item.put("sprite_id", rs.getInt("sprite_id"));
        item.put("item_name", rs.getString("item_name") != null ? rs.getString("item_name") : "");
        item.put("public_name", rs.getString("public_name") != null ? rs.getString("public_name") : "");
        item.put("type", rs.getString("type") != null ? rs.getString("type") : "s");
        item.put("width", rs.getInt("width"));
        item.put("length", rs.getInt("length"));
        item.put("stack_height", rs.getDouble("stack_height"));
        item.put("allow_stack", "1".equals(rs.getString("allow_stack")));
        item.put("allow_walk", "1".equals(rs.getString("allow_walk")));
        item.put("allow_sit", "1".equals(rs.getString("allow_sit")));
        item.put("allow_lay", "1".equals(rs.getString("allow_lay")));
        item.put("interaction_type", rs.getString("interaction_type") != null ? rs.getString("interaction_type") : "default");
        item.put("interaction_modes_count", rs.getInt("interaction_modes_count"));
        return item;
    }

    public static Map<String, Object> readDetailItem(ResultSet rs) throws SQLException {
        Map<String, Object> item = readBasicItem(rs);
        item.put("allow_gift", "1".equals(rs.getString("allow_gift")));
        item.put("allow_trade", "1".equals(rs.getString("allow_trade")));
        item.put("allow_recycle", "1".equals(rs.getString("allow_recycle")));
        item.put("allow_marketplace_sell", "1".equals(rs.getString("allow_marketplace_sell")));
        item.put("allow_inventory_stack", "1".equals(rs.getString("allow_inventory_stack")));
        item.put("vending_ids", rs.getString("vending_ids") != null ? rs.getString("vending_ids") : "");
        item.put("customparams", rs.getString("customparams") != null ? rs.getString("customparams") : "");
        item.put("effect_id_male", rs.getInt("effect_id_male"));
        item.put("effect_id_female", rs.getInt("effect_id_female"));
        item.put("clothing_on_walk", rs.getString("clothing_on_walk") != null ? rs.getString("clothing_on_walk") : "");
        item.put("multiheight", rs.getString("multiheight") != null ? rs.getString("multiheight") : "");
        item.put("description", rs.getString("description") != null ? rs.getString("description") : "");
        return item;
    }

    public static Map<String, Object> readCatalogRef(ResultSet rs) throws SQLException {
        Map<String, Object> ref = new HashMap<>();
        ref.put("id", rs.getInt("ci_id"));
        ref.put("catalog_name", rs.getString("catalog_name") != null ? rs.getString("catalog_name") : "");
        ref.put("cost_credits", rs.getInt("cost_credits"));
        ref.put("cost_points", rs.getInt("cost_points"));
        ref.put("points_type", rs.getInt("points_type"));
        ref.put("page_id", rs.getInt("page_id"));
        ref.put("page_caption", rs.getString("page_caption") != null ? rs.getString("page_caption") : "");
        return ref;
    }
}
