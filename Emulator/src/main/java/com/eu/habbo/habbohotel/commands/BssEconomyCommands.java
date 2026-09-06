package com.eu.habbo.habbohotel.commands;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.catalog.CatalogManager;
import com.eu.habbo.habbohotel.gameclients.GameClient;
import com.eu.habbo.habbohotel.items.Item;
import com.eu.habbo.habbohotel.rooms.Room;
import com.eu.habbo.habbohotel.rooms.RoomChatMessageBubbles;
import com.eu.habbo.habbohotel.users.HabboItem;
import com.eu.habbo.messages.outgoing.inventory.InventoryRefreshComposer;
import com.eu.habbo.threading.runnables.QueryDeleteHabboItems;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

final class BssRedeemCurrencyCommand extends Command {
    enum Mode {
        CREDITS,
        DIAMONDS
    }

    private final Mode mode;

    BssRedeemCurrencyCommand(String permission, Mode mode) {
        super(permission, Emulator.getTexts().getValue("commands.keys." + permission).split(";"));
        this.mode = mode;
    }

    @Override
    public boolean handle(GameClient gameClient, String[] params) {
        Room room = gameClient.getHabbo().getHabboInfo().getCurrentRoom();
        if (room != null && room.getActiveTradeForHabbo(gameClient.getHabbo()) != null) return false;

        int diamondType = Emulator.getConfig().getInt("seasonal.currency.diamond", 5);
        long credits = 0;
        int diamonds = 0;
        List<HabboItem> redeemed = new ArrayList<>();

        for (HabboItem item : gameClient.getHabbo().getInventory().getItemsComponent().getItemsAsValueCollection()) {
            if (item.getUserId() != gameClient.getHabbo().getHabboInfo().getId()) continue;
            String name = item.getBaseItem().getName();
            Integer amount = null;

            if (mode == Mode.CREDITS
                    && (name.startsWith("CF_") || name.startsWith("CFC_"))
                    && !name.startsWith("CF_diamond_")) {
                amount = RedeemCommand.parsePositiveRedeemValue(name, 1);
                if (amount != null) credits = Math.addExact(credits, amount.longValue());
            } else if (mode == Mode.DIAMONDS && name.startsWith("CF_diamond_")) {
                amount = RedeemCommand.parsePositiveRedeemValue(name, 2);
                if (amount != null) diamonds = Math.addExact(diamonds, amount);
            } else if (mode == Mode.DIAMONDS && name.startsWith("DF_")) {
                Integer type = RedeemCommand.parsePositiveRedeemValue(name, 1);
                amount = RedeemCommand.parsePositiveRedeemValue(name, 2);
                if (type == null || type != diamondType) amount = null;
                if (amount != null) diamonds = Math.addExact(diamonds, amount);
            }

            if (amount != null) redeemed.add(item);
        }

        for (HabboItem item : redeemed) {
            gameClient.getHabbo().getInventory().getItemsComponent().removeHabboItem(item);
        }
        if (!redeemed.isEmpty()) Emulator.getThreading().runPersistence(new QueryDeleteHabboItems(redeemed));

        long remainingCredits = credits;
        while (remainingCredits > 0) {
            int chunk = (int) Math.min(Integer.MAX_VALUE, remainingCredits);
            gameClient.getHabbo().giveCredits(chunk);
            remainingCredits -= chunk;
        }
        if (diamonds > 0) gameClient.getHabbo().givePoints(diamondType, diamonds);
        gameClient.sendResponse(new InventoryRefreshComposer());
        gameClient.getHabbo().whisper(
                Emulator.getTexts().getValue("commands.success." + permission)
                        .replace("%items%", Integer.toString(redeemed.size()))
                        .replace("%amount%", Long.toString(mode == Mode.CREDITS ? credits : diamonds)),
                RoomChatMessageBubbles.ALERT);
        return true;
    }
}

final class BssRareValueCommand extends Command {
    enum Scope {
        ITEM,
        INVENTORY,
        ROOM
    }

    private final Scope scope;

    BssRareValueCommand(String permission, Scope scope) {
        super(permission, Emulator.getTexts().getValue("commands.keys." + permission).split(";"));
        this.scope = scope;
    }

    @Override
    public boolean handle(GameClient gameClient, String[] params) {
        return switch (scope) {
            case ITEM -> showItemValue(gameClient, params);
            case INVENTORY -> showCollectionValue(
                    gameClient,
                    gameClient.getHabbo().getInventory().getItemsComponent().getItemsAsValueCollection(),
                    "commands.generic.cmd_bss_inventory_value");
            case ROOM -> {
                Room room = gameClient.getHabbo().getHabboInfo().getCurrentRoom();
                if (room == null) yield true;
                List<HabboItem> items = new ArrayList<>(room.getFloorItems());
                items.addAll(room.getWallItems());
                yield showCollectionValue(gameClient, items, "commands.generic.cmd_bss_room_value");
            }
        };
    }

    private boolean showItemValue(GameClient gameClient, String[] params) {
        if (params.length < 2) {
            gameClient.getHabbo().whisper(
                    Emulator.getTexts().getValue("commands.error.cmd_bss_rare_value.usage"),
                    RoomChatMessageBubbles.ALERT);
            return true;
        }

        String query = String.join(" ", java.util.Arrays.copyOfRange(params, 1, params.length)).strip();
        String normalized = query.toLowerCase(Locale.ROOT);
        Item matched = null;
        for (Item item : Emulator.getGameEnvironment().getItemManager().getItems().values()) {
            if (item.getName().equalsIgnoreCase(query) || item.getDisplayName().equalsIgnoreCase(query)) {
                matched = item;
                break;
            }
            if (matched == null
                    && (item.getName().toLowerCase(Locale.ROOT).contains(normalized)
                            || item.getDisplayName().toLowerCase(Locale.ROOT).contains(normalized))) {
                matched = item;
            }
        }

        int[] value = matched == null ? null : values().get(matched.getSpriteId());
        if (matched == null || value == null) {
            gameClient.getHabbo().whisper(
                    Emulator.getTexts().getValue("commands.error.cmd_bss_rare_value.not_found")
                            .replace("%name%", query),
                    RoomChatMessageBubbles.ALERT);
            return true;
        }

        gameClient.getHabbo().alert(Emulator.getTexts().getValue("commands.generic.cmd_bss_rare_value")
                .replace("%name%", matched.getDisplayName())
                .replace("%credits%", Integer.toString(value[0]))
                .replace("%points%", Integer.toString(value[1]))
                .replace("%type%", currencyName(value[2])));
        return true;
    }

    private boolean showCollectionValue(
            GameClient gameClient, Collection<HabboItem> items, String textKey) {
        long credits = 0;
        Map<Integer, Long> points = new TreeMap<>();
        int valuedItems = 0;
        for (HabboItem item : items) {
            int[] value = values().get(item.getBaseItem().getSpriteId());
            if (value == null) continue;
            credits = Math.addExact(credits, value[0]);
            points.merge(value[2], (long) value[1], Math::addExact);
            valuedItems++;
        }

        StringBuilder totals = new StringBuilder();
        for (Map.Entry<Integer, Long> entry : points.entrySet()) {
            if (!totals.isEmpty()) totals.append("\r\n");
            totals.append(currencyName(entry.getKey())).append(": ").append(entry.getValue());
        }
        if (totals.isEmpty()) totals.append(currencyName(Emulator.getConfig().getInt("seasonal.currency.diamond", 5))).append(": 0");

        gameClient.getHabbo().alert(Emulator.getTexts().getValue(textKey)
                .replace("%items%", Integer.toString(valuedItems))
                .replace("%credits%", Long.toString(credits))
                .replace("%points%", totals.toString()));
        return true;
    }

    private it.unimi.dsi.fastutil.ints.Int2ObjectMap<int[]> values() {
        CatalogManager catalog = Emulator.getGameEnvironment().getCatalogManager();
        return catalog.getFurnitureValues();
    }

    private String currencyName(int type) {
        return Emulator.getTexts().getValueQuietly("seasonal.name." + type, "Valuta " + type);
    }
}
