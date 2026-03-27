package com.eu.habbo.habbohotel.wired.core;

import com.eu.habbo.habbohotel.bots.Bot;
import com.eu.habbo.habbohotel.items.interactions.InteractionWiredExtra;
import com.eu.habbo.habbohotel.items.interactions.wired.extra.WiredExtraTextOutputFurniName;
import com.eu.habbo.habbohotel.items.interactions.wired.extra.WiredExtraTextOutputUsername;
import com.eu.habbo.habbohotel.pets.Pet;
import com.eu.habbo.habbohotel.rooms.Room;
import com.eu.habbo.habbohotel.rooms.RoomUnit;
import com.eu.habbo.habbohotel.rooms.RoomUnitType;
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
            if (extra instanceof WiredExtraTextOutputUsername) {
                WiredExtraTextOutputUsername usernameExtra = (WiredExtraTextOutputUsername) extra;
                String placeholderToken = usernameExtra.getPlaceholderToken();

                if (!placeholderToken.isEmpty() && resolvedText.contains(placeholderToken)) {
                    resolvedText = resolvedText.replace(placeholderToken, buildUsernameReplacement(ctx, usernameExtra));
                }

                continue;
            }

            if (extra instanceof WiredExtraTextOutputFurniName) {
                WiredExtraTextOutputFurniName furniExtra = (WiredExtraTextOutputFurniName) extra;
                String placeholderToken = furniExtra.getPlaceholderToken();

                if (!placeholderToken.isEmpty() && resolvedText.contains(placeholderToken)) {
                    resolvedText = resolvedText.replace(placeholderToken, buildFurniNameReplacement(ctx, furniExtra));
                }
            }
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

        LinkedHashSet<Integer> seenUserIds = new LinkedHashSet<>();
        List<String> usernames = new ArrayList<>();

        for (RoomUnit unit : users) {
            if ((unit == null) || !seenUserIds.add(unit.getId())) {
                continue;
            }

            String roomUnitName = getRoomUnitName(ctx.room(), unit);
            if (roomUnitName == null || roomUnitName.trim().isEmpty()) {
                continue;
            }

            usernames.add(roomUnitName);
        }

        if (usernames.isEmpty()) {
            return "";
        }

        if (extra.getPlaceholderType() == WiredExtraTextOutputUsername.TYPE_MULTIPLE) {
            return String.join(extra.getDelimiter(), usernames);
        }

        return usernames.get(0);
    }

    private static String buildFurniNameReplacement(WiredContext ctx, WiredExtraTextOutputFurniName extra) {
        List<HabboItem> items;
        if (extra.getFurniSource() == WiredSourceUtil.SOURCE_SELECTOR) {
            items = WiredSourceUtil.resolveSelectorItems(ctx, true);
        } else {
            items = WiredSourceUtil.resolveItems(ctx, extra.getFurniSource(), extra.getItems());
        }

        if (items.isEmpty()) {
            return "";
        }

        LinkedHashSet<Integer> seenItemIds = new LinkedHashSet<>();
        List<String> furniNames = new ArrayList<>();

        for (HabboItem item : items) {
            if ((item == null) || (item.getBaseItem() == null) || !seenItemIds.add(item.getId())) {
                continue;
            }

            String furniName = item.getBaseItem().getFullName();
            if (furniName == null || furniName.trim().isEmpty()) {
                furniName = item.getBaseItem().getName();
            }

            if (furniName == null || furniName.trim().isEmpty()) {
                continue;
            }

            furniNames.add(furniName);
        }

        if (furniNames.isEmpty()) {
            return "";
        }

        if (extra.getPlaceholderType() == WiredExtraTextOutputFurniName.TYPE_MULTIPLE) {
            return String.join(extra.getDelimiter(), furniNames);
        }

        return furniNames.get(0);
    }

    private static String getRoomUnitName(Room room, RoomUnit roomUnit) {
        if (room == null || roomUnit == null) {
            return "";
        }

        if (roomUnit.getRoomUnitType() == RoomUnitType.USER) {
            Habbo habbo = room.getHabbo(roomUnit);
            return (habbo != null && habbo.getHabboInfo() != null) ? habbo.getHabboInfo().getUsername() : "";
        }

        if (roomUnit.getRoomUnitType() == RoomUnitType.BOT) {
            Bot bot = room.getBot(roomUnit);
            return (bot != null) ? bot.getName() : "";
        }

        if (roomUnit.getRoomUnitType() == RoomUnitType.PET) {
            Pet pet = room.getPet(roomUnit);
            return (pet != null) ? pet.getName() : "";
        }

        return "";
    }
}
