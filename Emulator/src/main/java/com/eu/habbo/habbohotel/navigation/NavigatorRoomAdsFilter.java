package com.eu.habbo.habbohotel.navigation;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.permissions.Permission;
import com.eu.habbo.habbohotel.users.Habbo;

import java.util.ArrayList;
import java.util.List;

public class NavigatorRoomAdsFilter extends NavigatorFilter {
    public final static String name = "roomads_view";

    public NavigatorRoomAdsFilter() {
        super(name);
    }

    @Override
    public List<SearchResultList> getResult(Habbo habbo) {
        boolean showInvisible = habbo.hasPermission(Permission.ACC_ENTERANYROOM) || habbo.hasPermission(Permission.ACC_ANYROOMOWNER);
        List<SearchResultList> resultList = new ArrayList<>();
        resultList.add(new SearchResultList(0, "categories", "", SearchAction.NONE, habbo.getHabboStats().navigatorWindowSettings.getListModeForCategory("categories", ListMode.LIST), habbo.getHabboStats().navigatorWindowSettings.getDisplayModeForCategory("official-root", DisplayMode.VISIBLE), Emulator.getGameEnvironment().getNavigatorManager().getRoomsForCategory("categories", habbo), false, showInvisible, DisplayOrder.ACTIVITY, 0));
        return resultList;
    }
}