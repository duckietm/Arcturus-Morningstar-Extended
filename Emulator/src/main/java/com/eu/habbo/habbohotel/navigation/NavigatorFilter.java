package com.eu.habbo.habbohotel.navigation;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.rooms.Room;
import com.eu.habbo.habbohotel.users.Habbo;
import org.apache.commons.lang3.StringUtils;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public abstract class NavigatorFilter {
    public final String viewName;

    public NavigatorFilter(String viewName) {
        this.viewName = viewName;
    }

    public void filter(Method method, Object value, List<SearchResultList> collection) {
        if (method == null) {
            return;
        }

        if (value instanceof String) {
            if (((String) value).isEmpty()) {
                return;
            }
        }

        for (SearchResultList result : collection) {
            if (!result.filter) {
                continue;
            }

            this.filterRooms(method, value, result.rooms);
        }
    }

    public void filterRooms(Method method, Object value, List<Room> result) {
        if (method == null) {
            return;
        }

        if (value instanceof String) {
            if (((String) value).isEmpty()) {
                return;
            }
        }

        List<Room> toRemove = new ArrayList<>();
        method.setAccessible(true);
        NavigatorFilterComparator comparator = Emulator.getGameEnvironment().getNavigatorManager().comperatorForField(method);
        if (comparator == null) comparator = NavigatorFilterComparator.CONTAINS;

        for (Room room : result) {
            try {
                Object o = method.invoke(room);
                boolean matches;
                if (o == null) {
                    matches = false;
                } else if (o instanceof String && value instanceof String) {
                    matches = this.applies(comparator, (String) o, (String) value);
                } else if (o instanceof String[] && value instanceof String) {
                    // filterAnything(): any field may match
                    matches = false;
                    for (String s : (String[]) o) {
                        if (s != null && this.applies(comparator, s, (String) value)) {
                            matches = true;
                            break;
                        }
                    }
                } else {
                    matches = o.equals(value);
                }
                if (!matches) toRemove.add(room);
            } catch (Exception e) {
                toRemove.add(room);
            }
        }

        result.removeAll(toRemove);
        toRemove.clear();
    }

    public abstract List<SearchResultList> getResult(Habbo habbo);

    public List<SearchResultList> getResult(Habbo habbo, NavigatorFilterField filterField, String value, int roomCategory) {
        return this.getResult(habbo);
    }

    private boolean applies(NavigatorFilterComparator comparator, String o, String value) {
        switch (comparator) {
            case CONTAINS:
                if (StringUtils.containsIgnoreCase(o,
                        value)) {
                    return true;
                }
                break;

            case EQUALS:
                if (o.equals(value)) {
                    return true;
                }
                // ";"-joined lists (room tags): equal to any single entry
                for (String entry : o.split(";")) {
                    if (entry.trim().equalsIgnoreCase(value.trim())) return true;
                }
                break;

            case EQUALS_IGNORE_CASE:
                if (o.equalsIgnoreCase(value)) {
                    return true;
                }
        }

        return false;
    }
}