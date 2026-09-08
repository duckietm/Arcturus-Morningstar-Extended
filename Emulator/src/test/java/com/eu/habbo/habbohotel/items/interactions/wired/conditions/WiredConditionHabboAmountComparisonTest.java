package com.eu.habbo.habbohotel.items.interactions.wired.conditions;

import static com.eu.habbo.habbohotel.items.interactions.wired.conditions.WiredConditionTestSupport.boxBase;
import static com.eu.habbo.habbohotel.items.interactions.wired.conditions.WiredConditionTestSupport.context;
import static com.eu.habbo.habbohotel.items.interactions.wired.conditions.WiredConditionTestSupport.room;
import static com.eu.habbo.habbohotel.items.interactions.wired.conditions.WiredConditionTestSupport.settings;
import static com.eu.habbo.habbohotel.items.interactions.wired.conditions.WiredConditionTestSupport.user;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.eu.habbo.habbohotel.items.interactions.InteractionWiredCondition;
import com.eu.habbo.habbohotel.items.interactions.wired.WiredComparison;
import com.eu.habbo.habbohotel.rooms.Room;
import com.eu.habbo.habbohotel.rooms.RoomUnit;
import com.eu.habbo.habbohotel.users.Habbo;
import com.eu.habbo.habbohotel.users.HabboInfo;
import com.eu.habbo.habbohotel.users.HabboInventory;
import com.eu.habbo.habbohotel.users.inventory.ItemsComponent;
import com.eu.habbo.habbohotel.wired.core.WiredContext;
import com.eu.habbo.habbohotel.wired.core.WiredSourceUtil;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * The credits, duckets, diamonds and item-count boxes save an operator with their amount. The "has"
 * boxes compare the way it says; the "lacks" boxes answer the opposite of the same comparison. A row
 * saved before the operator was honoured keeps the one way those boxes always compared.
 */
class WiredConditionHabboAmountComparisonTest {

    private static final int DUCKETS = 0;
    private static final int DIAMONDS = 5;

    @Test
    void theHasBoxesCompareTheWayTheOperatorSays() {
        Room room = room(1);
        RoomUnit actor = holdingTen(room);
        WiredContext ctx = context(room, actor);

        for (InteractionWiredCondition box : hasBoxes()) {
            assertTrue(
                    passes(box, ctx, WiredComparison.GREATER, 5), box.getClass().getSimpleName());
            assertFalse(
                    passes(box, ctx, WiredComparison.EQUAL, 5), box.getClass().getSimpleName());
            assertTrue(
                    passes(box, ctx, WiredComparison.LESS_EQUAL, 10),
                    box.getClass().getSimpleName());
            assertFalse(
                    passes(box, ctx, WiredComparison.LESS, 10), box.getClass().getSimpleName());
        }
    }

    @Test
    void theLacksBoxesAnswerTheOppositeOfTheSameComparison() {
        Room room = room(1);
        RoomUnit actor = holdingTen(room);
        WiredContext ctx = context(room, actor);

        for (InteractionWiredCondition box : lacksBoxes()) {
            assertFalse(
                    passes(box, ctx, WiredComparison.GREATER, 5), box.getClass().getSimpleName());
            assertTrue(
                    passes(box, ctx, WiredComparison.EQUAL, 5), box.getClass().getSimpleName());
            assertFalse(
                    passes(box, ctx, WiredComparison.LESS_EQUAL, 10),
                    box.getClass().getSimpleName());
            assertTrue(
                    passes(box, ctx, WiredComparison.LESS, 10), box.getClass().getSimpleName());
        }
    }

    @Test
    void rowsSavedBeforeTheOperatorKeepTheirOneWayComparison() throws Exception {
        Room room = room(1);
        RoomUnit actor = holdingTen(room);
        WiredContext ctx = context(room, actor);

        WiredConditionHabboHasCredits has = new WiredConditionHabboHasCredits(1, 1, boxBase(), "", 0, 0);
        has.loadWiredData(legacyRow("score", 10), room);
        assertTrue(has.evaluate(ctx), "ten credits reach an amount of ten");
        has.loadWiredData(legacyRow("score", 11), room);
        assertFalse(has.evaluate(ctx), "ten credits do not reach eleven");

        WiredConditionHabboLacksCredits lacks = new WiredConditionHabboLacksCredits(2, 1, boxBase(), "", 0, 0);
        lacks.loadWiredData(legacyRow("score", 10), room);
        assertFalse(lacks.evaluate(ctx), "ten credits are not fewer than ten");
        lacks.loadWiredData(legacyRow("score", 11), room);
        assertTrue(lacks.evaluate(ctx), "ten credits are fewer than eleven");

        WiredConditionHabboLacksDiamonds diamonds = new WiredConditionHabboLacksDiamonds(3, 1, boxBase(), "", 0, 0);
        diamonds.loadWiredData(legacyRow("amount", 11), room);
        assertTrue(diamonds.evaluate(ctx), "the diamonds row names its amount differently and still loads");
    }

    @Test
    void theWholeFamilyDefaultsToEveryUserAndAtLeastAndWritesTheOperator() {
        for (InteractionWiredCondition box : allBoxes()) {
            String name = box.getClass().getSimpleName();

            // Four ints: no quantifier sent, and an operator the dialog did not show.
            box.saveData(settings(new int[] {1, WiredComparison.GREATER, 5, WiredSourceUtil.SOURCE_TRIGGER}));
            JsonObject saved = JsonParser.parseString(box.getWiredData()).getAsJsonObject();
            assertEquals(0, saved.get("quantifier").getAsInt(), name + " quantifier");
            assertEquals(WiredComparison.GREATER, saved.get("operator").getAsInt(), name + " operator");

            // One int: nothing but a team colour, so the operator is the historical "at least".
            box.saveData(settings(new int[] {1}));
            saved = JsonParser.parseString(box.getWiredData()).getAsJsonObject();
            assertEquals(WiredComparison.GREATER_EQUAL, saved.get("operator").getAsInt(), name + " default");
        }
    }

    /** A user holding ten of everything: credits, duckets, diamonds and inventory items. */
    private static RoomUnit holdingTen(Room room) {
        RoomUnit actor = user(room, 10, 1);
        Habbo habbo = room.getHabbo(actor);
        HabboInfo info = habbo.getHabboInfo();
        when(info.getCredits()).thenReturn(10);
        when(info.getCurrencyAmount(DUCKETS)).thenReturn(10);
        when(info.getCurrencyAmount(DIAMONDS)).thenReturn(10);

        HabboInventory inventory = mock(HabboInventory.class);
        ItemsComponent items = mock(ItemsComponent.class);
        when(items.itemCount()).thenReturn(10);
        when(inventory.getItemsComponent()).thenReturn(items);
        when(habbo.getInventory()).thenReturn(inventory);
        return actor;
    }

    private static boolean passes(InteractionWiredCondition box, WiredContext ctx, int comparison, int amount) {
        box.saveData(settings(new int[] {1, comparison, amount, WiredSourceUtil.SOURCE_TRIGGER, 0}));
        return box.evaluate(ctx);
    }

    /** The JSON these boxes wrote before they had an operator: a comparison the dialog never showed. */
    private static ResultSet legacyRow(String amountField, int amount) throws SQLException {
        ResultSet set = mock(ResultSet.class);
        when(set.getString("wired_data"))
                .thenReturn("{\"teamType\":1,\"comparison\":1,\"" + amountField + "\":" + amount
                        + ",\"userSource\":0,\"quantifier\":1}");
        return set;
    }

    private static List<InteractionWiredCondition> hasBoxes() {
        return List.of(
                new WiredConditionHabboHasCredits(1, 1, boxBase(), "", 0, 0),
                new WiredConditionHabboHasDuckets(2, 1, boxBase(), "", 0, 0),
                new WiredConditionHabboHasMinItems(3, 1, boxBase(), "", 0, 0));
    }

    private static List<InteractionWiredCondition> lacksBoxes() {
        return List.of(
                new WiredConditionHabboLacksCredits(4, 1, boxBase(), "", 0, 0),
                new WiredConditionHabboLacksDuckets(5, 1, boxBase(), "", 0, 0),
                new WiredConditionHabboLacksDiamonds(6, 1, boxBase(), "", 0, 0));
    }

    private static List<InteractionWiredCondition> allBoxes() {
        List<InteractionWiredCondition> boxes = new java.util.ArrayList<>(hasBoxes());
        boxes.addAll(lacksBoxes());
        return boxes;
    }
}
