package com.eu.habbo.habbohotel.rooms;

import com.eu.habbo.habbohotel.users.HabboItem;

/**
 * Bed Profile Configuration
 * ┌──────────────────────────────────────────────────────────────────────────┐
 * │  EDIT THESE VALUES TO ADJUST BED POSITIONING                           │
 * ├──────────────────┬──────────┬────────────┬────────────┬────────────────┤
 * │ Bed Type         │ Width    │ Lay X      │ Lay Y      │ Lay Z          │
 * ├──────────────────┼──────────┼────────────┼────────────┼────────────────┤
 * │ Flat single      │ single   │  0         │  0         │ -0.5           │
 * │ Flat double      │ double   │  0         │  0         │ -0.5           │
 * │ Raised single    │ single   │  0         │  0         │ -0.5           │
 * │ Raised double    │ double   │  0         │  0         │ -0.5           │
 * └──────────────────┴──────────┴────────────┴────────────┴────────────────┘
 */
public class BedProfile {

    // ===== HEIGHT THRESHOLD =====
    private static final double FLAT_BED_HEIGHT_THRESHOLD = 1.0;

    // ===== FLAT BED OFFSETS (sleeping bags, mats, etc.) =====
    private static final double FLAT_SINGLE_LAY_X_OFFSET = 0;
    private static final double FLAT_SINGLE_LAY_Y_OFFSET = 15;
    private static final double FLAT_SINGLE_LAY_Z_OFFSET = -0.5;

    private static final double FLAT_DOUBLE_LAY_X_OFFSET = 0;
    private static final double FLAT_DOUBLE_LAY_Y_OFFSET = 0;
    private static final double FLAT_DOUBLE_LAY_Z_OFFSET = -0.5;

    // ===== RAISED BED OFFSETS (normal beds with frames) =====
    private static final double RAISED_SINGLE_LAY_X_OFFSET = 5;
    private static final double RAISED_SINGLE_LAY_Y_OFFSET = 0;
    private static final double RAISED_SINGLE_LAY_Z_OFFSET = 0;

    private static final double RAISED_DOUBLE_LAY_X_OFFSET = 0;
    private static final double RAISED_DOUBLE_LAY_Y_OFFSET = 0;
    private static final double RAISED_DOUBLE_LAY_Z_OFFSET = -0;

    private final boolean isDouble;
    private final boolean isFlat;
    private final int rotation;
    private final int length;
    private final double layXOffset;
    private final double layYOffset;
    private final double layZOffset;

    public BedProfile(HabboItem bed) {
        this.rotation = bed.getRotation();
        this.length = Math.max(1, bed.getBaseItem().getLength());

        this.isDouble = bed.getBaseItem().getWidth() >= 2;
        this.isFlat = bed.getBaseItem().getHeight() < FLAT_BED_HEIGHT_THRESHOLD;

        if (this.isFlat) {
            if (this.isDouble) {
                this.layXOffset = FLAT_DOUBLE_LAY_X_OFFSET;
                this.layYOffset = FLAT_DOUBLE_LAY_Y_OFFSET;
                this.layZOffset = FLAT_DOUBLE_LAY_Z_OFFSET;
            } else {
                this.layXOffset = FLAT_SINGLE_LAY_X_OFFSET;
                this.layYOffset = FLAT_SINGLE_LAY_Y_OFFSET;
                this.layZOffset = FLAT_SINGLE_LAY_Z_OFFSET;
            }
        } else {
            if (this.isDouble) {
                this.layXOffset = RAISED_DOUBLE_LAY_X_OFFSET;
                this.layYOffset = RAISED_DOUBLE_LAY_Y_OFFSET;
                this.layZOffset = RAISED_DOUBLE_LAY_Z_OFFSET;
            } else {
                this.layXOffset = RAISED_SINGLE_LAY_X_OFFSET;
                this.layYOffset = RAISED_SINGLE_LAY_Y_OFFSET;
                this.layZOffset = RAISED_SINGLE_LAY_Z_OFFSET;
            }
        }
    }

    public double getLayXOffset() {
        return this.layXOffset;
    }

    public double getLayYOffset() {
        return this.layYOffset;
    }

    public double getLayZOffset() {
        return this.layZOffset;
    }

    // Nitro treats a negative lay height as "lay inside": the avatar is drawn behind the item's
    // front layer instead of on top of it. Flat items (sleeping bags, mats) need that so the body
    // is covered; framed beds keep the avatar on top of the mattress.
    private static final double FLAT_INSIDE_DEPTH = 0.15;

    public double getLayHeight(double itemHeight) {
        if (this.isFlat) {
            return -FLAT_INSIDE_DEPTH;
        }

        return Math.max(0.0D, itemHeight + this.layZOffset);
    }

    public boolean isDouble() {
        return this.isDouble;
    }

    public boolean isFlat() {
        return this.isFlat;
    }

    public boolean isLengthAlongY() {
        return this.rotation == 0 || this.rotation == 4;
    }

    /**
     * The pillow end of the bed. The item anchor is always the minimum corner of the
     * occupied tiles, so rotations 4 and 6 have their head at the far end of the length.
     */
    private short pillowX(HabboItem bed) {
        return (short) (this.rotation == 6 ? bed.getX() + this.length - 1 : bed.getX());
    }

    private short pillowY(HabboItem bed) {
        return (short) (this.rotation == 4 ? bed.getY() + this.length - 1 : bed.getY());
    }

    public RoomTile getPillow(Room room, short clickX, short clickY, HabboItem bed) {
        if (isLengthAlongY()) {
            return room.getLayout().getTile(clickX, pillowY(bed));
        } else {
            return room.getLayout().getTile(pillowX(bed), clickY);
        }
    }

    public RoomTile snapToLay(Room room, HabboItem bed, short unitX, short unitY) {
        RoomTile pillow = isLengthAlongY()
                ? room.getLayout().getTile(unitX, pillowY(bed))
                : room.getLayout().getTile(pillowX(bed), unitY);

        return pillow != null ? pillow : room.getLayout().getTile(unitX, unitY);
    }

    public RoomTile getOtherSide(Room room, HabboItem bed, RoomTile currentPillow) {
        if (!this.isDouble) return null;

        short otherX = currentPillow.x;
        short otherY = currentPillow.y;

        if (isLengthAlongY()) {
            otherX = (short) (bed.getX() + (currentPillow.x == bed.getX() ? 1 : -1));
        } else {
            otherY = (short) (bed.getY() + (currentPillow.y == bed.getY() ? 1 : -1));
        }

        RoomTile otherTile = room.getLayout().getTile(otherX, otherY);
        if (otherTile == null) return null;

        HabboItem itemAtOther = room.getTopItemAt(otherX, otherY);
        if (itemAtOther != null && itemAtOther.getId() == bed.getId()) {
            return otherTile;
        }
        return null;
    }
}
