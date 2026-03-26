package com.eu.habbo.habbohotel.wired.core;

import com.eu.habbo.habbohotel.items.interactions.InteractionWiredExtra;
import com.eu.habbo.habbohotel.items.interactions.wired.extra.WiredExtraTextOutputUsername;
import com.eu.habbo.habbohotel.rooms.Room;
import com.eu.habbo.habbohotel.rooms.RoomUnit;
import com.eu.habbo.habbohotel.users.Habbo;
import com.eu.habbo.habbohotel.users.HabboItem;
import gnu.trove.set.hash.THashSet;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

public final class WiredTextPlaceholderUtil {
    private WiredTextPlaceholderUtil() {
    }

    public static String applyUsernamePlaceholders(WiredContext ctx, String text) {
        if (ctx == null || text == null || text.isEmpty()) {
            return text;
        }

        Room room = ctx.room();
        HabboItem triggerItem = ctx.triggerItem();

        if (room == null || triggerItem == null || room.getRoomSpecialTypes() == null) {
            return text;
        }

        THashSet<InteractionWiredExtra> extras = room.getRoomSpecialTypes().getExtras(triggerItem.getX(), triggerItem.getY());
        if (extras == null || extras.isEmpty()) {
            return text;
        }

        String resolvedText = text;

        for (InteractionWiredExtra extra : WiredExecutionOrderUtil.sort(extras)) {
            if (!(extra instanceof WiredExtraTextOutputUsername)) {
                continue;
            }

            WiredExtraTextOutputUsername usernameExtra = (WiredExtraTextOutputUsername) extra;
            String placeholderToken = usernameExtra.getPlaceholderToken();

            if (placeholderToken.isEmpty() || !resolvedText.contains(placeholderToken)) {
                continue;
            }

            resolvedText = resolvedText.replace(placeholderToken, buildUsernameReplacement(ctx, usernameExtra));
        }

        return resolvedText;
    }

    public static boolean requiresActor(Room room, HabboItem stackItem) {
        if (room == null || stackItem == null || room.getRoomSpecialTypes() == null) {
            return false;
        }

        THashSet<InteractionWiredExtra> extras = room.getRoomSpecialTypes().getExtras(stackItem.getX(), stackItem.getY());
        if (extras == null || extras.isEmpty()) {
            return false;
        }

        for (InteractionWiredExtra extra : extras) {
            if (!(extra instanceof WiredExtraTextOutputUsername)) {
                continue;
            }

            int userSource = ((WiredExtraTextOutputUsername) extra).getUserSource();
            if ((userSource == WiredSourceUtil.SOURCE_TRIGGER) || (userSource == WiredSourceUtil.SOURCE_CLICKED_USER)) {
                return true;
            }
        }

        return false;
    }

    private static String buildUsernameReplacement(WiredContext ctx, WiredExtraTextOutputUsername extra) {
        List<RoomUnit> users = WiredSourceUtil.resolveUsers(ctx, extra.getUserSource());
        if (users.isEmpty()) {
            return "";
        }

        Room room = ctx.room();
        LinkedHashSet<Integer> seenUserIds = new LinkedHashSet<>();
        List<String> usernames = new ArrayList<>();

        for (RoomUnit unit : users) {
            if ((unit == null) || !seenUserIds.add(unit.getId())) {
                continue;
            }

            Habbo habbo = room.getHabbo(unit);
            if ((habbo == null) || (habbo.getHabboInfo() == null)) {
                continue;
            }

            usernames.add(habbo.getHabboInfo().getUsername());
        }

        if (usernames.isEmpty()) {
            return "";
        }

        if (extra.getPlaceholderType() == WiredExtraTextOutputUsername.TYPE_MULTIPLE) {
            return String.join(extra.getDelimiter(), usernames);
        }

        return usernames.get(0);
    }
}
