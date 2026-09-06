package com.eu.habbo.habbohotel.items;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.eu.habbo.habbohotel.items.interactions.InteractionDefault;
import com.eu.habbo.habbohotel.items.interactions.InteractionGate;
import com.eu.habbo.habbohotel.items.interactions.InteractionTeleportTile;
import com.eu.habbo.habbohotel.items.interactions.InteractionVendingMachine;
import com.eu.habbo.habbohotel.items.interactions.InteractionYoutubeTV;
import com.eu.habbo.habbohotel.items.interactions.wired.contract.InteractionWiredContractPayment;
import com.eu.habbo.habbohotel.items.interactions.wired.contract.InteractionWiredContractReward;
import com.eu.habbo.habbohotel.items.interactions.wired.contract.InteractionWiredContractTrade;
import com.eu.habbo.habbohotel.items.interactions.wired.contract.InteractionWiredCustomContract;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import org.junit.jupiter.api.Test;

class ItemInteractionRegistryCompatibilityTest {

    @Test
    void lookupFallbackOrderingAndUniquenessStayStable() {
        TestItemManager manager = new TestItemManager();
        manager.loadDefaults();

        ItemInteraction defaultInteraction = manager.getItemInteraction(InteractionDefault.class);
        assertSame(defaultInteraction, manager.getItemInteraction("missing-interaction"));

        List<String> names = manager.getInteractionList();
        List<String> sorted = new ArrayList<>(names);
        sorted.sort(String::compareTo);
        assertEquals(sorted, names);
        assertTrue(names.contains("default"));

        assertThrows(
                RuntimeException.class,
                () -> manager.addItemInteraction(
                        new ItemInteraction("duplicate-default-type", InteractionDefault.class)));
    }

    @Test
    void duplicateBuiltInNamesResolveDeterministicallyToLastRegistration() {
        TestItemManager manager = new TestItemManager();
        manager.loadDefaults();

        assertSame(
                InteractionWiredContractPayment.class,
                manager.getItemInteraction("wf_contract_payment").getType());
        assertSame(
                InteractionWiredContractReward.class,
                manager.getItemInteraction("WF_CONTRACT_REWARD").getType());
        assertSame(
                InteractionWiredContractTrade.class,
                manager.getItemInteraction("wf_contract_trade").getType());
        assertSame(
                InteractionWiredCustomContract.class,
                manager.getItemInteraction("wf_xtra_custom_contract").getType());

        List<String> names = manager.getInteractionList();
        assertEquals(
                names.size(),
                new HashSet<>(names.stream()
                                .map(name -> name.toLowerCase(Locale.ROOT))
                                .toList())
                        .size(),
                "runtime interaction names must be unique");
    }

    @Test
    void duplicateBuiltInNamesFailFastWithoutChangingTheExistingAliases() {
        ItemInteractionRegistry registry = new ItemInteractionRegistry();
        ItemInteraction first = new ItemInteraction("first", InteractionDefault.class);
        ItemInteraction replacement = new ItemInteraction("first", InteractionGate.class);

        assertTrue(registry.add(first));
        assertThrows(IllegalStateException.class, () -> registry.add(replacement));
        assertSame(first, registry.find("FIRST"));
        assertSame(first, registry.find(InteractionDefault.class));
    }

    @Test
    void furniEditorClassnameRecoversEveryRegisteredWiredInteraction() {
        TestItemManager manager = new TestItemManager();
        manager.loadDefaults();

        ItemInteraction selector = manager.resolveItemInteraction(
                "default", "wf_slc_users_area", "WIRED Selector: Users In Area");
        assertEquals("wf_slc_users_area", selector.getName());

        ItemInteraction extra = manager.resolveItemInteraction(
                "default", "wf_xtra_anim_time", "WIRED Add-on: Animation Time");
        assertEquals("wf_xtra_anim_time", extra.getName());

        ItemInteraction explicit = manager.resolveItemInteraction(
                "gate", "wf_slc_users_area", "WIRED Selector: Users In Area");
        assertEquals("gate", explicit.getName(), "an explicit non-default interaction must win");

        ItemInteraction decorative = manager.resolveItemInteraction(
                "default", "wf_wire2", "Wire Junction");
        assertEquals("default", decorative.getName(), "unregistered wf assets remain decorative");
    }

    @Test
    void legacyImportedActionAliasesResolveToTheirWorkingRuntimeHandlers() {
        TestItemManager manager = new TestItemManager();
        manager.loadDefaults();

        assertSame(
                InteractionTeleportTile.class,
                manager.resolveItemInteraction("teletile", "custom_tele", "Teleport").getType());
        assertSame(
                InteractionVendingMachine.class,
                manager.resolveItemInteraction("Vending", "custom_vendor", "Vendor").getType());
        assertSame(
                InteractionYoutubeTV.class,
                manager.resolveItemInteraction("yt_jukebox", "custom_tv", "TV").getType());
        assertEquals(
                "wf_conf_handitem_block",
                manager.resolveItemInteraction("conf_handitem_block", "conf_handitem_block", "Blocker").getName());
    }

    private static final class TestItemManager extends ItemManager {
        private void loadDefaults() {
            loadItemInteractions();
        }
    }
}
