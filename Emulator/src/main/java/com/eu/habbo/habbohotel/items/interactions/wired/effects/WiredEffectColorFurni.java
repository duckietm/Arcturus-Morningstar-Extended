package com.eu.habbo.habbohotel.items.interactions.wired.effects;

import com.eu.habbo.Emulator;
import com.eu.habbo.WiredPlatform;
import com.eu.habbo.habbohotel.gameclients.GameClient;
import com.eu.habbo.habbohotel.items.Item;
import com.eu.habbo.habbohotel.items.ItemManager;
import com.eu.habbo.habbohotel.items.interactions.FurnitureCustomColors;
import com.eu.habbo.habbohotel.items.interactions.InteractionWiredEffect;
import com.eu.habbo.habbohotel.items.interactions.wired.WiredSettings;
import com.eu.habbo.habbohotel.rooms.Room;
import com.eu.habbo.habbohotel.rooms.RoomUnit;
import com.eu.habbo.habbohotel.users.HabboItem;
import com.eu.habbo.habbohotel.wired.WiredEffectType;
import com.eu.habbo.habbohotel.wired.core.WiredContext;
import com.eu.habbo.habbohotel.wired.core.WiredManager;
import com.eu.habbo.habbohotel.wired.core.WiredSourceUtil;
import com.eu.habbo.messages.ServerMessage;
import com.eu.habbo.messages.incoming.wired.WiredSaveException;
import com.eu.habbo.messages.outgoing.rooms.items.AddFloorItemComposer;
import com.eu.habbo.messages.outgoing.rooms.items.RemoveFloorItemComposer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * WIRED effect "wf_act_furni_color" (client layout code 115): sets a colour on colourable furni.
 *
 * Only recolourable furni are affected: the ones carrying COLOR1/COLOR2 layers, which the catalogue lists under
 * "Ricolorabili". Those are tinted in place, so the colour is what everybody sees and it survives pick-ups and
 * restarts. Everything else is left alone.
 *
 * It used to also swap {@code <classname>*<index>} colour variants by re-pointing the row in {@code items}, which
 * removes and re-adds the furni. Too many ordinary furni carry variants - rollers included (queue_tile1*2..*9) - so
 * an effect aimed at a few lamps re-pointed the rollers under people's feet and cancelled the slides they were
 * running. {@link #findVariant} and {@link #recolor} remain for callers outside this class.
 */
public class WiredEffectColorFurni extends InteractionWiredEffect {
    public static final WiredEffectType type = WiredEffectType.COLOR_FURNI;
    private static final Logger LOGGER = LoggerFactory.getLogger(WiredEffectColorFurni.class);
    static final int MINIMUM_COLOR_INDEX = 0;
    static final int MAXIMUM_COLOR_INDEX = 99;

    /** The chosen colour is used as-is. */
    static final int MODE_FIXED = 0;
    /** A different colour every run, never the one already worn and never a repeat until all have been used. */
    static final int MODE_RANDOM = 1;
    /** One step along the rainbow each run. */
    static final int MODE_RAINBOW = 2;

    /** Steps in a full turn of the hue wheel. Twelve reads as a rainbow without banding into stripes. */
    private static final int RAINBOW_STEPS = 12;

    private final List<Integer> itemIds = new ArrayList<>();
    private int colorIndex = 1;
    private int furniSource = WiredSourceUtil.SOURCE_SELECTED;
    /** Custom colours (0xRRGGBB, -1 = none) for guild-customised furni: the recolourable hlive_clr25 / habbox_clr lines. */
    private int customColorOne = -1;
    private int customColorTwo = -1;
    private int colorMode = MODE_FIXED;

    /** How far along the rainbow this effect has walked. */
    private int rainbowStep = 0;

    public WiredEffectColorFurni(ResultSet set, Item baseItem) throws SQLException {
        super(set, baseItem);
    }

    public WiredEffectColorFurni(int id, int userId, Item item, String extradata, int limitedStack, int limitedSells) {
        super(id, userId, item, extradata, limitedStack, limitedSells);
    }

    /** Classname without the {@code *index} colour suffix. */
    public static String baseName(String name) {
        if (name == null) return "";
        int star = name.indexOf('*');
        return star >= 0 ? name.substring(0, star) : name;
    }

    /** The items_base row of the same furni in colour {@code colorIndex}, or null when that variant does not exist. */
    public static Item findVariant(ItemManager itemManager, Item current, int colorIndex) {
        if (itemManager == null || current == null || current.getName() == null) return null;

        String base = baseName(current.getName());
        if (base.isEmpty()) return null;

        Item variant = itemManager.getItem(base + "*" + colorIndex);
        if (variant == null && colorIndex == 0) variant = itemManager.getItem(base);
        if (variant == null || variant.getType() != current.getType()) return null;

        return variant;
    }

    @Override
    public void execute(WiredContext context) {
        if (context == null || context.room() == null) return;

        Room room = context.room();
        List<HabboItem> selected = new ArrayList<>();
        for (int itemId : this.itemIds) {
            HabboItem item = room.getHabboItem(itemId);
            if (item != null) selected.add(item);
        }

        List<HabboItem> targets = WiredSourceUtil.resolveItems(context, this.furniSource, selected);
        if (targets == null || targets.isEmpty()) return;

        int recolored = 0;

        // One step per run, not per furni, so a row of furni driven by the same effect moves together.
        if (this.colorMode == MODE_RAINBOW) this.rainbowStep++;

        for (HabboItem target : new ArrayList<>(targets)) {
            if (target == null || target.getBaseItem() == null || target.getRoomId() != room.getId()) continue;
            if (room.getHabboItem(target.getId()) != target) continue;

            // Only recolourable furni - the ones carrying COLOR1/COLOR2 layers, the "Ricolorabili" tab -
            // are touched. Anything else would have to be swapped to a different items_base row, which means
            // removing and re-adding the furni; ordinary furni have colour variants too, rollers among them,
            // so that quietly re-pointed rollers under people's feet and made what stood on them jump.
            if (!FurnitureCustomColors.isColorable(target.getBaseItem())) continue;

            int[] picked = pickCustomColors(target);

            if (picked == null) continue;

            String one = FurnitureCustomColors.colorToHex(picked[0]);
            String two = FurnitureCustomColors.colorToHex(picked[1]);

            if (target.setCustomColors(one, two)) {
                room.updateItem(target);
                recolored++;
            }
        }

        if (recolored > 0) {
            LOGGER.debug("Wired furni colour: {} furni set to colour {} in room {}", recolored, this.colorIndex, room.getId());
        }
    }

    /**
     * Both colours to tint a recolourable furni with, or null when there is nothing to do.
     *
     * A recolourable furni has two: COLOR1 on the main parts and COLOR2 on the trim. Every mode drives both,
     * or the second slot would simply never change.
     */
    private int[] pickCustomColors(HabboItem target) {
        switch (this.colorMode) {
            case MODE_RAINBOW:
                // Opposite sides of the wheel, both walking. Same hue on both slots would flatten the furni
                // into one colour and lose the trim entirely.
                return new int[]{
                        rainbowColor(this.rainbowStep),
                        rainbowColor(this.rainbowStep + RAINBOW_STEPS / 2)
                };
            case MODE_RANDOM: {
                // Whole random RGBs rather than steps, since a recolourable furni has no palette to draw
                // from. Rolled independently so the trim is not just the body again, and each rerolled while
                // it matches what is already worn so a run always changes something visible.
                int wornOne = currentCustomColor(target, true);
                int wornTwo = currentCustomColor(target, false);

                return new int[]{roll(wornOne), roll(wornTwo)};
            }
            default:
                if (this.customColorOne < 0) return null;

                return new int[]{
                        this.customColorOne,
                        this.customColorTwo < 0 ? this.customColorOne : this.customColorTwo
                };
        }
    }

    /** A random colour that is not {@code avoid}, giving up after a few tries rather than looping. */
    private static int roll(int avoid) {
        int picked = avoid;

        for (int attempt = 0; attempt < 8 && picked == avoid; attempt++) {
            picked = Emulator.getRandom().nextInt(0x1000000);
        }

        return picked;
    }

    private static int currentCustomColor(HabboItem target, boolean first) {
        String worn = target == null ? null : (first ? target.getCustomColorOne() : target.getCustomColorTwo());

        if (worn == null || worn.isEmpty()) return -1;

        try {
            return Integer.parseInt(worn.replace("#", ""), 16) & 0xFFFFFF;
        } catch (NumberFormatException exception) {
            return -1;
        }
    }

    /**
     * One step along the hue wheel, fully saturated, as 0xRRGGBB.
     *
     * Written out rather than borrowed from java.awt so the emulator does not pull in the desktop module for
     * six lines of arithmetic.
     */
    static int rainbowColor(int step) {
        double hue = (Math.floorMod(step, RAINBOW_STEPS) / (double) RAINBOW_STEPS) * 6.0;
        int sector = (int) Math.floor(hue);
        double rising = hue - sector;
        int high = 255;
        int up = (int) Math.round(rising * 255.0);
        int down = 255 - up;

        return switch (sector % 6) {
            case 0 -> (high << 16) | (up << 8);
            case 1 -> (down << 16) | (high << 8);
            case 2 -> (high << 8) | up;
            case 3 -> (down << 8) | high;
            case 4 -> (up << 16) | high;
            default -> (high << 16) | down;
        };
    }

    private boolean recolor(Room room, ItemManager itemManager, HabboItem old, Item variant) {
        try (Connection connection = Emulator.getDatabase().getDataSource().getConnection();
                PreparedStatement statement = connection.prepareStatement("UPDATE items SET item_id = ? WHERE id = ?")) {
            statement.setInt(1, variant.getId());
            statement.setInt(2, old.getId());
            statement.execute();
        } catch (SQLException exception) {
            LOGGER.error("Wired furni colour: could not repoint item {} to base {}", old.getId(), variant.getId(), exception);
            return false;
        }

        // Position, rotation and extradata are flushed first so the fresh instance starts from the same row.
        if (old.needsUpdate()) old.run();

        HabboItem fresh = itemManager.loadHabboItem(old.getId());
        if (fresh == null) return false;

        room.sendComposer(new RemoveFloorItemComposer(old, true).compose());
        room.removeHabboItem(old);
        fresh.setRoomId(room.getId());
        room.addHabboItem(fresh);
        room.sendComposer(new AddFloorItemComposer(fresh, room.getFurniOwnerName(fresh.getUserId())).compose());
        room.updateItem(fresh);


        return true;
    }

    @Override
    public boolean saveData(WiredSettings settings, GameClient gameClient) throws WiredSaveException {
        Room room = getRoom();
        int[] parameters = settings == null ? null : settings.getIntParams();
        int[] selectedIds = settings == null || settings.getFurniIds() == null ? new int[0] : settings.getFurniIds();

        if (room == null || parameters == null || parameters.length < 1) {
            throw new WiredSaveException("Invalid furni colour effect data");
        }
        if (selectedIds.length > WiredManager.MAXIMUM_FURNI_SELECTION) {
            throw new WiredSaveException("Too many furni selected");
        }

        int delay = settings.getDelay();
        int maximumDelay = WiredPlatform.configuration().getInt("hotel.wired.max_delay", 20);
        if (delay < 0 || delay > maximumDelay) {
            throw new WiredSaveException("Delay out of range");
        }

        this.itemIds.clear();
        for (int itemId : selectedIds) {
            HabboItem item = room.getHabboItem(itemId);
            if (item == null) throw new WiredSaveException("Selected furni not found");
            this.itemIds.add(item.getId());
        }

        this.colorIndex = clamp(parameters[0], MINIMUM_COLOR_INDEX, MAXIMUM_COLOR_INDEX);
        this.furniSource = parameters.length > 1 ? WiredMovementPayloadGuard.furniSource(parameters[1]) : WiredSourceUtil.SOURCE_SELECTED;
        this.customColorOne = parameters.length > 2 ? clampColor(parameters[2]) : -1;
        this.customColorTwo = parameters.length > 3 ? clampColor(parameters[3]) : -1;
        this.colorMode = parameters.length > 4 ? clamp(parameters[4], MODE_FIXED, MODE_RAINBOW) : MODE_FIXED;

        // A new choice of mode starts its own cycle rather than continuing the last one.
                this.rainbowStep = 0;

        if (!this.itemIds.isEmpty() && this.furniSource == WiredSourceUtil.SOURCE_TRIGGER) {
            this.furniSource = WiredSourceUtil.SOURCE_SELECTED;
        }

        this.setDelay(delay);
        return true;
    }

    @Override
    public String getWiredData() {
        return WiredManager.getGson().toJson(new JsonData(this.colorIndex, this.furniSource, this.customColorOne, this.customColorTwo, this.colorMode, this.getDelay(), new ArrayList<>(this.itemIds)));
    }

    @Override
    public void loadWiredData(ResultSet set, Room room) throws SQLException {
        reset();

        JsonData data = WiredMovementPayloadGuard.fromJson(set.getString("wired_data"), JsonData.class);
        if (data == null) return;

        this.colorIndex = clamp(data.colorIndex, MINIMUM_COLOR_INDEX, MAXIMUM_COLOR_INDEX);
        this.furniSource = WiredMovementPayloadGuard.furniSource(data.furniSource);
        this.customColorOne = clampColor(data.customColorOne);
        this.customColorTwo = clampColor(data.customColorTwo);
        this.colorMode = clamp(data.colorMode, MODE_FIXED, MODE_RAINBOW);
        this.setDelay(WiredMovementPayloadGuard.delay(data.delay));

        if (data.itemIds != null) {
            for (Integer itemId : data.itemIds) {
                if (itemId != null && room.getHabboItem(itemId) != null && this.itemIds.size() < WiredManager.MAXIMUM_FURNI_SELECTION) {
                    this.itemIds.add(itemId);
                }
            }
        }

        if (!this.itemIds.isEmpty() && this.furniSource == WiredSourceUtil.SOURCE_TRIGGER) {
            this.furniSource = WiredSourceUtil.SOURCE_SELECTED;
        }
    }

    @Override
    public void serializeWiredData(ServerMessage message, Room room) {
        List<Integer> selected = new ArrayList<>();
        for (int itemId : this.itemIds) {
            if (room != null && room.getHabboItem(itemId) != null) selected.add(itemId);
        }

        message.appendBoolean(false);
        message.appendInt(WiredManager.MAXIMUM_FURNI_SELECTION);
        message.appendInt(selected.size());
        for (int itemId : selected) message.appendInt(itemId);
        message.appendInt(this.getBaseItem().getSpriteId());
        message.appendInt(this.getId());
        message.appendString("");
        message.appendInt(5);
        message.appendInt(this.colorIndex);
        message.appendInt(this.furniSource);
        message.appendInt(this.customColorOne);
        message.appendInt(this.customColorTwo);
        message.appendInt(this.colorMode);
        message.appendInt(0);
        message.appendInt(this.getType().code);
        message.appendInt(this.getDelay());
        message.appendInt(0);
    }

    @Override
    public WiredEffectType getType() {
        return type;
    }

    @Override
    public boolean requiresTriggeringUser() {
        return false;
    }

    @Deprecated
    @Override
    public boolean execute(RoomUnit roomUnit, Room room, Object[] stuff) {
        return false;
    }

    @Override
    public void onPickUp() {
        reset();
    }

    private void reset() {
        this.itemIds.clear();
        this.colorIndex = 1;
        this.furniSource = WiredSourceUtil.SOURCE_SELECTED;
        this.customColorOne = -1;
        this.customColorTwo = -1;
        this.colorMode = MODE_FIXED;
                this.rainbowStep = 0;
        this.setDelay(0);
    }

    private static int clampColor(int value) {
        return value < 0 ? -1 : value & 0xFFFFFF;
    }

    private static int clamp(int value, int minimum, int maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }

    static class JsonData {
        int colorIndex = 1;
        int furniSource = WiredSourceUtil.SOURCE_SELECTED;
        int customColorOne = -1;
        int customColorTwo = -1;
        int colorMode = MODE_FIXED;
        int delay;
        List<Integer> itemIds;

        JsonData() {}

        JsonData(int colorIndex, int furniSource, int customColorOne, int customColorTwo, int colorMode, int delay, List<Integer> itemIds) {
            this.colorIndex = colorIndex;
            this.furniSource = furniSource;
            this.customColorOne = customColorOne;
            this.customColorTwo = customColorTwo;
            this.colorMode = colorMode;
            this.delay = delay;
            this.itemIds = itemIds;
        }
    }
}
