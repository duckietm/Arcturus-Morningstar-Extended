package com.eu.habbo.habbohotel.items.interactions.wired.effects;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.gameclients.GameClient;
import com.eu.habbo.habbohotel.items.Item;
import com.eu.habbo.habbohotel.items.interactions.InteractionWiredEffect;
import com.eu.habbo.habbohotel.items.interactions.wired.WiredSettings;
import com.eu.habbo.habbohotel.rooms.*;
import com.eu.habbo.habbohotel.users.Habbo;
import com.eu.habbo.habbohotel.users.HabboItem;
import com.eu.habbo.habbohotel.wired.WiredEffectType;
import com.eu.habbo.habbohotel.wired.WiredHandler;
import com.eu.habbo.messages.ClientMessage;
import com.eu.habbo.messages.ServerMessage;
import com.eu.habbo.messages.incoming.wired.WiredSaveException;
import com.eu.habbo.messages.outgoing.rooms.items.FloorItemOnRollerComposer;
import com.eu.habbo.threading.runnables.WiredCollissionRunnable;
import gnu.trove.map.hash.THashMap;
import gnu.trove.set.hash.THashSet;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Wired effect: move to closest user
 * Confirmed as working exactly like Habbo.com 03/05/2019 04:00
 *
 * @author Beny.
 */
public class WiredEffectMoveFurniTowards extends InteractionWiredEffect {
    public static final WiredEffectType type = WiredEffectType.CHASE;

    private THashSet<HabboItem> items;

    private THashMap<Integer, RoomUserRotation> lastDirections;


    public WiredEffectMoveFurniTowards(ResultSet set, Item baseItem) throws SQLException {
        super(set, baseItem);
        this.items = new THashSet<>();
        this.lastDirections = new THashMap<>();
    }

    public WiredEffectMoveFurniTowards(int id, int userId, Item item, String extradata, int limitedStack, int limitedSells) {
        super(id, userId, item, extradata, limitedStack, limitedSells);
        this.items = new THashSet<>();
        this.lastDirections = new THashMap<>();
    }

    public List<RoomUserRotation> getAvailableDirections(HabboItem item, Room room) {
        List<RoomUserRotation> availableDirections = new ArrayList<>();
        RoomLayout layout = room.getLayout();

        RoomTile currentTile = layout.getTile(item.getX(), item.getY());

        RoomUserRotation[] rotations = new RoomUserRotation[]{RoomUserRotation.NORTH, RoomUserRotation.EAST, RoomUserRotation.SOUTH, RoomUserRotation.WEST};

        for (RoomUserRotation rot : rotations) {
            RoomTile tile = layout.getTileInFront(currentTile, rot.getValue());

            if (tile == null || tile.state == RoomTileState.BLOCKED || tile.state == RoomTileState.INVALID)
                continue;

            if (!layout.tileExists(tile.x, tile.y))
                continue;

            if (room.furnitureFitsAt(tile, item, item.getRotation()) == FurnitureMovementError.INVALID_MOVE)
                continue;

            HabboItem topItem = room.getTopItemAt(tile.x, tile.y);
            if (topItem != null && !topItem.getBaseItem().allowStack())
                continue;

            if (tile.getAllowStack()) {
                availableDirections.add(rot);
            }
        }

        return availableDirections;
    }

    @Override
    public boolean execute(RoomUnit roomUnit, Room room, Object[] stuff) {

        THashSet<HabboItem> items = new THashSet<>();

        for (HabboItem item : this.items) {
            if (Emulator.getGameEnvironment().getRoomManager().getRoom(this.getRoomId()).getHabboItem(item.getId()) == null)
                items.add(item);
        }

        for (HabboItem item : items) {
            this.items.remove(item);
        }

        for (HabboItem item : this.items) {

            if (item == null)
                continue;

            // direction the furni will move in
            RoomUserRotation moveDirection = null;
            RoomUserRotation lastDirection = lastDirections.get(item.getId());

            // 1. Check if any user is within 3 tiles from the item
            RoomUnit target = null; // closest found user
            RoomLayout layout = room.getLayout();
            boolean collided = false;

            if (layout == null) {
                break;
            }

            for (int i = 0; i < 3; i++) {
                if (target != null)
                    break;

                RoomUserRotation[] rotations = new RoomUserRotation[]{RoomUserRotation.NORTH, RoomUserRotation.EAST, RoomUserRotation.SOUTH, RoomUserRotation.WEST};

                for (RoomUserRotation rot : rotations) {
                    RoomTile startTile = layout.getTile(item.getX(), item.getY());

                    for (int ii = 0; ii <= i; ii++) {
                        if (startTile == null)
                            break;

                        startTile = layout.getTileInFront(startTile, rot.getValue());
                    }

                    if (startTile != null && layout.tileExists(startTile.x, startTile.y)) {
                        Collection<RoomUnit> roomUnitsAtTile = room.getRoomUnitsAt(startTile);
                        if (roomUnitsAtTile.size() > 0) {
                            target = roomUnitsAtTile.iterator().next();
                            if (i == 0) { // i = 0 means right next to it
                                collided = true;
                                Emulator.getThreading().run(new WiredCollissionRunnable(target, room, new Object[]{item}));
                            }
                            break;
                        }
                    }
                }
            }

            if (collided)
                continue;

            if (target != null) {
                if (target.getX() == item.getX()) {
                    if (item.getY() < target.getY())
                        moveDirection = RoomUserRotation.SOUTH;
                    else
                        moveDirection = RoomUserRotation.NORTH;
                } else if (target.getY() == item.getY()) {
                    if (item.getX() < target.getX())
                        moveDirection = RoomUserRotation.EAST;
                    else
                        moveDirection = RoomUserRotation.WEST;
                } else if (target.getX() - item.getX() > target.getY() - item.getY()) {
                    if (target.getX() - item.getX() > 0)
                        moveDirection = RoomUserRotation.EAST;
                    else
                        moveDirection = RoomUserRotation.WEST;
                } else {
                    if (target.getY() - item.getY() > 0)
                        moveDirection = RoomUserRotation.SOUTH;
                    else
                        moveDirection = RoomUserRotation.NORTH;
                }
            }


            // 2. Get a random direction
            /*
            getAvailableDirections:
                0 available - don't move
                1 available - move in that direction
                2 available - if lastdirection = null move in random possible direction
                              else if direction[0] = lastdirection opposite, move in direction[1]
                              else move in direction[0]
                3+ available - move in random direction, but never the opposite
             */

            List<RoomUserRotation> availableDirections = this.getAvailableDirections(item, room);

            if (moveDirection != null && !availableDirections.contains(moveDirection))
                moveDirection = null;

            if (moveDirection == null) {
                if (availableDirections.size() == 0) {
                    continue;
                } else if (availableDirections.size() == 1) {
                    moveDirection = availableDirections.iterator().next();
                } else if (availableDirections.size() == 2) {
                    if (lastDirection == null) {
                        moveDirection = availableDirections.get(Emulator.getRandom().nextInt(availableDirections.size()));
                    } else {
                        RoomUserRotation oppositeLast = lastDirection.getOpposite();

                        if (availableDirections.get(0) == oppositeLast) {
                            moveDirection = availableDirections.get(1);
                        } else {
                            moveDirection = availableDirections.get(0);
                        }
                    }
                } else {
                    if (lastDirection != null) {
                        RoomUserRotation opposite = lastDirection.getOpposite();
                        availableDirections.remove(opposite);
                    }
                    moveDirection = availableDirections.get(Emulator.getRandom().nextInt(availableDirections.size()));
                }
            }

            RoomTile newTile = room.getLayout().getTileInFront(room.getLayout().getTile(item.getX(), item.getY()), moveDirection.getValue());

            RoomTile oldLocation = room.getLayout().getTile(item.getX(), item.getY());
            double oldZ = item.getZ();

            if(newTile != null) {
                lastDirections.put(item.getId(), moveDirection);
                if(newTile.state != RoomTileState.INVALID && newTile != oldLocation && room.furnitureFitsAt(newTile, item, item.getRotation(), true) == FurnitureMovementError.NONE) {
                    if (room.moveFurniTo(item, newTile, item.getRotation(), null, false) == FurnitureMovementError.NONE) {
                        room.sendComposer(new FloorItemOnRollerComposer(item, null, oldLocation, oldZ, newTile, item.getZ(), 0, room).compose());
                    }
                }
            }
        }

        return true;
    }

    @Override
    public String getWiredData() {
        return WiredHandler.getGsonBuilder().create().toJson(new JsonData(
                this.getDelay(),
                this.items.stream().map(HabboItem::getId).collect(Collectors.toList())
        ));
    }

    @Override
    public void loadWiredData(ResultSet set, Room room) throws SQLException {
        this.items = new THashSet<>();
        String wiredData = set.getString("wired_data");

        if (wiredData.startsWith("{")) {
            JsonData data = WiredHandler.getGsonBuilder().create().fromJson(wiredData, JsonData.class);
            this.setDelay(data.delay);

            for (Integer id: data.itemIds) {
                HabboItem item = room.getHabboItem(id);
                if (item != null) {
                    this.items.add(item);
                }
            }
        } else {
            String[] wiredDataOld = wiredData.split("\t");

            if (wiredDataOld.length >= 1) {
                this.setDelay(Integer.parseInt(wiredDataOld[0]));
            }
            if (wiredDataOld.length == 2) {
                if (wiredDataOld[1].contains(";")) {
                    for (String s : wiredDataOld[1].split(";")) {
                        HabboItem item = room.getHabboItem(Integer.parseInt(s));

                        if (item != null)
                            this.items.add(item);
                    }
                }
            }
        }
    }

    @Override
    public void onPickUp() {
        this.items.clear();
        this.setDelay(0);
    }

    @Override
    public WiredEffectType getType() {
        return type;
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
        message.appendString("");
        message.appendInt(0);
        message.appendInt(0);
        message.appendInt(this.getType().code);
        message.appendInt(this.getDelay());
        message.appendInt(0);
    }

    @Override
    public boolean saveData(WiredSettings settings, GameClient gameClient) throws WiredSaveException {
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
        this.setDelay(delay);

        return true;
    }

    @Override
    protected long requiredCooldown() {
        return 495;
    }

    static class JsonData {
        int delay;
        List<Integer> itemIds;

        public JsonData(int delay, List<Integer> itemIds) {
            this.delay = delay;
            this.itemIds = itemIds;
        }
    }
}
