package com.eu.habbo.habbohotel.items.interactions.wired.selector;

import com.eu.habbo.habbohotel.gameclients.GameClient;
import com.eu.habbo.habbohotel.items.Item;
import com.eu.habbo.habbohotel.items.interactions.InteractionWired;
import com.eu.habbo.habbohotel.items.interactions.InteractionWiredEffect;
import com.eu.habbo.habbohotel.items.interactions.wired.WiredSettings;
import com.eu.habbo.habbohotel.rooms.Room;
import com.eu.habbo.habbohotel.rooms.RoomUnit;
import com.eu.habbo.habbohotel.users.HabboItem;
import com.eu.habbo.habbohotel.wired.WiredEffectType;
import com.eu.habbo.habbohotel.wired.core.WiredContext;
import com.eu.habbo.habbohotel.wired.core.WiredManager;
import com.eu.habbo.messages.ServerMessage;
import com.eu.habbo.messages.incoming.wired.WiredSaveException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

public class WiredEffectFurniNeighborhood extends InteractionWiredEffect {
    private static final Logger LOGGER = LoggerFactory.getLogger(WiredEffectFurniNeighborhood.class);

    public static final WiredEffectType type = WiredEffectType.FURNI_NEIGHBORHOOD_SELECTOR;

    private static final int SOURCE_USER_TRIGGER  = 0;
    private static final int SOURCE_USER_SIGNAL   = 1;
    private static final int SOURCE_USER_CLICKED  = 2;
    private static final int SOURCE_FURNI_TRIGGER = 3;
    private static final int SOURCE_FURNI_PICKED  = 4;
    private static final int SOURCE_FURNI_SIGNAL  = 5;

    private static boolean isUserGroup(int src)  { return src <= SOURCE_USER_CLICKED;  }
    private static boolean isFurniGroup(int src) { return src >= SOURCE_FURNI_TRIGGER; }

    private static final int MAX_PICKED_FURNI = 20;
    private static final int MAX_TILE_OFFSETS  = 64;

    private int             sourceType      = SOURCE_USER_TRIGGER;
    private boolean         filterExisting  = false;
    private boolean         invert          = false;
    private List<int[]>     tileOffsets     = new ArrayList<>();
    private List<Integer>   pickedFurniIds  = new ArrayList<>();

    public WiredEffectFurniNeighborhood(ResultSet set, Item baseItem) throws SQLException {
        super(set, baseItem);
    }

    public WiredEffectFurniNeighborhood(int id, int userId, Item item, String extradata,
                                        int limitedStack, int limitedSells) {
        super(id, userId, item, extradata, limitedStack, limitedSells);
    }

    @Override
    public void execute(WiredContext ctx) {
        Room room = ctx.room();
        if (room == null || tileOffsets.isEmpty()) return;

        List<int[]> sourcePositions = resolveSourcePositions(ctx, room);
        if (sourcePositions.isEmpty()) return;

        int totalRaw = 0;
        int wiredSkipped = 0;
        Set<HabboItem> result = new LinkedHashSet<>();
        for (int[] src : sourcePositions) {
            LOGGER.info("[FurniNeighborhood] Source: ({},{}), offsets: {}", src[0], src[1], tileOffsets.size());
            for (int[] offset : tileOffsets) {
                int tx = src[0] + offset[0];
                int ty = src[1] + offset[1];
                for (HabboItem item : room.getItemsAt(tx, ty)) {
                    if (item == null) continue;
                    totalRaw++;
                    if (item instanceof InteractionWired) {
                        wiredSkipped++;
                        LOGGER.info("[FurniNeighborhood]   SKIP wired item {} ({}) at ({},{})",
                                item.getId(), item.getClass().getSimpleName(), tx, ty);
                    } else {
                        result.add(item);
                        LOGGER.info("[FurniNeighborhood]   KEEP item {} ({}) at ({},{})",
                                item.getId(), item.getClass().getSimpleName(), tx, ty);
                    }
                }
            }
        }
        LOGGER.info("[FurniNeighborhood] Raw={}, wiredSkipped={}, kept={}", totalRaw, wiredSkipped, result.size());

        if (filterExisting) {
            result.retainAll(ctx.targets().items());
        }

        if (invert) {
            Set<HabboItem> all = new LinkedHashSet<>();
            room.getFloorItems().forEach(item -> {
                if (!(item instanceof InteractionWired)) all.add(item);
            });
            all.removeAll(result);
            result = all;
        }

        // Always set the selector result — even if empty.
        // An empty result means no items matched the neighborhood, so downstream
        // effects should target nothing rather than falling back to the original targets.
        ctx.targets().setItems(result);
        LOGGER.info("[FurniNeighborhood] Set {} items as targets", result.size());
    }

    private List<int[]> resolveSourcePositions(WiredContext ctx, Room room) {

        if (isUserGroup(sourceType)) {
            // Prefer the event tile for user-based sources because during walk-on/walk-off
            // events the user's position (getX/getY) hasn't been updated yet (stale position).
            // The event tile correctly represents where the triggering action occurred.
            if (ctx.tile().isPresent()) {
                return Collections.singletonList(new int[]{ ctx.tile().get().x, ctx.tile().get().y });
            }
            List<int[]> positions = ctx.targets().users().stream()
                    .map(u -> new int[]{ u.getX(), u.getY() })
                    .collect(Collectors.toList());
            if (positions.isEmpty()) {
                ctx.actor().ifPresent(a -> positions.add(new int[]{ a.getX(), a.getY() }));
            }
            return positions;
        }

        switch (sourceType) {
            case SOURCE_FURNI_TRIGGER: {
                return ctx.sourceItem()
                        .map(i -> Collections.singletonList(new int[]{ i.getX(), i.getY() }))
                        .orElse(Collections.emptyList());
            }
            case SOURCE_FURNI_PICKED: {
                return pickedFurniIds.stream()
                        .map(room::getHabboItem)
                        .filter(Objects::nonNull)
                        .map(i -> new int[]{ i.getX(), i.getY() })
                        .collect(Collectors.toList());
            }
            case SOURCE_FURNI_SIGNAL: {
                return ctx.targets().items().stream()
                        .map(i -> new int[]{ i.getX(), i.getY() })
                        .collect(Collectors.toList());
            }
            default:
                return Collections.emptyList();
        }
    }

    @Override
    public boolean saveData(WiredSettings settings, GameClient gameClient) throws WiredSaveException {
        int[] params = settings.getIntParams();
        if (params == null || params.length < 1) {
            throw new WiredSaveException("wf_slc_furni_neighborhood: intParams must have at least 1 element");
        }

        this.sourceType     = params[0];
        this.filterExisting = params.length > 1 && params[1] == 1;
        this.invert         = params.length > 2 && params[2] == 1;

        this.tileOffsets = new ArrayList<>();
        if (params.length > 3) {
            int n = params[3];
            for (int i = 0; i < n && i < MAX_TILE_OFFSETS; i++) {
                int xi = 4 + i * 2;
                if (xi + 1 < params.length) {
                    tileOffsets.add(new int[]{ params[xi], params[xi + 1] });
                }
            }
        }

        this.pickedFurniIds = new ArrayList<>();
        if (this.sourceType == SOURCE_FURNI_PICKED && settings.getFurniIds() != null) {
            for (int id : settings.getFurniIds()) {
                if (pickedFurniIds.size() >= MAX_PICKED_FURNI) break;
                pickedFurniIds.add(id);
            }
        }

        this.setDelay(settings.getDelay());

        LOGGER.info("[FurniNeighborhood] saveData: sourceType={}, filterExisting={}, invert={}, offsets={}, pickedFurniIds={}",
                sourceType, filterExisting, invert, tileOffsets.size(), pickedFurniIds);
        for (int[] o : tileOffsets) {
            LOGGER.info("[FurniNeighborhood]   offset: ({}, {})", o[0], o[1]);
        }
        LOGGER.info("[FurniNeighborhood]   raw intParams (len={}): {}", params.length, java.util.Arrays.toString(params));

        return true;
    }

    @Override
    public void serializeWiredData(ServerMessage message, Room room) {
        boolean pickMode = (sourceType == SOURCE_FURNI_PICKED);

        message.appendBoolean(pickMode);
        message.appendInt(pickMode ? MAX_PICKED_FURNI : 0);

        if (pickMode && !pickedFurniIds.isEmpty()) {
            message.appendInt(pickedFurniIds.size());
            pickedFurniIds.forEach(message::appendInt);
        } else {
            message.appendInt(0);
        }

        message.appendInt(this.getBaseItem().getSpriteId());
        message.appendInt(this.getId());
        message.appendString("");

        int paramCount = 4 + tileOffsets.size() * 2;
        message.appendInt(paramCount);
        message.appendInt(sourceType);
        message.appendInt(filterExisting ? 1 : 0);
        message.appendInt(invert ? 1 : 0);
        message.appendInt(tileOffsets.size());
        for (int[] offset : tileOffsets) {
            message.appendInt(offset[0]);
            message.appendInt(offset[1]);
        }

        message.appendInt(0);
        message.appendInt(this.getType().code);
        message.appendInt(this.getDelay());
        message.appendInt(0);
    }

    @Override
    public WiredEffectType getType() { return type; }

    @Override
    public boolean isSelector() {
        return true;
    }

    @Override
    public String getWiredData() {
        return WiredManager.getGson().toJson(
                new JsonData(sourceType, filterExisting, invert, tileOffsets, pickedFurniIds, getDelay()));
    }

    @Override
    public void loadWiredData(ResultSet set, Room room) throws SQLException {
        String wiredData = set.getString("wired_data");
        if (wiredData != null && wiredData.startsWith("{")) {
            JsonData data = WiredManager.getGson().fromJson(wiredData, JsonData.class);
            this.sourceType    = data.sourceType;
            this.filterExisting = data.filterExisting;
            this.invert        = data.invert;
            this.tileOffsets   = data.tileOffsets != null ? data.tileOffsets : new ArrayList<>();
            this.pickedFurniIds = data.pickedFurniIds != null ? data.pickedFurniIds : new ArrayList<>();
            this.setDelay(data.delay);
        }
    }

    @Override
    public void onPickUp() {
        this.sourceType    = SOURCE_USER_TRIGGER;
        this.filterExisting = false;
        this.invert        = false;
        this.tileOffsets   = new ArrayList<>();
        this.pickedFurniIds = new ArrayList<>();
        this.setDelay(0);
    }

    @Override
    public boolean execute(RoomUnit roomUnit, Room room, Object[] stuff) { return false; }

    static class JsonData {
        int            sourceType;
        boolean        filterExisting;
        boolean        invert;
        List<int[]>    tileOffsets;
        List<Integer>  pickedFurniIds;
        int            delay;

        JsonData(int sourceType, boolean filterExisting, boolean invert,
                 List<int[]> tileOffsets, List<Integer> pickedFurniIds, int delay) {
            this.sourceType     = sourceType;
            this.filterExisting = filterExisting;
            this.invert         = invert;
            this.tileOffsets    = tileOffsets;
            this.pickedFurniIds = pickedFurniIds;
            this.delay          = delay;
        }
    }
}
