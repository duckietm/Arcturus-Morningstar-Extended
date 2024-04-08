package com.eu.habbo.habbohotel.items.interactions.wired.effects;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.bots.Bot;
import com.eu.habbo.habbohotel.gameclients.GameClient;
import com.eu.habbo.habbohotel.items.Item;
import com.eu.habbo.habbohotel.items.interactions.InteractionWiredEffect;
import com.eu.habbo.habbohotel.items.interactions.wired.WiredSettings;
import com.eu.habbo.habbohotel.rooms.Room;
import com.eu.habbo.habbohotel.rooms.RoomTile;
import com.eu.habbo.habbohotel.rooms.RoomTileState;
import com.eu.habbo.habbohotel.rooms.RoomUnit;
import com.eu.habbo.habbohotel.users.HabboItem;
import com.eu.habbo.habbohotel.wired.WiredEffectType;
import com.eu.habbo.habbohotel.wired.WiredHandler;
import com.eu.habbo.messages.ClientMessage;
import com.eu.habbo.messages.ServerMessage;
import com.eu.habbo.messages.incoming.wired.WiredSaveException;
import com.eu.habbo.messages.outgoing.rooms.users.RoomUserEffectComposer;
import com.eu.habbo.threading.runnables.RoomUnitTeleport;
import com.eu.habbo.threading.runnables.SendRoomUnitEffectComposer;
import gnu.trove.set.hash.THashSet;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class WiredEffectBotTeleport extends InteractionWiredEffect {
    public static final WiredEffectType type = WiredEffectType.BOT_TELEPORT;

    private THashSet<HabboItem> items;
    private String botName = "";

    public WiredEffectBotTeleport(ResultSet set, Item baseItem) throws SQLException {
        super(set, baseItem);
        this.items = new THashSet<>();
    }

    public WiredEffectBotTeleport(int id, int userId, Item item, String extradata, int limitedStack, int limitedSells) {
        super(id, userId, item, extradata, limitedStack, limitedSells);
        this.items = new THashSet<>();
    }

    public static void teleportUnitToTile(RoomUnit roomUnit, RoomTile tile) {
        if (roomUnit == null || tile == null || roomUnit.isWiredTeleporting)
            return;

        Room room = roomUnit.getRoom();

        if (room == null) {
            return;
        }

        // makes a temporary effect

        roomUnit.getRoom().unIdle(roomUnit.getRoom().getHabbo(roomUnit));
        room.sendComposer(new RoomUserEffectComposer(roomUnit, 4).compose());
        Emulator.getThreading().run(new SendRoomUnitEffectComposer(room, roomUnit), WiredHandler.TELEPORT_DELAY + 1000);

        if (tile == roomUnit.getCurrentLocation()) {
            return;
        }

        if (tile.state == RoomTileState.INVALID || tile.state == RoomTileState.BLOCKED) {
            RoomTile alternativeTile = null;
            List<RoomTile> optionalTiles = room.getLayout().getTilesAround(tile);

            Collections.reverse(optionalTiles);
            for (RoomTile optionalTile : optionalTiles) {
                if (optionalTile.state != RoomTileState.INVALID && optionalTile.state != RoomTileState.BLOCKED) {
                    alternativeTile = optionalTile;
                    break;
                }
            }

            if (alternativeTile != null) {
                tile = alternativeTile;
            }
        }

        Emulator.getThreading().run(() -> { roomUnit.isWiredTeleporting = true; }, Math.max(0, WiredHandler.TELEPORT_DELAY - 500));
        Emulator.getThreading().run(new RoomUnitTeleport(roomUnit, room, tile.x, tile.y, tile.getStackHeight() + (tile.state == RoomTileState.SIT ? -0.5 : 0), roomUnit.getEffectId()), WiredHandler.TELEPORT_DELAY);
    }

    @Override
    public void serializeWiredData(ServerMessage message, Room room) {
        THashSet<HabboItem> items = new THashSet<>();

        for (HabboItem item : this.items) {
            if (item.getRoomId() != this.getRoomId() || Emulator.getGameEnvironment().getRoomManager().getRoom(this.getRoomId()).getHabboItem(item.getId()) == null)
                items.add(item);
        }

        for (HabboItem item : items) {
            this.items.remove(item);
        }

        message.appendBoolean(false);
        message.appendInt(WiredHandler.MAXIMUM_FURNI_SELECTION);
        message.appendInt(this.items.size());
        for (HabboItem item : this.items)
            message.appendInt(item.getId());

        message.appendInt(this.getBaseItem().getSpriteId());
        message.appendInt(this.getId());
        message.appendString(this.botName);
        message.appendInt(0);
        message.appendInt(0);
        message.appendInt(this.getType().code);
        message.appendInt(this.getDelay());
        message.appendInt(0);
    }

    @Override
    public boolean saveData(WiredSettings settings, GameClient gameClient) throws WiredSaveException {
        String botName = settings.getStringParam();
        int itemsCount = settings.getFurniIds().length;

        if(itemsCount > Emulator.getConfig().getInt("hotel.wired.furni.selection.count")) {
            throw new WiredSaveException("Too many furni selected");
        }

        List<HabboItem> newItems = new ArrayList<>();

        for (int i = 0; i < itemsCount; i++) {
            int itemId = settings.getFurniIds()[i];
            HabboItem it = Emulator.getGameEnvironment().getRoomManager().getRoom(this.getRoomId()).getHabboItem(itemId);

            if(it == null)
                throw new WiredSaveException(String.format("Item %s not found", itemId));

            newItems.add(it);
        }

        int delay = settings.getDelay();

        if(delay > Emulator.getConfig().getInt("hotel.wired.max_delay", 20))
            throw new WiredSaveException("Delay too long");

        this.items.clear();
        this.items.addAll(newItems);
        this.botName = botName.substring(0, Math.min(botName.length(), Emulator.getConfig().getInt("hotel.wired.message.max_length", 100)));
        this.setDelay(delay);

        return true;
    }

    @Override
    public WiredEffectType getType() {
        return type;
    }

    @Override
    public boolean execute(RoomUnit roomUnit, Room room, Object[] stuff) {
        if (this.items.isEmpty())
            return false;

        List<Bot> bots = room.getBots(this.botName);

        if (bots.size() != 1) {
            return false;
        }

        Bot bot = bots.get(0);

        int i = Emulator.getRandom().nextInt(this.items.size()) + 1;
        int j = 1;

        for (HabboItem item : this.items) {
            if (item.getRoomId() != 0 && item.getRoomId() == bot.getRoom().getId()) {
                if (i == j) {
                    teleportUnitToTile(bot.getRoomUnit(), room.getLayout().getTile(item.getX(), item.getY()));
                    return true;
                } else {
                    j++;
                }
            }
        }

        return true;
    }

    @Override
    public String getWiredData() {
        ArrayList<Integer> itemIds = new ArrayList<>();

        if (this.items != null) {
            for (HabboItem item : this.items) {
                if (item.getRoomId() != 0) {
                    itemIds.add(item.getId());
                }
            }
        }

        return WiredHandler.getGsonBuilder().create().toJson(new JsonData(this.botName, itemIds, this.getDelay()));
    }

    @Override
    public void loadWiredData(ResultSet set, Room room) throws SQLException {
        this.items = new THashSet<>();

        String wiredData = set.getString("wired_data");

        if(wiredData.startsWith("{")) {
            JsonData data = WiredHandler.getGsonBuilder().create().fromJson(wiredData, JsonData.class);
            this.setDelay(data.delay);
            this.botName = data.bot_name;

            for(int itemId : data.items) {
                HabboItem item = room.getHabboItem(itemId);

                if (item != null)
                    this.items.add(item);
            }
        }
        else {
            String[] wiredDataSplit = set.getString("wired_data").split("\t");

            if (wiredDataSplit.length >= 2) {
                this.setDelay(Integer.valueOf(wiredDataSplit[0]));
                String[] data = wiredDataSplit[1].split(";");

                if (data.length > 1) {
                    this.botName = data[0];

                    for (int i = 1; i < data.length; i++) {
                        HabboItem item = room.getHabboItem(Integer.valueOf(data[i]));

                        if (item != null)
                            this.items.add(item);
                    }
                }
            }

            this.needsUpdate(true);
        }
    }

    @Override
    public void onPickUp() {
        this.botName = "";
        this.items.clear();
        this.setDelay(0);
    }

    static class JsonData {
        String bot_name;
        List<Integer> items;
        int delay;

        public JsonData(String bot_name, List<Integer> items, int delay) {
            this.bot_name = bot_name;
            this.items = items;
            this.delay = delay;
        }
    }
}
