package com.eu.habbo.habbohotel.items.interactions.wired.effects;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.gameclients.GameClient;
import com.eu.habbo.habbohotel.items.Item;
import com.eu.habbo.habbohotel.items.interactions.InteractionWiredEffect;
import com.eu.habbo.habbohotel.items.interactions.InteractionWiredTrigger;
import com.eu.habbo.habbohotel.items.interactions.wired.WiredSettings;
import com.eu.habbo.habbohotel.rooms.Room;
import com.eu.habbo.habbohotel.rooms.RoomSpecialTypes;
import com.eu.habbo.habbohotel.rooms.RoomTile;
import com.eu.habbo.habbohotel.rooms.RoomUnit;
import com.eu.habbo.habbohotel.users.HabboItem;
import com.eu.habbo.habbohotel.wired.WiredEffectType;
import com.eu.habbo.habbohotel.wired.core.*;
import com.eu.habbo.habbohotel.wired.core.WiredEvent;
import com.eu.habbo.messages.ServerMessage;
import com.eu.habbo.messages.incoming.wired.WiredSaveException;
import gnu.trove.procedure.TObjectProcedure;
import gnu.trove.set.hash.THashSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;


public class WiredEffectSendSignal extends InteractionWiredEffect {
    private static final Logger LOGGER = LoggerFactory.getLogger(WiredEffectSendSignal.class);

    public static final WiredEffectType type = WiredEffectType.SEND_SIGNAL;

    private static final int MAX_SIGNAL_DEPTH = 10;

    private static final int ANTENNA_PICKED  = 0;
    private static final int ANTENNA_TRIGGER = 1;

    private static final int FORWARD_NONE    = 0;
    private static final int FORWARD_TRIGGER = 1;

    private THashSet<HabboItem> items;
    private int     antennaSource   = ANTENNA_PICKED;
    private int     furniForward    = FORWARD_NONE;
    private int     userForward     = FORWARD_NONE;
    private boolean signalPerFurni  = false;
    private boolean signalPerUser   = false;
    private int     channel         = 0;

    public WiredEffectSendSignal(ResultSet set, Item baseItem) throws SQLException {
        super(set, baseItem);
        this.items = new THashSet<>();
    }

    public WiredEffectSendSignal(int id, int userId, Item item, String extradata, int limitedStack, int limitedSells) {
        super(id, userId, item, extradata, limitedStack, limitedSells);
        this.items = new THashSet<>();
    }

    @Override
    public void execute(WiredContext ctx) {
        Room room = ctx.room();
        if (room == null) return;

        LOGGER.debug("[SendSignal] execute() called, itemId={}, antennaSource={}, pickedItems={}", this.getId(), antennaSource, this.items.size());

        int currentDepth = ctx.event().getCallStackDepth();
        if (currentDepth >= MAX_SIGNAL_DEPTH) {
            LOGGER.debug("[SendSignal] Max signal depth reached ({}), aborting", currentDepth);
            return;
        }

        Collection<HabboItem> antennas;
        if (antennaSource == ANTENNA_TRIGGER) {
            antennas = ctx.sourceItem()
                    .map(Collections::<HabboItem>singleton)
                    .orElse(Collections.emptySet());
        } else {
            antennas = ctx.targets().isItemsModifiedBySelector()
                    ? new ArrayList<>(ctx.targets().items())
                    : new ArrayList<>(this.items);
        }

        if (antennas.isEmpty()) {
            LOGGER.debug("[SendSignal] No antennas resolved, aborting. antennaSource={}, selectorModified={}", antennaSource, ctx.targets().isItemsModifiedBySelector());
            return;
        }
        LOGGER.debug("[SendSignal] Resolved {} antenna(s), firing signals", antennas.size());

        RoomUnit forwardedUser = null;
        if (userForward == FORWARD_TRIGGER) {
            forwardedUser = ctx.actor().orElse(null);
        }

        HabboItem forwardedFurni = null;
        if (furniForward == FORWARD_TRIGGER) {
            forwardedFurni = ctx.sourceItem().orElse(null);
        }

        Set<String> visitedTiles = new HashSet<>();
        List<RoomTile> antennaTiles = new ArrayList<>();
        for (HabboItem antenna : antennas) {
            if (antenna == null) continue;
            String key = antenna.getX() + "," + antenna.getY();
            if (visitedTiles.add(key)) {
                RoomTile tile = room.getLayout().getTile(antenna.getX(), antenna.getY());
                if (tile != null) {
                    antennaTiles.add(tile);
                }
            }
        }

        int nextDepth = currentDepth + 1;

        if (signalPerFurni || signalPerUser) {
            if (signalPerFurni) {
                for (RoomTile tile : antennaTiles) {
                    fireSignalAtTile(room, tile, forwardedUser, forwardedFurni, nextDepth);
                }
            }
            if (signalPerUser && ctx.targets().hasUsers()) {
                for (RoomUnit user : ctx.targets().users()) {
                    for (RoomTile tile : antennaTiles) {
                        fireSignalAtTile(room, tile, user, forwardedFurni, nextDepth);
                    }
                }
            } else if (!signalPerFurni) {
                for (RoomTile tile : antennaTiles) {
                    fireSignalAtTile(room, tile, forwardedUser, forwardedFurni, nextDepth);
                }
            }
        } else {
            for (RoomTile tile : antennaTiles) {
                fireSignalAtTile(room, tile, forwardedUser, forwardedFurni, nextDepth);
            }
        }
    }

    private void fireSignalAtTile(Room room, RoomTile tile, RoomUnit actor, HabboItem sourceItem, int depth) {
        LOGGER.debug("[SendSignal] fireSignalAtTile: tile={},{} depth={} channel={} actor={} sourceItem={}", tile.x, tile.y, depth, channel, actor != null ? actor.getId() : "null", sourceItem != null ? sourceItem.getId() : "null");

        WiredEvent.Builder builder = WiredEvent.builder(WiredEvent.Type.SIGNAL_RECEIVED, room)
                .tile(tile)
                .callStackDepth(depth)
                .signalChannel(this.channel)
                .triggeredByEffect(true);

        if (actor != null) builder.actor(actor);
        if (sourceItem != null) builder.sourceItem(sourceItem);

        boolean result = WiredManager.handleEvent(builder.build());
        LOGGER.debug("[SendSignal] handleEvent returned: {}", result);
    }

    @Override
    public void serializeWiredData(ServerMessage message, Room room) {
        List<HabboItem> itemsSnapshot = new ArrayList<>(this.items);

        itemsSnapshot.removeIf(item ->
                item.getRoomId() != this.getRoomId() || room.getHabboItem(item.getId()) == null);
        this.items.retainAll(itemsSnapshot);

        message.appendBoolean(false);
        message.appendInt(WiredManager.MAXIMUM_FURNI_SELECTION);
        message.appendInt(itemsSnapshot.size());
        for (HabboItem item : itemsSnapshot) {
            message.appendInt(item.getId());
        }
        message.appendInt(this.getBaseItem().getSpriteId());
        message.appendInt(this.getId());
        message.appendString("");

        message.appendInt(6);
        message.appendInt(antennaSource);
        message.appendInt(furniForward);
        message.appendInt(userForward);
        message.appendInt(signalPerFurni ? 1 : 0);
        message.appendInt(signalPerUser ? 1 : 0);
        message.appendInt(channel);

        message.appendInt(0);
        message.appendInt(this.getType().code);
        message.appendInt(this.getDelay());

        if (this.requiresTriggeringUser()) {
            List<Integer> invalidTriggers = new ArrayList<>();
            room.getRoomSpecialTypes().getTriggers(this.getX(), this.getY()).forEach(new TObjectProcedure<InteractionWiredTrigger>() {
                @Override
                public boolean execute(InteractionWiredTrigger object) {
                    if (!object.isTriggeredByRoomUnit()) {
                        invalidTriggers.add(object.getBaseItem().getSpriteId());
                    }
                    return true;
                }
            });
            message.appendInt(invalidTriggers.size());
            for (Integer i : invalidTriggers) {
                message.appendInt(i);
            }
        } else {
            message.appendInt(0);
        }
    }

    @Override
    public boolean saveData(WiredSettings settings, GameClient gameClient) throws WiredSaveException {
        int itemsCount = settings.getFurniIds().length;
        if (itemsCount > Emulator.getConfig().getInt("hotel.wired.furni.selection.count")) {
            throw new WiredSaveException("Too many furni selected");
        }

        List<HabboItem> newItems = new ArrayList<>();
        Room room = Emulator.getGameEnvironment().getRoomManager().getRoom(this.getRoomId());
        for (int i = 0; i < itemsCount; i++) {
            int itemId = settings.getFurniIds()[i];
            HabboItem it = room.getHabboItem(itemId);
            if (it == null) throw new WiredSaveException(String.format("Item %s not found", itemId));
            newItems.add(it);
        }

        if (room != null && room.getRoomSpecialTypes() != null) {
            for (HabboItem receiver : newItems) {
                int count = room.getRoomSpecialTypes().countSendersTargetingReceiver(receiver.getId(), this);
                if (count >= RoomSpecialTypes.MAX_SENDERS_PER_RECEIVER) {
                    throw new WiredSaveException("Maximum of " + RoomSpecialTypes.MAX_SENDERS_PER_RECEIVER + " senders per receiver reached");
                }
            }
        }

        int delay = settings.getDelay();
        if (delay > Emulator.getConfig().getInt("hotel.wired.max_delay", 20)) {
            throw new WiredSaveException("Delay too long");
        }

        int[] params = settings.getIntParams();
        this.antennaSource  = params.length > 0 ? params[0] : ANTENNA_PICKED;
        this.furniForward   = params.length > 1 ? params[1] : FORWARD_NONE;
        this.userForward    = params.length > 2 ? params[2] : FORWARD_NONE;
        this.signalPerFurni = params.length > 3 && params[3] == 1;
        this.signalPerUser  = params.length > 4 && params[4] == 1;
        this.channel = params.length > 5 ? params[5] : 0;
        this.items.clear();
        this.items.addAll(newItems);
        this.setDelay(delay);

        LOGGER.debug("[SendSignal] saveData: antennaSource={}, furniForward={}, userForward={}, signalPerFurni={}, signalPerUser={}, channel={}, items={}",
                antennaSource, furniForward, userForward, signalPerFurni, signalPerUser, channel, items.size());

        return true;
    }

    @Deprecated
    @Override
    public boolean execute(RoomUnit roomUnit, Room room, Object[] stuff) {
        return false;
    }

    @Override
    public String getWiredData() {
        List<HabboItem> itemsSnapshot = new ArrayList<>(this.items);
        return WiredManager.getGson().toJson(new JsonData(
                this.getDelay(),
                itemsSnapshot.stream().map(HabboItem::getId).collect(Collectors.toList()),
                antennaSource, furniForward, userForward, signalPerFurni, signalPerUser, channel
        ));
    }

    @Override
    public void loadWiredData(ResultSet set, Room room) throws SQLException {
        this.items = new THashSet<>();
        String wiredData = set.getString("wired_data");

        if (wiredData != null && wiredData.startsWith("{")) {
            JsonData data = WiredManager.getGson().fromJson(wiredData, JsonData.class);
            this.setDelay(data.delay);
            this.antennaSource  = data.antennaSource;
            this.furniForward   = data.furniForward;
            this.userForward    = data.userForward;
            this.signalPerFurni = data.signalPerFurni;
            this.signalPerUser  = data.signalPerUser;
            this.channel        = data.channel;
            if (data.itemIds != null) {
                for (Integer id : data.itemIds) {
                    HabboItem item = room.getHabboItem(id);
                    if (item != null) this.items.add(item);
                }
            }
        }
    }

    @Override
    public void onPickUp() {
        this.items.clear();
        this.antennaSource  = ANTENNA_PICKED;
        this.furniForward   = FORWARD_NONE;
        this.userForward    = FORWARD_NONE;
        this.signalPerFurni = false;
        this.signalPerUser  = false;
        this.channel        = 0;
        this.setDelay(0);
    }

    @Override
    public WiredEffectType getType() {
        return type;
    }

    public int getChannel() {
        return channel;
    }

    public boolean hasPickedItem(int itemId) {
        try {
            for (HabboItem item : new ArrayList<>(this.items)) {
                if (item != null && item.getId() == itemId) return true;
            }
        } catch (Exception e) {
        }
        return false;
    }

    @Override
    protected long requiredCooldown() {
        return COOLDOWN_TRIGGER_STACKS;
    }

    static class JsonData {
        int delay;
        List<Integer> itemIds;
        int antennaSource;
        int furniForward;
        int userForward;
        boolean signalPerFurni;
        boolean signalPerUser;
        int channel;

        public JsonData(int delay, List<Integer> itemIds, int antennaSource, int furniForward,
                        int userForward, boolean signalPerFurni, boolean signalPerUser, int channel) {
            this.delay          = delay;
            this.itemIds        = itemIds;
            this.antennaSource  = antennaSource;
            this.furniForward   = furniForward;
            this.userForward    = userForward;
            this.signalPerFurni = signalPerFurni;
            this.signalPerUser  = signalPerUser;
            this.channel        = channel;
        }
    }
}
