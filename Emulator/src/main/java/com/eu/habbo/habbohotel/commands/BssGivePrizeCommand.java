package com.eu.habbo.habbohotel.commands;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.items.FurnitureType;
import com.eu.habbo.habbohotel.items.Item;
import com.eu.habbo.habbohotel.items.ItemManager;
import com.eu.habbo.habbohotel.gameclients.GameClient;
import com.eu.habbo.habbohotel.modtool.HousekeepingAuditLog;
import com.eu.habbo.habbohotel.rooms.RoomChatMessageBubbles;
import com.eu.habbo.habbohotel.users.Habbo;
import com.eu.habbo.habbohotel.users.HabboInfo;
import com.eu.habbo.habbohotel.users.HabboItem;
import com.eu.habbo.habbohotel.users.HabboManager;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** Gives an online user actual, persisted BSS rares selected by their exact diamond value. */
final class BssGivePrizeCommand extends Command {
    private static final int DEFAULT_MAX_QUANTITY = 25;

    BssGivePrizeCommand() {
        super("cmd_bss_give_prize", Emulator.getTexts().getValue("commands.keys.cmd_bss_give_prize").split(";"));
    }

    @Override
    public boolean handle(GameClient gameClient, String[] params) {
        if (params.length < 3 || params.length > 4) {
            whisperText(gameClient, "commands.error.cmd_bss_give_prize.usage");
            return true;
        }

        Integer requestedValue = parsePositive(params[2]);
        if (requestedValue == null) {
            whisperText(gameClient, "commands.error.cmd_bss_give_prize.invalid_value");
            return true;
        }

        int maxQuantity = Math.max(1, Emulator.getConfig().getInt(
                "bss.commands.give_prize.max_quantity", DEFAULT_MAX_QUANTITY));
        Integer quantity = params.length == 4 ? parsePositive(params[3]) : 1;
        if (quantity == null || quantity > maxQuantity) {
            whisper(gameClient, Emulator.getTexts().getValue("commands.error.cmd_bss_give_prize.invalid_quantity")
                    .replace("%max%", Integer.toString(maxQuantity)));
            return true;
        }

        HabboInfo targetInfo = HabboManager.getOfflineHabboInfo(params[1]);
        if (targetInfo == null) {
            whisper(gameClient, Emulator.getTexts().getValue("commands.error.cmd_bss_give_prize.user_not_found")
                    .replace("%user%", params[1]));
            return true;
        }

        // A prize is inventory data, not a room interaction. BSS staff commands
        // must deliver the whole requested batch even when the recipient has
        // just changed room or is offline.
        Habbo target = Emulator.getGameEnvironment().getHabboManager().getHabbo(targetInfo.getId());

        List<Item> candidates = candidatesForValue(requestedValue);
        if (candidates.isEmpty()) {
            whisper(gameClient, Emulator.getTexts().getValue("commands.error.cmd_bss_give_prize.no_rares")
                    .replace("%value%", Integer.toString(requestedValue)));
            return true;
        }

        // Every cycle is reshuffled. This makes every item a fresh random draw and avoids
        // repeating a rare within the batch while unused alternatives still exist.
        List<Item> selected = drawRandomRares(candidates, quantity);
        List<HabboItem> created = persistPrize(targetInfo.getId(), selected);
        if (created == null) {
            whisperText(gameClient, "commands.error.cmd_bss_give_prize.storage");
            return true;
        }

        if (target != null) target.addFurniture(created);
        String itemSummary = summarize(selected);
        if (target != null) {
            String recipientTextKey = quantity == 1
                    ? "commands.generic.cmd_bss_give_prize.received_one"
                    : "commands.generic.cmd_bss_give_prize.received_many";
            target.whisper(Emulator.getTexts().getValue(recipientTextKey)
                    .replace("%quantity%", Integer.toString(quantity))
                    .replace("%value%", Integer.toString(requestedValue))
                    .replace("%items%", itemSummary), RoomChatMessageBubbles.STAFF);
        }

        whisper(gameClient, Emulator.getTexts().getValue("commands.success.cmd_bss_give_prize")
                .replace("%quantity%", Integer.toString(quantity))
                .replace("%user%", targetInfo.getUsername())
                .replace("%value%", Integer.toString(requestedValue))
                .replace("%items%", itemSummary));

        HousekeepingAuditLog.log(
                gameClient.getHabbo().getHabboInfo().getId(),
                gameClient.getHabbo().getHabboInfo().getUsername(),
                "command.give_prize",
                targetInfo.getId(),
                "value=" + requestedValue + " quantity=" + quantity + " base_item_ids="
                        + selected.stream().map(item -> Integer.toString(item.getId())).collect(Collectors.joining(",")),
                gameClient.getHabbo().getHabboInfo().getIpLogin());
        return true;
    }

    private List<Item> candidatesForValue(int requestedValue) {
        int diamondType = Emulator.getConfig().getInt("seasonal.currency.diamond", 5);
        Map<Integer, Item> bySprite = new LinkedHashMap<>();
        for (Item item : Emulator.getGameEnvironment().getItemManager().getItems().values()) {
            int[] value = Emulator.getGameEnvironment().getCatalogManager()
                    .getFurnitureValues().get(item.getSpriteId());
            if (value == null || value.length < 3 || value[1] != requestedValue || value[2] != diamondType) continue;
            if (item.getType() != FurnitureType.FLOOR && item.getType() != FurnitureType.WALL) continue;
            bySprite.putIfAbsent(item.getSpriteId(), item);
        }
        return new ArrayList<>(bySprite.values());
    }

    static List<Item> drawRandomRares(List<Item> candidates, int quantity) {
        List<Item> result = new ArrayList<>(quantity);
        List<Item> cycle = new ArrayList<>();
        while (result.size() < quantity) {
            if (cycle.isEmpty()) {
                cycle.addAll(candidates);
                Collections.shuffle(cycle, Emulator.getRandom());
                if (!result.isEmpty() && cycle.size() > 1
                        && cycle.get(cycle.size() - 1).getSpriteId() == result.get(result.size() - 1).getSpriteId()) {
                    Collections.swap(cycle, cycle.size() - 1, 0);
                }
            }
            result.add(cycle.remove(cycle.size() - 1));
        }
        return result;
    }

    private List<HabboItem> persistPrize(int targetId, List<Item> selected) {
        ItemManager itemManager = Emulator.getGameEnvironment().getItemManager();
        List<HabboItem> created = new ArrayList<>(selected.size());
        try (Connection connection = Emulator.getDatabase().getDataSource().getConnection()) {
            connection.setAutoCommit(false);
            try {
                for (Item item : selected) {
                    HabboItem createdItem = itemManager.createItem(connection, targetId, item, 0, 0, "");
                    if (createdItem == null) throw new SQLException("Could not create prize item " + item.getId());
                    created.add(createdItem);
                }
                connection.commit();
                return created;
            } catch (Exception exception) {
                connection.rollback();
                return null;
            }
        } catch (SQLException exception) {
            return null;
        }
    }

    private String summarize(List<Item> selected) {
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (Item item : selected) {
            String name = item.getDisplayName().isBlank() ? item.getName() : item.getDisplayName();
            counts.merge(name, 1, Integer::sum);
        }
        return counts.entrySet().stream()
                .map(entry -> entry.getKey() + (entry.getValue() > 1 ? " x" + entry.getValue() : ""))
                .collect(Collectors.joining(", "));
    }

    private Integer parsePositive(String value) {
        try {
            int parsed = Integer.parseInt(value);
            return parsed > 0 ? parsed : null;
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private void whisperText(GameClient gameClient, String textKey) {
        whisper(gameClient, Emulator.getTexts().getValue(textKey));
    }

    private void whisper(GameClient gameClient, String message) {
        gameClient.getHabbo().whisper(message, RoomChatMessageBubbles.ALERT);
    }
}
