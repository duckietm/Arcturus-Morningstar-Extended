package com.eu.habbo.habbohotel.navigation;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.rooms.Room;
import com.eu.habbo.habbohotel.users.Habbo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class NavigatorUserFilter extends NavigatorFilter {
    public final static String name = "myworld_view";

    public NavigatorUserFilter() {
        super(name);
    }

    @Override
    public List<SearchResultList> getResult(Habbo habbo) {
        int i = 0;
        List<SearchResultList> resultLists = new ArrayList<>();

        List<Room> rooms = Emulator.getGameEnvironment().getNavigatorManager().getRoomsForCategory("my", habbo);
        resultLists.add(new SearchResultList(i, "my", "", SearchAction.NONE, habbo.getHabboStats().navigatorWindowSettings.getListModeForCategory("my"), habbo.getHabboStats().navigatorWindowSettings.getDisplayModeForCategory("my"), rooms, true, true, DisplayOrder.ORDER_NUM, i));
        i++;

        List<Room> favoriteRooms = Emulator.getGameEnvironment().getNavigatorManager().getRoomsForCategory("favorites", habbo);
        if (!favoriteRooms.isEmpty()) {
            resultLists.add(new SearchResultList(i, "favorites", "", SearchAction.NONE, habbo.getHabboStats().navigatorWindowSettings.getListModeForCategory("favorites"), habbo.getHabboStats().navigatorWindowSettings.getDisplayModeForCategory("favorites"), favoriteRooms, true, true, DisplayOrder.ORDER_NUM, i));
            i++;
        }

        List<Room> frequentlyVisited = Emulator.getGameEnvironment().getNavigatorManager().getRoomsForCategory("history_freq", habbo);
        if (!frequentlyVisited.isEmpty()) {
            resultLists.add(new SearchResultList(i, "history_freq", "", SearchAction.NONE, habbo.getHabboStats().navigatorWindowSettings.getListModeForCategory("history_freq"), habbo.getHabboStats().navigatorWindowSettings.getDisplayModeForCategory("history_freq"), frequentlyVisited, true, true, DisplayOrder.ORDER_NUM, i));
            i++;
        }

        List<Room> groupRooms = Emulator.getGameEnvironment().getNavigatorManager().getRoomsForCategory("my_groups", habbo);
        if (!groupRooms.isEmpty()) {
            resultLists.add(new SearchResultList(i, "my_groups", "", SearchAction.NONE, habbo.getHabboStats().navigatorWindowSettings.getListModeForCategory("my_groups"), habbo.getHabboStats().navigatorWindowSettings.getDisplayModeForCategory("my_groups"), groupRooms, true, true, DisplayOrder.ORDER_NUM, i));
            i++;
        }

        List<Room> friendRooms = Emulator.getGameEnvironment().getNavigatorManager().getRoomsForCategory("with_friends", habbo);
        if (!friendRooms.isEmpty()) {
            resultLists.add(new SearchResultList(i, "with_friends", "", SearchAction.NONE, habbo.getHabboStats().navigatorWindowSettings.getListModeForCategory("with_friends"), habbo.getHabboStats().navigatorWindowSettings.getDisplayModeForCategory("with_friends"), friendRooms, true, true, DisplayOrder.ORDER_NUM, i));
            i++;
        }

        List<Room> rightRooms = Emulator.getGameEnvironment().getNavigatorManager().getRoomsForCategory("with_rights", habbo);
        if (!rightRooms.isEmpty()) {
            resultLists.add(new SearchResultList(i, "with_rights", "", SearchAction.NONE, habbo.getHabboStats().navigatorWindowSettings.getListModeForCategory("with_rights"), habbo.getHabboStats().navigatorWindowSettings.getDisplayModeForCategory("with_rights"), rightRooms, true, true, DisplayOrder.ORDER_NUM, i));
        }

        return resultLists;
    }
}