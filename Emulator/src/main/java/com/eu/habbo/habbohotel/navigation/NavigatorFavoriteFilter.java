package com.eu.habbo.habbohotel.navigation;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.rooms.Room;
import com.eu.habbo.habbohotel.users.Habbo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class NavigatorFavoriteFilter extends NavigatorFilter {
    public final static String name = "favorites";

    public NavigatorFavoriteFilter() {
        super(name);
    }

    @Override
    public List<SearchResultList> getResult(Habbo habbo) {
        List<SearchResultList> resultLists = new ArrayList<>();
        List<Room> rooms = Emulator.getGameEnvironment().getNavigatorManager().getRoomsForCategory("favorites", habbo);
        resultLists.add(new SearchResultList(0, "favorites", "", SearchAction.NONE, habbo.getHabboStats().navigatorWindowSettings.getListModeForCategory("favorites", ListMode.LIST), habbo.getHabboStats().navigatorWindowSettings.getDisplayModeForCategory("popular", DisplayMode.VISIBLE), rooms, true, true, DisplayOrder.ACTIVITY, -1));
        return resultLists;
    }
}