package com.eu.habbo.habbohotel.navigation;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.permissions.Permission;
import com.eu.habbo.habbohotel.users.Habbo;

import java.util.ArrayList;
import java.util.List;

public class NavigatorPublicFilter extends NavigatorFilter {
    public final static String name = "official_view";

    public NavigatorPublicFilter() {
        super(name);
    }

    @Override
    public List<SearchResultList> getResult(Habbo habbo) {
        boolean showInvisible = habbo.hasPermission(Permission.ACC_ENTERANYROOM) || habbo.hasPermission(Permission.ACC_ANYROOMOWNER);
        List<SearchResultList> resultLists = new ArrayList<>();

        int i = 0;
        resultLists.add(new SearchResultList(i, "official-root", "", SearchAction.NONE, habbo.getHabboStats().navigatorWindowSettings.getListModeForCategory("official-root", ListMode.THUMBNAILS), habbo.getHabboStats().navigatorWindowSettings.getDisplayModeForCategory("official-root"), Emulator.getGameEnvironment().getNavigatorManager().getRoomsForCategory("official-root", habbo), false, showInvisible, DisplayOrder.ORDER_NUM, -1));
        i++;

        for (NavigatorPublicCategory category : Emulator.getGameEnvironment().getNavigatorManager().publicCategories.values()) {
            if (!category.rooms.isEmpty()) {
                resultLists.add(new SearchResultList(i, "", category.name, SearchAction.NONE, habbo.getHabboStats().navigatorWindowSettings.getListModeForCategory(category.name, category.image), habbo.getHabboStats().navigatorWindowSettings.getDisplayModeForCategory(category.name), category.rooms, true, showInvisible, DisplayOrder.ORDER_NUM, category.order));
                i++;
            }
        }

        return resultLists;
    }
}