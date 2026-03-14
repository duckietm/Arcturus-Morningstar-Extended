package com.eu.habbo.messages.incoming.navigator;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.permissions.Rank;
import com.eu.habbo.habbohotel.rooms.Room;
import com.eu.habbo.messages.ServerMessage;
import com.eu.habbo.messages.incoming.MessageHandler;
import com.eu.habbo.messages.outgoing.navigator.PrivateRoomsComposer;
import com.eu.habbo.plugin.events.navigator.NavigatorSearchResultEvent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SearchRoomsEvent extends MessageHandler {
    private static final int MAX_CACHE_SIZE = 200;
    public final static Map<Rank, Map<String, ServerMessage>> cachedResults = new ConcurrentHashMap<>(4);

    private static Map<String, ServerMessage> createLRUCache() {
        return Collections.synchronizedMap(new LinkedHashMap<String, ServerMessage>(MAX_CACHE_SIZE, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<String, ServerMessage> eldest) {
                return size() > MAX_CACHE_SIZE;
            }
        });
    }

    @Override
    public int getRatelimit() {
        return 500;
    }

    @Override
    public void handle() throws Exception {
        String name = this.packet.readString();

        String prefix = "";
        String query = name;
        ArrayList<Room> rooms;

        ServerMessage message = null;
        Map<String, ServerMessage> rankCache = cachedResults.get(this.client.getHabbo().getHabboInfo().getRank());
        if (rankCache != null) {
            message = rankCache.get((name + "\t" + query).toLowerCase());
        } else {
            rankCache = createLRUCache();
            cachedResults.put(this.client.getHabbo().getHabboInfo().getRank(), rankCache);
        }

        if (message == null) {
            if (name.startsWith("owner:")) {
                query = name.split("owner:")[1];
                prefix = "owner:";
                rooms = (ArrayList<Room>) Emulator.getGameEnvironment().getRoomManager().getRoomsForHabbo(name);
            } else if (name.startsWith("tag:")) {
                query = name.split("tag:")[1];
                prefix = "tag:";
                rooms = Emulator.getGameEnvironment().getRoomManager().getRoomsWithTag(name);
            } else if (name.startsWith("group:")) {
                query = name.split("group:")[1];
                prefix = "group:";
                rooms = Emulator.getGameEnvironment().getRoomManager().getGroupRoomsWithName(name);
            } else {
                rooms = Emulator.getGameEnvironment().getRoomManager().getRoomsWithName(name);
            }

            message = new PrivateRoomsComposer(rooms).compose();
            Map<String, ServerMessage> map = cachedResults.get(this.client.getHabbo().getHabboInfo().getRank());

            if (map == null) {
                map = createLRUCache();
            }

            map.put((name + "\t" + query).toLowerCase(), message);
            cachedResults.put(this.client.getHabbo().getHabboInfo().getRank(), map);

            NavigatorSearchResultEvent event = new NavigatorSearchResultEvent(this.client.getHabbo(), prefix, query, rooms);
            if (Emulator.getPluginManager().fireEvent(event).isCancelled()) {
                return;
            }
        }

        this.client.sendResponse(message);
    }
}