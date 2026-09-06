package com.eu.habbo.habbohotel.items;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * The footprint decides which tiles a furni blocks, so a rotation that is off by one paints a sofa you can
 * walk through and seats you cannot reach. These pin the shape at each of the four rotations.
 */
class FurniFootprintTest {

    /** "x,y" for every occupied tile, sorted, so a set comparison reads as coordinates. */
    private static String tiles(FurniFootprint footprint, int rotation) {
        return footprint.offsetsFor(rotation).stream()
                .map(offset -> offset[0] + "," + offset[1])
                .sorted()
                .collect(Collectors.joining(" "));
    }

    @Test
    @DisplayName("no shape stored means the furni is the plain rectangle it always was")
    void defaultsToRectangle() {
        FurniFootprint footprint = FurniFootprint.parse(2, 3, "", "");

        assertFalse(footprint.isCustom());
        assertEquals(6, footprint.offsetsFor(0).size());
        assertEquals("0,0 0,1 0,2 1,0 1,1 1,2", tiles(footprint, 0));

        // Rotations 2 and 6 swap the box, exactly as RoomLayout.getTilesAt has always done.
        assertEquals(3, footprint.boxWidth(2));
        assertEquals(2, footprint.boxLength(2));
    }

    @Test
    @DisplayName("an L keeps its shape through every quarter turn")
    void rotatesTheShape() {
        // 4 wide, 2 long. Back row full, front row only its leftmost tile:
        //   x: 0 1 2 3
        //   y0 1 1 1 1
        //   y1 1 0 0 0
        FurniFootprint footprint = FurniFootprint.parse(4, 2, "1111/1000", "");

        assertTrue(footprint.isCustom());
        assertEquals(5, footprint.offsetsFor(0).size());
        assertEquals("0,0 0,1 1,0 2,0 3,0", tiles(footprint, 0));

        // Every rotation keeps the same number of tiles - a turn moves an L, it does not resize it.
        for (int rotation : new int[]{0, 2, 4, 6}) {
            assertEquals(5, footprint.offsetsFor(rotation).size(), "tiles at rotation " + rotation);
        }

        // 90 degrees: the box becomes 2 wide by 4 long, and the long arm runs down the right-hand column.
        assertEquals(2, footprint.boxWidth(2));
        assertEquals(4, footprint.boxLength(2));
        assertEquals("0,0 1,0 1,1 1,2 1,3", tiles(footprint, 2));

        // 180 degrees: the same 4x2 box, mirrored in both axes.
        assertEquals("0,1 1,1 2,1 3,0 3,1", tiles(footprint, 4));

        // 270 degrees: back to a 2x4 box, long arm down the left-hand column.
        assertEquals("0,0 0,1 0,2 0,3 1,3", tiles(footprint, 6));
    }

    @Test
    @DisplayName("a seating direction is authored at rotation 0 and turns with the furni")
    void turnsSeatingDirections() {
        // Both back tiles face 4 (south-east); the front-left tile faces 2 (north-east).
        FurniFootprint footprint = FurniFootprint.parse(2, 2, "11/10", "44/2.");

        assertTrue(footprint.isCustom());
        assertEquals(4, footprint.sitDirectionAt(0, 0, 0));
        assertEquals(2, footprint.sitDirectionAt(0, 0, 1));

        // Turning the sofa 90 degrees turns its seats with it: 4 + 2 = 6.
        assertEquals(6, footprint.sitDirectionAt(2, 1, 0));

        // And all the way round, back to where it started.
        assertEquals(4, footprint.sitDirectionAt(0, 1, 0));
    }

    @Test
    @DisplayName("a tile with no direction defers to the furni's own rotation")
    void inheritsWhereUnset() {
        FurniFootprint footprint = FurniFootprint.parse(2, 1, "11", "4.");

        assertEquals(4, footprint.sitDirectionAt(0, 0, 0));
        assertEquals(FurniFootprint.NO_DIRECTION, footprint.sitDirectionAt(0, 1, 0));

        // Off the footprint entirely.
        assertEquals(FurniFootprint.NO_DIRECTION, footprint.sitDirectionAt(0, 9, 9));
    }

    @Test
    @DisplayName("a shape that does not describe the furni is ignored rather than trusted")
    void rejectsUnusableShapes() {
        // Wrong row count, wrong row width, illegal character, and nothing left occupied. Each has to fall
        // back to the rectangle - a furni that occupies no tiles could not be placed anywhere.
        List<String> broken = List.of("111", "11/11/11", "1x/11", "00/00");

        for (String shape : broken) {
            FurniFootprint footprint = FurniFootprint.parse(2, 2, shape, "");

            assertFalse(footprint.isCustom(), "should have rejected " + shape);
            assertEquals(4, footprint.offsetsFor(0).size(), "should be a full rectangle for " + shape);
        }
    }

    @Test
    @DisplayName("normalize accepts what parse accepts, and clearing is allowed")
    void normalizesForStorage() {
        assertEquals("1111/1000", FurniFootprint.normalizeShape(4, 2, "1111/1000"));
        assertEquals("", FurniFootprint.normalizeShape(4, 2, ""));
        assertEquals(null, FurniFootprint.normalizeShape(4, 2, "11/10"));

        assertEquals("44/2.", FurniFootprint.normalizeSitDirections(2, 2, "11/10", "44/2."));
        assertEquals(null, FurniFootprint.normalizeSitDirections(2, 2, "11/10", "49/2."));
    }
}
