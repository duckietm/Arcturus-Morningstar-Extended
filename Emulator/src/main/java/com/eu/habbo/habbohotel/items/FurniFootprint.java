package com.eu.habbo.habbohotel.items;

import java.util.ArrayList;
import java.util.List;

/**
 * The tiles a furni actually occupies, and which way a unit faces when it sits on each of them.
 *
 * <p>Until now a furni was always a full {@code width x length} rectangle. Plenty of custom furni are not:
 * an L-shaped sofa draws over eight tiles but only ever fitted a rectangle, so half of it was scenery you
 * could walk through and could not sit on. A footprint carves tiles out of that rectangle, and gives each
 * remaining tile its own seating direction - the two arms of an L face different ways.
 *
 * <p>The shape is authored at rotation 0 and turned with the furni, exactly like the artwork. It is stored
 * as one character per tile, rows (y) separated by {@code /}:
 *
 * <pre>
 *   tile_shape      "1111/1000"   a 4x2 L: the whole back row, then only the left tile of the front row
 *   sit_directions  "4444/2..."   back row faces 4, front-left faces 2, '.' inherits the furni rotation
 *                   "…|…|…|…"     optional rows for rotations 2, 4 and 6, each in that rotation's own box
 *
 * A direction in the base slot is authored at rotation 0 and TURNS with the furni (stored + rotation). A rotation
 * with its own slot holds the facing to use AS IS there and wins over the base: the two sides of a two-way furni
 * map back to the same base cell, so before this they had to share one angle.
 * </pre>
 *
 * <p>Both columns are optional. An absent or malformed value yields {@link #rectangle}, which behaves
 * exactly as the engine always has - so every furni that has never been through the aligner is untouched.
 */
public final class FurniFootprint {

    /** One character per tile in a row; rows separated by this. */
    public static final char ROW_SEPARATOR = '/';

    /** Separates the optional per-rotation masks, in the order 0, 2, 4, 6. */
    public static final char ROTATION_SEPARATOR = '|';

    /** A sit direction cell that defers to the furni's own rotation. */
    public static final char INHERIT_DIRECTION = '.';

    /** No per-tile direction: the caller should keep using the furni rotation. */
    public static final int NO_DIRECTION = -1;

    /** Guards the stored strings, which are VARCHAR(160) - 12x12 shape plus separators fits. */
    public static final int MAX_SIDE = 12;

    /** Separates an optional "ox,oy" anchor offset from the rows that follow it. */
    public static final char ANCHOR_SEPARATOR = ':';

    /**
     * Where the shape starts, relative to the tile the furni is placed on.
     *
     * Zero for every furni whose drawing sits on its own tile. Negative when the artwork spills up or left of
     * it - which is most custom furni that declare a 1x1 bundle but occupy more than one tile - and the only
     * way to put the collision under the picture rather than beside it.
     */
    private int anchorX = 0;
    private int anchorY = 0;

    /** Offsets drawn for a specific rotation. The base one above is used for any rotation without its own. */
    private final java.util.Map<Integer, int[]> anchors = new java.util.HashMap<>();

    private final int width;
    private final int length;
    private final boolean[][] occupied;
    private final int[][] sitDirections;
    private final boolean custom;

    /**
     * Masks drawn for a specific rotation, keyed 0/2/4/6. Absent means "turn the rotation-0 mask", which is
     * right whenever the artwork turns with the furni; present means the artwork does not, and the mask was
     * drawn for that direction and is used as it stands.
     */
    private final java.util.Map<Integer, boolean[][]> perRotation = new java.util.HashMap<>();

    /** Seating directions authored for one rotation, in that rotation's own box, used as they stand. */
    private final java.util.Map<Integer, int[][]> perRotationDirections = new java.util.HashMap<>();

    private FurniFootprint(int width, int length, boolean[][] occupied, int[][] sitDirections, boolean custom) {
        this.width = width;
        this.length = length;
        this.occupied = occupied;
        this.sitDirections = sitDirections;
        this.custom = custom;
    }

    /**
     * The plain {@code width x length} rectangle with no per-tile directions - what every furni was before
     * footprints existed.
     */
    public static FurniFootprint rectangle(int width, int length) {
        int w = Math.max(1, width);
        int l = Math.max(1, length);
        boolean[][] occupied = new boolean[l][w];
        int[][] directions = new int[l][w];

        for (int y = 0; y < l; y++) {
            for (int x = 0; x < w; x++) {
                occupied[y][x] = true;
                directions[y][x] = NO_DIRECTION;
            }
        }

        return new FurniFootprint(w, l, occupied, directions, false);
    }

    /**
     * Reads the two stored columns. Anything that does not describe a usable shape - wrong row count, wrong
     * row width, no tile left occupied - falls back to the rectangle rather than leaving a furni that cannot
     * be placed anywhere.
     */
    public static FurniFootprint parse(int width, int length, String shape, String sitDirections) {
        FurniFootprint fallback = rectangle(width, length);

        if (width < 1 || length < 1 || width > MAX_SIDE || length > MAX_SIDE) {
            return fallback;
        }

        String[] rotationShapes = shape == null
                ? new String[0]
                : shape.split("\\" + ROTATION_SEPARATOR, -1);

        int[] anchor = readAnchor(rotationShapes.length > 0 ? rotationShapes[0] : null);

        // The anchor lives on slot 0 but applies to every rotation, so it is stripped before the rows are read.
        if (rotationShapes.length > 0) rotationShapes[0] = stripAnchor(rotationShapes[0]);

        boolean[][] occupied = readShape(fallback.width, fallback.length, rotationShapes.length > 0 ? rotationShapes[0] : null);
        boolean custom = occupied != null;
        boolean baseWasDrawn = custom;

        // A plain rectangle that has been moved off its tile is still not the default, and must take the
        // offsets path in RoomLayout.getTilesAt rather than the plain width x length one.
        if (anchor[0] != 0 || anchor[1] != 0) custom = true;

        if (occupied == null) {
            occupied = fallback.occupied;
        }

        String[] directionSlots = sitDirections == null
                ? new String[0]
                : sitDirections.split("\\" + ROTATION_SEPARATOR, -1);

        int[][] directions = readDirections(
                fallback.width, fallback.length, occupied, directionSlots.length > 0 ? directionSlots[0] : null);

        if (directions == null) {
            directions = fallback.sitDirections;
        } else {
            custom = true;
        }

        FurniFootprint footprint = new FurniFootprint(fallback.width, fallback.length, occupied, directions, true);

        footprint.anchorX = anchor[0];
        footprint.anchorY = anchor[1];

        // Rotations 2, 4 and 6 in order, each optional and each drawn in the box that rotation uses.
        int[] rotations = {2, 4, 6};

        for (int index = 0; index < rotations.length; index++) {
            int slot = index + 1;

            if (rotationShapes.length <= slot || rotationShapes[slot].isEmpty()) continue;

            int rotation = rotations[index];
            int[] slotAnchor = readAnchor(rotationShapes[slot]);
            boolean[][] drawn = readShape(footprint.boxWidth(rotation), footprint.boxLength(rotation), stripAnchor(rotationShapes[slot]));

            // An explicit prefix is kept even when it is "0,0": an override drawn on the furni's own tile must not
            // inherit a base anchor that was moved somewhere else.
            if (hasAnchorPrefix(rotationShapes[slot])) {
                footprint.anchors.put(rotation, slotAnchor);
                custom = true;
            }

            if (drawn != null) {
                footprint.perRotation.put(rotation, drawn);

                // A furni can be a plain rectangle head-on and a different shape turned. The editor stores
                // exactly that as an empty slot 0, so this has to count as custom or the override is lost.
                custom = true;
            }
        }

        // Direction slots come after the masks: each is in its own rotation's box and needs that box's occupancy.
        for (int index = 0; index < rotations.length; index++) {
            int slot = index + 1;

            if (directionSlots.length <= slot || directionSlots[slot].isEmpty()) continue;

            int rotation = rotations[index];
            int[][] own = readDirections(
                    footprint.boxWidth(rotation),
                    footprint.boxLength(rotation),
                    footprint.rotationOccupancy(rotation),
                    directionSlots[slot]);

            if (own != null) {
                footprint.perRotationDirections.put(rotation, own);
                custom = true;
            }
        }

        // Nothing usable was stored anywhere: a plain rectangle, so hand back the shared fallback.
        if (!custom) return fallback;

        // The base was not drawn, but some rotation was. A furni only offers the rotations its bundle
        // declares, and a two-way one has no rotation 0 to draw - so without this its base stays a full
        // rectangle and every rotation nobody drew inherits that instead of the authored shape.
        if (!baseWasDrawn) {
            footprint.adoptBaseFromRotation();
        }

        return footprint;
    }

    private static boolean[][] readShape(int width, int length, String shape) {
        if (shape == null || shape.isEmpty()) return null;

        String[] rows = split(shape, length);
        if (rows == null) return null;

        boolean[][] occupied = new boolean[length][width];
        boolean any = false;

        for (int y = 0; y < length; y++) {
            if (rows[y].length() != width) return null;

            for (int x = 0; x < width; x++) {
                char c = rows[y].charAt(x);
                if (c != '0' && c != '1') return null;

                occupied[y][x] = (c == '1');
                any |= occupied[y][x];
            }
        }

        return any ? occupied : null;
    }

    private static int[][] readDirections(int width, int length, boolean[][] occupied, String value) {
        if (value == null || value.isEmpty()) return null;

        String[] rows = split(value, length);
        if (rows == null) return null;

        int[][] directions = new int[length][width];
        boolean any = false;

        for (int y = 0; y < length; y++) {
            if (rows[y].length() != width) return null;

            for (int x = 0; x < width; x++) {
                char c = rows[y].charAt(x);

                if (c == INHERIT_DIRECTION) {
                    directions[y][x] = NO_DIRECTION;
                    continue;
                }

                if (c < '0' || c > '7') return null;

                // A direction on a tile the furni does not occupy is meaningless, not fatal.
                directions[y][x] = occupied[y][x] ? (c - '0') : NO_DIRECTION;
                any |= occupied[y][x];
            }
        }

        return any ? directions : null;
    }

    private static String[] split(String value, int expectedRows) {
        String[] rows = value.split("\\" + ROW_SEPARATOR, -1);
        return rows.length == expectedRows ? rows : null;
    }

    /** True when this furni was shaped in the aligner and is not a plain rectangle. */
    public boolean isCustom() {
        return this.custom;
    }

    public int getWidth() {
        return this.width;
    }

    public int getLength() {
        return this.length;
    }

    /**
     * Offsets from the anchor tile, {@code {dx, dy}}, for a furni turned to {@code rotation}.
     *
     * <p>Rotations 0 and 4 keep the {@code width x length} box, 2 and 6 swap it - the same convention
     * {@code RoomLayout.getTilesAt} has always used, so a rectangle produces exactly the tiles it used to.
     */
    public List<int[]> offsetsFor(int rotation) {
        int boxWidth = boxWidth(rotation);
        int boxLength = boxLength(rotation);
        List<int[]> offsets = new ArrayList<>(boxWidth * boxLength);

        for (int dy = 0; dy < boxLength; dy++) {
            for (int dx = 0; dx < boxWidth; dx++) {
                // isOccupied speaks the same anchored offsets the callers do, so the box is walked in its
                // own coordinates and each hit is emitted where the tile actually is.
                int[] at = anchor(rotation);

                if (isOccupied(rotation, dx + at[0], dy + at[1])) {
                    offsets.add(new int[]{dx + at[0], dy + at[1]});
                }
            }
        }

        return offsets;
    }

    public int boxWidth(int rotation) {
        return isSwapped(rotation) ? this.length : this.width;
    }

    public int boxLength(int rotation) {
        return isSwapped(rotation) ? this.width : this.length;
    }

    /**
     * Whether the tile {@code dx, dy} away from the furni's own tile is part of it.
     *
     * The offsets are relative to where the furni is placed, not to the corner of its shape - those differ
     * whenever the shape has been anchored off its tile.
     */
    public boolean isOccupied(int rotation, int dx, int dy) {
        int[] at = anchor(rotation);

        dx -= at[0];
        dy -= at[1];

        boolean[][] drawn = this.perRotation.get(rotation);

        if (drawn != null) {
            // Drawn for this rotation, so it is already in the right orientation.
            if (dy < 0 || dy >= drawn.length || dx < 0 || dx >= drawn[dy].length) return false;
            return drawn[dy][dx];
        }

        int[] source = source(rotation, dx, dy);
        return source != null && this.occupied[source[1]][source[0]];
    }

    /**
     * The direction a unit sitting on {@code dx, dy} should face, or {@link #NO_DIRECTION} when the tile has
     * none and the furni's own rotation should be used. The stored direction is authored at rotation 0, so
     * it turns with the furni.
     */
    public int sitDirectionAt(int rotation, int dx, int dy) {
        int[] at = anchor(rotation);

        // A slot authored for this rotation is the facing itself and wins over the base one.
        int[][] own = this.perRotationDirections.get(rotation);

        if (own != null) {
            int oy = dy - at[1];
            int ox = dx - at[0];

            if (oy >= 0 && oy < own.length && ox >= 0 && ox < own[oy].length && own[oy][ox] != NO_DIRECTION) {
                return own[oy][ox];
            }
        }

        int[] source = source(rotation, (dx - at[0]), (dy - at[1]));
        if (source == null) return NO_DIRECTION;

        int stored = this.sitDirections[source[1]][source[0]];
        if (stored == NO_DIRECTION) return NO_DIRECTION;

        return Math.floorMod(stored + rotation, 8);
    }

    /** The occupancy of {@code rotation}'s own box: its drawn mask, or the base turned into it. */
    private boolean[][] rotationOccupancy(int rotation) {
        boolean[][] drawn = this.perRotation.get(rotation);
        if (drawn != null) return drawn;

        int boxWidth = boxWidth(rotation);
        int boxLength = boxLength(rotation);
        boolean[][] grid = new boolean[boxLength][boxWidth];

        for (int dy = 0; dy < boxLength; dy++) {
            for (int dx = 0; dx < boxWidth; dx++) {
                int[] source = source(rotation, dx, dy);
                if (source != null) grid[dy][dx] = this.occupied[source[1]][source[0]];
            }
        }

        return grid;
    }

    /**
     * Rebuilds the base mask from the lowest rotation that carries one.
     *
     * Only called when nothing was stored for the base. Every tile of that rotation's box is mapped back
     * through {@link FurniFootprint#source} - the same mapping the derived rotations already use - so the
     * result is the shape as it would have been authored head-on, and every rotation without a mask of its
     * own then derives from it consistently.
     *
     * Writes into the mask in place. On this path it is the array the local fallback rectangle was built
     * with, and that fallback is discarded here - nothing else can see it.
     */
    private void adoptBaseFromRotation() {
        for (int rotation : new int[]{2, 4, 6}) {
            boolean[][] drawn = this.perRotation.get(rotation);

            if (drawn == null) continue;

            int boxWidth = boxWidth(rotation);
            int boxLength = boxLength(rotation);
            boolean any = false;

            for (int dy = 0; dy < boxLength; dy++) {
                for (int dx = 0; dx < boxWidth; dx++) {
                    int[] target = source(rotation, dx, dy);

                    if (target == null) continue;

                    this.occupied[target[1]][target[0]] = drawn[dy][dx];

                    if (drawn[dy][dx]) any = true;
                }
            }

            // A rotation that occupies nothing would leave a furni that cannot be placed; leave the
            // rectangle alone and try the next one.
            if (any) return;

            for (int y = 0; y < this.length; y++) {
                for (int x = 0; x < this.width; x++) this.occupied[y][x] = true;
            }
        }
    }

    /**
     * How far the shape is offset from the furni's tile at {@code rotation}, or {0, 0} when it is not.
     *
     * A rotation without an offset of its own uses the base one, so a furni whose poses sit in the same place
     * needs to carry only one.
     */
    public int[] anchor(int rotation) {
        return this.anchors.getOrDefault(rotation, new int[]{this.anchorX, this.anchorY});
    }

    /**
     * Reads an "ox,oy:" prefix. Returns {0, 0} for anything that is not one, so an unprefixed shape - every
     * shape stored before this existed - reads exactly as it did.
     */
    private static int[] readAnchor(String slot) {
        if (slot == null) return new int[]{0, 0};

        int mark = slot.indexOf(ANCHOR_SEPARATOR);
        if (mark <= 0) return new int[]{0, 0};

        String[] parts = slot.substring(0, mark).split(",");
        if (parts.length != 2) return new int[]{0, 0};

        try {
            int x = Integer.parseInt(parts[0].trim());
            int y = Integer.parseInt(parts[1].trim());

            // Bounded so a bad value cannot push a furni's collision across the room.
            if (x < -MAX_SIDE || x > MAX_SIDE || y < -MAX_SIDE || y > MAX_SIDE) return new int[]{0, 0};

            return new int[]{x, y};
        } catch (NumberFormatException e) {
            return new int[]{0, 0};
        }
    }

    /** Whether the slot starts with a well-formed "ox,oy:" prefix, zero or not. */
    private static boolean hasAnchorPrefix(String slot) {
        if (slot == null) return false;

        int mark = slot.indexOf(ANCHOR_SEPARATOR);
        if (mark <= 0) return false;

        return slot.substring(0, mark).matches("-?\\d+,-?\\d+");
    }

    private static String stripAnchor(String slot) {
        if (slot == null) return null;

        int mark = slot.indexOf(ANCHOR_SEPARATOR);
        if (mark <= 0) return slot;

        // Only strip what actually parsed as an anchor; anything else is left for readShape to reject.
        String head = slot.substring(0, mark);
        if (!head.matches("-?\\d+,-?\\d+")) return slot;

        return slot.substring(mark + 1);
    }

    /** Maps a tile in the rotated box back to its authored position. */
    private int[] source(int rotation, int dx, int dy) {
        int boxWidth = boxWidth(rotation);
        int boxLength = boxLength(rotation);

        if (dx < 0 || dy < 0 || dx >= boxWidth || dy >= boxLength) return null;

        return switch (rotation) {
            case 2 -> new int[]{dy, this.length - 1 - dx};
            case 4 -> new int[]{this.width - 1 - dx, this.length - 1 - dy};
            case 6 -> new int[]{this.width - 1 - dy, dx};
            default -> new int[]{dx, dy};
        };
    }

    private static boolean isSwapped(int rotation) {
        return rotation == 2 || rotation == 6;
    }

    /**
     * Validates a shape string for storage. Returns the value to store, or null when it does not describe a
     * usable shape. An empty result means "no custom shape", which is a legitimate value to save.
     */
    public static String normalizeShape(int width, int length, String shape) {
        if (shape == null || shape.isEmpty()) return "";
        if (width < 1 || length < 1 || width > MAX_SIDE || length > MAX_SIDE) return null;

        String[] parts = shape.split("\\" + ROTATION_SEPARATOR, -1);

        if (parts.length > 4) return null;

        // An empty base is how "a plain rectangle, corrected only at some rotations" is written.
        String base = stripAnchor(parts[0]);
        if (!base.isEmpty() && readShape(width, length, base) == null) return null;

        // Rotations 2 and 6 swap the box; each optional mask has to fit the box for its own rotation.
        int[] rotations = {2, 4, 6};

        for (int index = 0; index < rotations.length; index++) {
            int slot = index + 1;

            if (parts.length <= slot || parts[slot].isEmpty()) continue;

            int rotation = rotations[index];
            boolean swapped = rotation == 2 || rotation == 6;

            if (readShape(swapped ? length : width, swapped ? width : length, stripAnchor(parts[slot])) == null) return null;
        }

        return shape;
    }

    /** Validates a sit-direction string for storage, on the same terms as {@link #normalizeShape}. */
    public static String normalizeSitDirections(int width, int length, String shape, String directions) {
        if (directions == null || directions.isEmpty()) return "";
        if (width < 1 || length < 1 || width > MAX_SIDE || length > MAX_SIDE) return null;

        boolean[][] occupied = readShape(width, length, shape);
        if (occupied == null) occupied = rectangle(width, length).occupied;

        return readDirections(width, length, occupied, directions) != null ? directions : null;
    }
}
