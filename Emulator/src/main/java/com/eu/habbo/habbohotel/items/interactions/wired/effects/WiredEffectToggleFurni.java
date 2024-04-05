package com.eu.habbo.habbohotel.items.interactions.wired.effects;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.gameclients.GameClient;
import com.eu.habbo.habbohotel.items.Item;
import com.eu.habbo.habbohotel.items.interactions.*;
import com.eu.habbo.habbohotel.items.interactions.games.InteractionGameGate;
import com.eu.habbo.habbohotel.items.interactions.games.InteractionGameScoreboard;
import com.eu.habbo.habbohotel.items.interactions.games.InteractionGameTeamItem;
import com.eu.habbo.habbohotel.items.interactions.games.InteractionGameTimer;
import com.eu.habbo.habbohotel.items.interactions.games.battlebanzai.InteractionBattleBanzaiPuck;
import com.eu.habbo.habbohotel.items.interactions.games.battlebanzai.InteractionBattleBanzaiTeleporter;
import com.eu.habbo.habbohotel.items.interactions.games.battlebanzai.InteractionBattleBanzaiTile;
import com.eu.habbo.habbohotel.items.interactions.games.freeze.InteractionFreezeBlock;
import com.eu.habbo.habbohotel.items.interactions.games.freeze.InteractionFreezeExitTile;
import com.eu.habbo.habbohotel.items.interactions.games.freeze.InteractionFreezeTile;
import com.eu.habbo.habbohotel.items.interactions.games.tag.InteractionTagField;
import com.eu.habbo.habbohotel.items.interactions.games.tag.InteractionTagPole;
import com.eu.habbo.habbohotel.items.interactions.pets.*;
import com.eu.habbo.habbohotel.items.interactions.wired.WiredSettings;
import com.eu.habbo.habbohotel.rooms.Room;
import com.eu.habbo.habbohotel.rooms.RoomUnit;
import com.eu.habbo.habbohotel.users.Habbo;
import com.eu.habbo.habbohotel.users.HabboItem;
import com.eu.habbo.habbohotel.wired.WiredEffectType;
import com.eu.habbo.habbohotel.wired.WiredHandler;
import com.eu.habbo.messages.ClientMessage;
import com.eu.habbo.messages.ServerMessage;
import com.eu.habbo.messages.incoming.wired.WiredSaveException;
import gnu.trove.procedure.TObjectProcedure;
import gnu.trove.set.hash.THashSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class WiredEffectToggleFurni extends InteractionWiredEffect {
    private static final Logger LOGGER = LoggerFactory.getLogger(WiredEffectToggleFurni.class);

    public static final WiredEffectType type = WiredEffectType.TOGGLE_STATE;

    private final THashSet<HabboItem> items;

    private static final List<Class<? extends HabboItem>> FORBIDDEN_TYPES = new ArrayList<Class<? extends HabboItem>>() {
        {
            this.add(InteractionWired.class);
            this.add(InteractionTeleport.class);
            this.add(InteractionPushable.class);
            this.add(InteractionTagPole.class);
            this.add(InteractionTagField.class);
            this.add(InteractionCrackable.class);
            this.add(InteractionGameScoreboard.class);
            this.add(InteractionGameGate.class);
            this.add(InteractionFreezeTile.class);
            this.add(InteractionFreezeBlock.class);
            this.add(InteractionFreezeExitTile.class);
            this.add(InteractionBattleBanzaiTeleporter.class);
            this.add(InteractionBattleBanzaiTile.class);
            this.add(InteractionMonsterPlantSeed.class);
            this.add(InteractionPetBreedingNest.class);
            this.add(InteractionPetDrink.class);
            this.add(InteractionPetFood.class);
            this.add(InteractionPetToy.class);
            this.add(InteractionBadgeDisplay.class);
            this.add(InteractionClothing.class);
            this.add(InteractionVendingMachine.class);
            this.add(InteractionGift.class);
            this.add(InteractionPressurePlate.class);
            this.add(InteractionMannequin.class);
            this.add(InteractionGymEquipment.class);
            this.add(InteractionHopper.class);
            this.add(InteractionObstacle.class);
            this.add(InteractionOneWayGate.class);
            this.add(InteractionPuzzleBox.class);
            this.add(InteractionRoller.class);
            this.add(InteractionSwitch.class);
            this.add(InteractionTent.class);
            this.add(InteractionTrap.class);
            this.add(InteractionTrophy.class);
            this.add(InteractionWater.class);
        }
    };

    public WiredEffectToggleFurni(ResultSet set, Item baseItem) throws SQLException {
        super(set, baseItem);
        this.items = new THashSet<>();
    }

    public WiredEffectToggleFurni(int id, int userId, Item item, String extradata, int limitedStack, int limitedSells) {
        super(id, userId, item, extradata, limitedStack, limitedSells);
        this.items = new THashSet<>();
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
        for (HabboItem item : this.items) {
            message.appendInt(item.getId());
        }
        message.appendInt(this.getBaseItem().getSpriteId());
        message.appendInt(this.getId());
        message.appendString("");
        message.appendInt(0);
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
    public boolean execute(RoomUnit roomUnit, Room room, Object[] stuff) {
        Habbo habbo = room.getHabbo(roomUnit);

        HabboItem triggerItem = null;

        THashSet<HabboItem> itemsToRemove = new THashSet<>();
        for (HabboItem item : this.items) {
            if (item == null || item.getRoomId() == 0 || FORBIDDEN_TYPES.stream().anyMatch(a -> a.isAssignableFrom(item.getClass()))) {
                itemsToRemove.add(item);
                continue;
            }

            try {
                if (item.getBaseItem().getStateCount() > 1 || item instanceof InteractionGameTimer) {
                    int state = 0;
                    if (!item.getExtradata().isEmpty()) {
                        try {
                            state = Integer.parseInt(item.getExtradata()); // assumes that extradata is state, could be something else for trophies etc.
                        } catch (NumberFormatException ignored) {

                        }
                    }
                    item.onClick(habbo != null && !(item instanceof InteractionGameTimer) ? habbo.getClient() : null, room, new Object[]{state, this.getType()});
                }
            } catch (Exception e) {
                LOGGER.error("Caught exception", e);
            }
        }

        this.items.removeAll(itemsToRemove);

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
        this.items.clear();
        String wiredData = set.getString("wired_data");

        if (wiredData.startsWith("{")) {
            JsonData data = WiredHandler.getGsonBuilder().create().fromJson(wiredData, JsonData.class);
            this.setDelay(data.delay);
            for (Integer id: data.itemIds) {
                HabboItem item = room.getHabboItem(id);

                if (item instanceof InteractionFreezeBlock || item instanceof InteractionFreezeTile || item instanceof InteractionCrackable) {
                    continue;
                }

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

                        if (item instanceof InteractionFreezeBlock || item instanceof InteractionFreezeTile || item instanceof InteractionCrackable)
                            continue;

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

    static class JsonData {
        int delay;
        List<Integer> itemIds;

        public JsonData(int delay, List<Integer> itemIds) {
            this.delay = delay;
            this.itemIds = itemIds;
        }
    }
}
