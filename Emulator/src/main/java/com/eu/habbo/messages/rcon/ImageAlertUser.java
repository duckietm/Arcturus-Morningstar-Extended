package com.eu.habbo.messages.rcon;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.users.Habbo;
import com.eu.habbo.messages.outgoing.generic.alerts.BubbleAlertComposer;
import com.google.gson.Gson;
import gnu.trove.map.hash.THashMap;

public class ImageAlertUser extends RCONMessage<ImageAlertUser.JSON> {
    public ImageAlertUser() {
        super(ImageAlertUser.JSON.class);
    }

    @Override
    public void handle(Gson gson, JSON json) {
        Habbo habbo = Emulator.getGameEnvironment().getHabboManager().getHabbo(json.user_id);

        if (habbo == null) {
            this.status = HABBO_NOT_FOUND;
            return;
        }

        THashMap<String, String> keys = new THashMap<>();

        if (!json.message.isEmpty()) {
            keys.put("message", json.message);
        }

        if (!json.url.isEmpty()) {
            keys.put("linkUrl", json.url);
        }

        if (!json.url_message.isEmpty()) {
            keys.put("linkTitle", json.url_message);
        }

        if (!json.title.isEmpty()) {
            keys.put("title", json.title);
        }

        if (!json.display_type.isEmpty()) {
            keys.put("display", json.display_type);
        }

        if (!json.image.isEmpty()) {
            keys.put("image", json.image);
        }

        habbo.getClient().sendResponse(new BubbleAlertComposer(json.bubble_key, keys));
    }

    static class JSON {

        public int user_id;


        public String bubble_key = "";


        public String message = "";


        public String url = "";


        public String url_message = "";


        public String title = "";


        public String display_type = "";


        public String image = "";
    }
}