package com.eu.habbo.habbohotel.items.interactions.wired.effects;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.gameclients.GameClient;
import com.eu.habbo.habbohotel.items.Item;
import com.eu.habbo.habbohotel.items.interactions.InteractionWiredEffect;
import com.eu.habbo.habbohotel.items.interactions.wired.WiredSettings;
import com.eu.habbo.habbohotel.items.interactions.wired.interfaces.InteractionWiredMatchFurniSettings;
import com.eu.habbo.habbohotel.rooms.*;
import com.eu.habbo.habbohotel.users.HabboItem;
import com.eu.habbo.habbohotel.wired.WiredEffectType;
import com.eu.habbo.habbohotel.wired.WiredHandler;
import com.eu.habbo.habbohotel.wired.WiredMatchFurniSetting;
import com.eu.habbo.messages.ClientMessage;
import com.eu.habbo.messages.ServerMessage;
import com.eu.habbo.messages.incoming.wired.WiredSaveException;
import com.eu.habbo.messages.outgoing.rooms.items.FloorItemOnRollerComposer;
import gnu.trove.set.hash.THashSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class WiredEffectMatchFurni extends InteractionWiredEffect implements InteractionWiredMatchFurniSettings {
    private static final Logger LOGGER = LoggerFactory.getLogger(WiredEffectMatchFurni.class);

    private static final WiredEffectType type = WiredEffectType.MATCH_SSHOT;
    public boolean checkForWiredResetPermission = true;
    private THashSet<WiredMatchFurniSetting> settings;
    private boolean state = false;
    private boolean direction = false;
    private boolean position = false;

    public WiredEffectMatchFurni(ResultSet set, Item baseItem) throws SQLException {
        super(set, baseItem);
        this.settings = new THashSet<>(0);
    }

    public WiredEffectMatchFurni(int id, int userId, Item item, String extradata, int limitedStack, int limitedSells) {
        super(id, userId, item, extradata, limitedStack, limitedSells);
        this.settings = new THashSet<>(0);
    }

    @Override
    public boolean execute(RoomUnit roomUnit, Room room, Object[] stuff) {

        if(this.settings.isEmpty())
            return true;

        for (WiredMatchFurniSetting setting : this.settings) {
            HabboItem item = room.getHabboItem(setting.item_id);
            if (item != null) {
                if (this.state && (this.checkForWiredResetPermission && item.allowWiredResetState())) {
                    if (!setting.state.equals(" ") && !item.getExtradata().equals(setting.state)) {
                        item.setExtradata(setting.state);
                        room.updateItemState(item);
                    }
                }

                RoomTile oldLocation = room.getLayout().getTile(item.getX(), item.getY());
                double oldZ = item.getZ();

                if(this.direction && !this.position) {
                    if(item.getRotation() != setting.rotation && room.furnitureFitsAt(oldLocation, item, setting.rotation, false) == FurnitureMovementError.NONE) {
                        room.moveFurniTo(item, oldLocation, setting.rotation, null, true);
                    }
                }
                else if(this.position) {
                    boolean slideAnimation = !this.direction || item.getRotation() == setting.rotation;
                    RoomTile newLocation = room.getLayout().getTile((short) setting.x, (short) setting.y);
                    int newRotation = this.direction ? setting.rotation : item.getRotation();

                    if(newLocation != null && newLocation.state != RoomTileState.INVALID && (newLocation != oldLocation || newRotation != item.getRotation()) && room.furnitureFitsAt(newLocation, item, newRotation, true) == FurnitureMovementError.NONE) {
                        if(room.moveFurniTo(item, newLocation, newRotation, null, !slideAnimation) == FurnitureMovementError.NONE) {
                            if(slideAnimation) {
                                room.sendComposer(new FloorItemOnRollerComposer(item, null, oldLocation, oldZ, newLocation, item.getZ(), 0, room).compose());
                            }
                        }
                    }
                }

            }
        }

        return true;
    }

    @Override
    public String getWiredData() {
        this.refresh();
        return WiredHandler.getGsonBuilder().create().toJson(new JsonData(this.state, this.direction, this.position, new ArrayList<WiredMatchFurniSetting>(this.settings), this.getDelay()));
    }

    @Override
    public void loadWiredData(ResultSet set, Room room) throws SQLException {
        String wiredData = set.getString("wired_data");

        if(wiredData.startsWith("{")) {
            JsonData data = WiredHandler.getGsonBuilder().create().fromJson(wiredData, JsonData.class);
            this.setDelay(data.delay);
            this.state = data.state;
            this.direction = data.direction;
            this.position = data.position;
            this.settings.clear();
            this.settings.addAll(data.items);
        }
        else {
            String[] data = set.getString("wired_data").split(":");

            int itemCount = Integer.parseInt(data[0]);

            String[] items = data[1].split(Pattern.quote(";"));

            for (int i = 0; i < items.length; i++) {
                try {

                    String[] stuff = items[i].split(Pattern.quote("-"));

                    if (stuff.length >= 5) {
                        this.settings.add(new WiredMatchFurniSetting(Integer.parseInt(stuff[0]), stuff[1], Integer.parseInt(stuff[2]), Integer.parseInt(stuff[3]), Integer.parseInt(stuff[4])));
                    }

                } catch (Exception e) {
                    LOGGER.error("Caught exception", e);
                }
            }

            this.state = data[2].equals("1");
            this.direction = data[3].equals("1");
            this.position = data[4].equals("1");
            this.setDelay(Integer.parseInt(data[5]));
            this.needsUpdate(true);
        }
    }

    @Override
    public void onPickUp() {
        this.settings.clear();
        this.state = false;
        this.direction = false;
        this.position = false;
        this.setDelay(0);
    }

    @Override
    public WiredEffectType getType() {
        return type;
    }

    @Override
    public void serializeWiredData(ServerMessage message, Room room) {
        this.refresh();

        message.appendBoolean(false);
        message.appendInt(WiredHandler.MAXIMUM_FURNI_SELECTION);
        message.appendInt(this.settings.size());

        for (WiredMatchFurniSetting item : this.settings)
            message.appendInt(item.item_id);

        message.appendInt(this.getBaseItem().getSpriteId());
        message.appendInt(this.getId());
        message.appendString("");
        message.appendInt(3);
        message.appendInt(this.state ? 1 : 0);
        message.appendInt(this.direction ? 1 : 0);
        message.appendInt(this.position ? 1 : 0);
        message.appendInt(0);
        message.appendInt(this.getType().code);
        message.appendInt(this.getDelay());
        message.appendInt(0);
    }

    @Override
    public boolean saveData(WiredSettings settings, GameClient gameClient) throws WiredSaveException {
        if(settings.getIntParams().length < 3) throw new WiredSaveException("Invalid data");
        boolean setState = settings.getIntParams()[0] == 1;
        boolean setDirection = settings.getIntParams()[1] == 1;
        boolean setPosition = settings.getIntParams()[2] == 1;

        Room room = Emulator.getGameEnvironment().getRoomManager().getRoom(this.getRoomId());

        if (room == null)
            throw new WiredSaveException("Trying to save wired in unloaded room");

        int itemsCount = settings.getFurniIds().length;

        if(itemsCount > Emulator.getConfig().getInt("hotel.wired.furni.selection.count")) {
            throw new WiredSaveException("Too many furni selected");
        }

        List<WiredMatchFurniSetting> newSettings = new ArrayList<>();

        for (int i = 0; i < itemsCount; i++) {
            int itemId = settings.getFurniIds()[i];
            HabboItem it = Emulator.getGameEnvironment().getRoomManager().getRoom(this.getRoomId()).getHabboItem(itemId);

            if(it == null)
                throw new WiredSaveException(String.format("Item %s not found", itemId));

            newSettings.add(new WiredMatchFurniSetting(it.getId(), this.checkForWiredResetPermission && it.allowWiredResetState() ? it.getExtradata() : " ", it.getRotation(), it.getX(), it.getY()));
        }

        int delay = settings.getDelay();

        if(delay > Emulator.getConfig().getInt("hotel.wired.max_delay", 20))
            throw new WiredSaveException("Delay too long");

        this.state = setState;
        this.direction = setDirection;
        this.position = setPosition;
        this.settings.clear();
        this.settings.addAll(newSettings);
        this.setDelay(delay);

        return true;
    }

    private void refresh() {
        Room room = Emulator.getGameEnvironment().getRoomManager().getRoom(this.getRoomId());

        if (room != null && room.isLoaded()) {
            THashSet<WiredMatchFurniSetting> remove = new THashSet<>();

            for (WiredMatchFurniSetting setting : this.settings) {
                HabboItem item = room.getHabboItem(setting.item_id);
                if (item == null) {
                    remove.add(setting);
                }
            }

            for (WiredMatchFurniSetting setting : remove) {
                this.settings.remove(setting);
            }
        }
    }

    @Override
    public THashSet<WiredMatchFurniSetting> getMatchFurniSettings() {
        return this.settings;
    }

    @Override
    public boolean shouldMatchState() {
        return this.state;
    }

    @Override
    public boolean shouldMatchRotation() {
        return this.direction;
    }

    @Override
    public boolean shouldMatchPosition() {
        return this.position;
    }

    static class JsonData {
        boolean state;
        boolean direction;
        boolean position;
        List<WiredMatchFurniSetting> items;
        int delay;

        public JsonData(boolean state, boolean direction, boolean position, List<WiredMatchFurniSetting> items, int delay) {
            this.state = state;
            this.direction = direction;
            this.position = position;
            this.items = items;
            this.delay = delay;
        }
    }
}
