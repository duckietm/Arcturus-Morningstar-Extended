package com.eu.habbo.messages.incoming.navigator;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.navigation.*;
import com.eu.habbo.habbohotel.permissions.Permission;
import com.eu.habbo.habbohotel.rooms.Room;
import com.eu.habbo.habbohotel.rooms.RoomCategory;
import com.eu.habbo.habbohotel.rooms.RoomState;
import com.eu.habbo.messages.incoming.MessageHandler;
import com.eu.habbo.messages.outgoing.navigator.NewNavigatorSearchResultsComposer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class RequestNewNavigatorRoomsEvent extends MessageHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(RequestNewNavigatorRoomsEvent.class);
    private static final int MAX_VIEW_LENGTH = 32;
    private static final int MAX_QUERY_LENGTH = 64;

    @Override
    public int getRatelimit() {
        return 200;
    }

    @Override
    public void handle() throws Exception {
        String view = this.packet.readString();
        String query = this.packet.readString();
        if (view.length() > MAX_VIEW_LENGTH) return;
        if (query.length() > MAX_QUERY_LENGTH) query = query.substring(0, MAX_QUERY_LENGTH);
        if (view.equals("query")) view = "hotel_view";
        if (view.equals("groups")) view = "hotel_view";

        NavigatorManager navigatorManager = Emulator.getGameEnvironment().getNavigatorManager();
        NavigatorFilter filter = navigatorManager.filters.get(view);
        RoomCategory category = Emulator.getGameEnvironment().getRoomManager().getCategoryBySafeCaption(view);

        if (filter == null) {
            List<Room> rooms = navigatorManager.getRoomsForCategory(view, this.client.getHabbo());

            if (rooms != null) {
                List<SearchResultList> resultLists = new ArrayList<>();
                resultLists.add(new SearchResultList(0, view, query, SearchAction.NONE, this.client.getHabbo().getHabboStats().navigatorWindowSettings.getListModeForCategory(view, ListMode.LIST), this.client.getHabbo().getHabboStats().navigatorWindowSettings.getDisplayModeForCategory(view, DisplayMode.VISIBLE), rooms, true, true, DisplayOrder.ACTIVITY, -1));
                this.client.sendResponse(new NewNavigatorSearchResultsComposer(view, query, resultLists));
                return;
            }
        }

        // "<prefix>:<text>" — the prefix is matched case-insensitively and both halves trimmed,
        // so "Roomname: tribute" behaves like "roomname:tribute".
        String filterField = "anything";
        String part = query.trim();
        if (query.contains(":")) {
            String[] parts = query.split(":", 2);
            String prefix = parts[0].trim().toLowerCase(Locale.ROOT);
            if (navigatorManager.filterSettings.containsKey(prefix)) {
                filterField = prefix;
                part = parts.length > 1 ? parts[1].trim() : "";
            }
        }
        NavigatorFilterField field = navigatorManager.filterSettings.get(filterField);

        if (field == null || part.isEmpty()) {
            if (filter == null)
                return;

            List<SearchResultList> resultLists = filter.getResult(this.client.getHabbo());
            Collections.sort(resultLists);

            if (!query.isEmpty()) {
                resultLists = toQueryResults(resultLists);
            }

            this.client.sendResponse(new NewNavigatorSearchResultsComposer(view, query, resultLists));
            return;
        }

        NavigatorFilter hotelFilter = navigatorManager.filters.get("hotel_view");
        if (filter == null) filter = hotelFilter;
        if (filter == null) return;

        try {
            Map<Integer, Room> found = new LinkedHashMap<>();

            // 1) Global, database-backed search over every room (all categories), whatever tab
            //    the user is on — the tab only decides which extra in-memory lists are merged in.
            if (hotelFilter != null) {
                for (SearchResultList list : hotelFilter.getResult(this.client.getHabbo(), field, part, -1)) {
                    for (Room room : list.rooms) found.put(room.getId(), room);
                }
            }

            // 2) The current tab's own lists (my rooms, favourites, ads, publics …) filtered in memory,
            //    so rooms the SQL search cannot reach (own invisible rooms, guild filter) still show up.
            if (filter != hotelFilter) {
                List<SearchResultList> viewLists = new ArrayList<>();
                for (SearchResultList original : filter.getResult(this.client.getHabbo())) {
                    viewLists.add(new SearchResultList(original.order, original.code, original.query, original.action, original.mode, original.hidden, new ArrayList<>(original.rooms), original.filter, original.showInvisible, original.displayOrder, original.categoryOrder));
                }
                if ("group".equals(filterField)) {
                    final String needle = part.toLowerCase(Locale.ROOT);
                    for (SearchResultList list : viewLists) {
                        list.rooms.removeIf(room -> !room.belongsToGuild()
                                || (!needle.isEmpty() && !safeLower(room.getGuildName()).contains(needle)));
                    }
                }
                filter.filter(field.field, part, viewLists);
                for (SearchResultList list : viewLists) {
                    for (Room room : list.rooms) found.putIfAbsent(room.getId(), room);
                }
            }

            SearchResultList wrapper = new SearchResultList(0, "query", "", SearchAction.NONE, ListMode.LIST, DisplayMode.VISIBLE, new ArrayList<>(found.values()), true, true, DisplayOrder.ACTIVITY, -1);
            List<SearchResultList> resultLists = toQueryResults(Collections.singletonList(wrapper));
            this.client.sendResponse(new NewNavigatorSearchResultsComposer(view, query, resultLists));
        } catch (Exception e) {
            LOGGER.error("Caught exception", e);
        }
    }

    private static String safeLower(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT);
    }

    /**
     * Collapses every list into the single "query" block. Invisible rooms are dropped unless the
     * searcher is staff or owns them (own hidden rooms must stay findable from "my world").
     */
    private ArrayList<SearchResultList> toQueryResults(List<SearchResultList> resultLists) {
        ArrayList<SearchResultList> nList = new ArrayList<>();
        Map<Integer, Room> searchRooms = new LinkedHashMap<>();
        boolean staff = this.client.getHabbo().hasPermission(Permission.ACC_ENTERANYROOM) || this.client.getHabbo().hasPermission(Permission.ACC_ANYROOMOWNER);
        int userId = this.client.getHabbo().getHabboInfo().getId();

        for (SearchResultList li : resultLists) {
            for (Room room : li.rooms) {
                if (room == null) continue;
                if (!staff && room.getState() == RoomState.INVISIBLE && room.getOwnerId() != userId) continue;
                searchRooms.put(room.getId(), room);
            }
        }

        SearchResultList list = new SearchResultList(0, "query", "", SearchAction.NONE, ListMode.LIST, DisplayMode.VISIBLE, new ArrayList<Room>(searchRooms.values()), true, true, DisplayOrder.ACTIVITY, -1);
        nList.add(list);
        return nList;
    }
}
