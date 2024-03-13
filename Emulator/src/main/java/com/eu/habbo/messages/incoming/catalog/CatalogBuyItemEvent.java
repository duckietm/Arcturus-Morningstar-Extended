package com.eu.habbo.messages.incoming.catalog;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.bots.BotManager;
import com.eu.habbo.habbohotel.catalog.*;
import com.eu.habbo.habbohotel.catalog.layouts.*;
import com.eu.habbo.habbohotel.items.FurnitureType;
import com.eu.habbo.habbohotel.permissions.Permission;
import com.eu.habbo.habbohotel.pets.PetManager;
import com.eu.habbo.habbohotel.rooms.RoomManager;
import com.eu.habbo.habbohotel.users.HabboBadge;
import com.eu.habbo.habbohotel.users.HabboInventory;
import com.eu.habbo.habbohotel.users.subscriptions.Subscription;
import com.eu.habbo.messages.incoming.MessageHandler;
import com.eu.habbo.messages.outgoing.catalog.AlertPurchaseFailedComposer;
import com.eu.habbo.messages.outgoing.catalog.AlertPurchaseUnavailableComposer;
import com.eu.habbo.messages.outgoing.catalog.PurchaseOKComposer;
import com.eu.habbo.messages.outgoing.generic.alerts.BubbleAlertComposer;
import com.eu.habbo.messages.outgoing.generic.alerts.BubbleAlertKeys;
import com.eu.habbo.messages.outgoing.generic.alerts.HotelWillCloseInMinutesComposer;
import com.eu.habbo.messages.outgoing.inventory.InventoryRefreshComposer;
import com.eu.habbo.messages.outgoing.navigator.CanCreateRoomComposer;
import com.eu.habbo.messages.outgoing.users.*;
import com.eu.habbo.threading.runnables.ShutdownEmulator;
import gnu.trove.map.hash.THashMap;
import gnu.trove.procedure.TObjectProcedure;
import org.apache.commons.lang3.StringUtils;

import static com.eu.habbo.messages.incoming.catalog.CheckPetNameEvent.PET_NAME_LENGTH_MAXIMUM;
import static com.eu.habbo.messages.incoming.catalog.CheckPetNameEvent.PET_NAME_LENGTH_MINIMUM;

public class CatalogBuyItemEvent extends MessageHandler {


    @Override
    public void handle() throws Exception {
        if (Emulator.getIntUnixTimestamp() - this.client.getHabbo().getHabboStats().lastPurchaseTimestamp >= CatalogManager.PURCHASE_COOLDOWN) {
            this.client.getHabbo().getHabboStats().lastPurchaseTimestamp = Emulator.getIntUnixTimestamp();
            if (ShutdownEmulator.timestamp > 0) {
                this.client.sendResponse(new HotelWillCloseInMinutesComposer((ShutdownEmulator.timestamp - Emulator.getIntUnixTimestamp()) / 60));
                return;
            }

            int pageId = this.packet.readInt();
            int itemId = this.packet.readInt();
            String extraData = this.packet.readString();
            int count = this.packet.readInt();

            try {
                if (this.client.getHabbo().getInventory().getItemsComponent().itemCount() > HabboInventory.MAXIMUM_ITEMS) {
                    this.client.sendResponse(new AlertPurchaseFailedComposer(AlertPurchaseFailedComposer.SERVER_ERROR).compose());
                    this.client.getHabbo().alert(Emulator.getTexts().getValue("inventory.full"));
                    return;
                }
            } catch (Exception e) {
                this.client.sendResponse(new AlertPurchaseFailedComposer(AlertPurchaseFailedComposer.SERVER_ERROR).compose());
            }

            CatalogPage page = null;

            if (pageId == -12345678 || pageId == -1) {
                CatalogItem searchedItem = Emulator.getGameEnvironment().getCatalogManager().getCatalogItem(itemId);

                if (searchedItem.getOfferId() > 0) {
                    page = Emulator.getGameEnvironment().getCatalogManager().getCatalogPage(searchedItem.getPageId());

                    if(page != null) {
                        if (page.getCatalogItem(itemId).getOfferId() <= 0) {
                            page = null;
                        } else if (page.getRank() > this.client.getHabbo().getHabboInfo().getRank().getId()) {
                            page = null;
                        } else if (page.getLayout() != null && page.getLayout().equalsIgnoreCase(CatalogPageLayouts.club_gift.name())) {
                            page = null;
                        }
                    }
                }
            } else {
                page = Emulator.getGameEnvironment().getCatalogManager().catalogPages.get(pageId);

                if(page != null && page.getLayout() != null && page.getLayout().equalsIgnoreCase(CatalogPageLayouts.club_gift.name())) {
                    page = null;
                }

                if (page instanceof RoomBundleLayout) {
                    final CatalogItem[] item = new CatalogItem[1];
                    page.getCatalogItems().forEachValue(new TObjectProcedure<CatalogItem>() {
                        @Override
                        public boolean execute(CatalogItem object) {
                            item[0] = object;
                            return false;
                        }
                    });

                    CatalogItem roomBundleItem = item[0];
                    if (roomBundleItem == null || roomBundleItem.getCredits() > this.client.getHabbo().getHabboInfo().getCredits() || roomBundleItem.getPoints() > this.client.getHabbo().getHabboInfo().getCurrencyAmount(roomBundleItem.getPointsType())) {
                        this.client.sendResponse(new AlertPurchaseFailedComposer(AlertPurchaseFailedComposer.SERVER_ERROR));
                        return;
                    }
                    int roomCount = Emulator.getGameEnvironment().getRoomManager().getRoomsForHabbo(this.client.getHabbo()).size();
                    int maxRooms = this.client.getHabbo().getHabboStats().hasActiveClub() ? RoomManager.MAXIMUM_ROOMS_HC : RoomManager.MAXIMUM_ROOMS_USER;

                    if (roomCount >= maxRooms) { // checks if a user has the maximum rooms
                       this.client.sendResponse(new CanCreateRoomComposer(roomCount, maxRooms)); // if so throws the max room error.
                       this.client.sendResponse(new PurchaseOKComposer(null)); // Send this so the alert disappears, not sure if this is how it should be handled :S
                       return;
                    }
                        ((RoomBundleLayout) page).buyRoom(this.client.getHabbo());
                        if (!this.client.getHabbo().hasPermission(Permission.ACC_INFINITE_CREDITS)) { //if the player has this perm disabled
                            this.client.getHabbo().giveCredits(-roomBundleItem.getCredits()); // takes their credits away
                        }
                        if (!this.client.getHabbo().hasPermission(Permission.ACC_INFINITE_POINTS)) { //if the player has this perm disabled
                            this.client.getHabbo().givePoints(roomBundleItem.getPointsType(), -roomBundleItem.getPoints()); // takes their points away
                        }
                        this.client.sendResponse(new PurchaseOKComposer()); // Sends the composer to close the window.

                    final boolean[] badgeFound = {false};
                    item[0].getBaseItems().stream().filter(i -> i.getType() == FurnitureType.BADGE).forEach(i -> {
                        if (!this.client.getHabbo().getInventory().getBadgesComponent().hasBadge(i.getName())) {
                            HabboBadge badge = new HabboBadge(0, i.getName(), 0, this.client.getHabbo());
                            Emulator.getThreading().run(badge);
                            this.client.getHabbo().getInventory().getBadgesComponent().addBadge(badge);
                            this.client.sendResponse(new AddUserBadgeComposer(badge));
                            THashMap<String, String> keys = new THashMap<>();
                            keys.put("display", "BUBBLE");
                            keys.put("image", "${image.library.url}album1584/" + badge.getCode() + ".gif");
                            keys.put("message", Emulator.getTexts().getValue("commands.generic.cmd_badge.received"));
                            this.client.sendResponse(new BubbleAlertComposer(BubbleAlertKeys.RECEIVED_BADGE.key, keys)); //:test 1992 s:npc.gift.received i:2 s:npc_name s:Admin s:image s:${image.library.url}album1584/ADM.gif);
                        } else {
                            badgeFound[0] = true;
                        }
                    });

                    if (badgeFound[0]) {
                        this.client.getHabbo().getClient().sendResponse(new AlertPurchaseFailedComposer(AlertPurchaseFailedComposer.ALREADY_HAVE_BADGE));
                    }

                    return;
                }
            }

            if (page == null) {
                this.client.sendResponse(new AlertPurchaseFailedComposer(AlertPurchaseFailedComposer.SERVER_ERROR).compose());
                return;
            }

            if (page.getRank() > this.client.getHabbo().getHabboInfo().getRank().getId()) {
                this.client.sendResponse(new AlertPurchaseUnavailableComposer(AlertPurchaseUnavailableComposer.ILLEGAL));
                return;
            }

            if (page instanceof ClubBuyLayout || page instanceof VipBuyLayout) {
                ClubOffer item = Emulator.getGameEnvironment().getCatalogManager().clubOffers.get(itemId);

                if (item == null) {
                    this.client.sendResponse(new AlertPurchaseFailedComposer(AlertPurchaseFailedComposer.SERVER_ERROR).compose());
                    return;
                }

                int totalDays = 0;
                int totalCredits = 0;
                int totalDuckets = 0;

                for (int i = 0; i < count; i++) {
                    totalDays += item.getDays();
                    totalCredits += item.getCredits();
                    totalDuckets += item.getPoints();
                }

                if (totalDays > 0) {
                    if (this.client.getHabbo().getHabboInfo().getCurrencyAmount(item.getPointsType()) < totalDuckets)
                        return;

                    if (this.client.getHabbo().getHabboInfo().getCredits() < totalCredits)
                        return;

                    if (!this.client.getHabbo().hasPermission(Permission.ACC_INFINITE_CREDITS))
                        this.client.getHabbo().giveCredits(-totalCredits);

                    if (!this.client.getHabbo().hasPermission(Permission.ACC_INFINITE_POINTS))
                        this.client.getHabbo().givePoints(item.getPointsType(), -totalDuckets);


                    if(this.client.getHabbo().getHabboStats().createSubscription(Subscription.HABBO_CLUB, (totalDays * 86400)) == null) {
                        this.client.sendResponse(new AlertPurchaseFailedComposer(AlertPurchaseFailedComposer.SERVER_ERROR).compose());
                        throw new Exception("Unable to create or extend subscription");
                    }

                    /*if (this.client.getHabbo().getHabboStats().getClubExpireTimestamp() <= Emulator.getIntUnixTimestamp())
                        this.client.getHabbo().getHabboStats().setClubExpireTimestamp(Emulator.getIntUnixTimestamp());

                    this.client.getHabbo().getHabboStats().setClubExpireTimestamp(this.client.getHabbo().getHabboStats().getClubExpireTimestamp() + (totalDays * 86400));

                    this.client.sendResponse(new UserPermissionsComposer(this.client.getHabbo()));
                    this.client.sendResponse(new UserClubComposer(this.client.getHabbo()));*/

                    this.client.sendResponse(new PurchaseOKComposer(null));
                    this.client.sendResponse(new InventoryRefreshComposer());
                    
                    this.client.getHabbo().getHabboStats().run();
                }
                return;
            }

            CatalogItem item;

            if (page instanceof RecentPurchasesLayout)
                item = this.client.getHabbo().getHabboStats().getRecentPurchases().get(itemId);

            else
                item = page.getCatalogItem(itemId);
            // temp patch, can a dev with better knowledge than me look into this asap pls.
            if (page instanceof  BotsLayout) {
                if (!this.client.getHabbo().hasPermission(Permission.ACC_UNLIMITED_BOTS) && this.client.getHabbo().getInventory().getBotsComponent().getBots().size() >= BotManager.MAXIMUM_BOT_INVENTORY_SIZE) {
                    this.client.getHabbo().alert(Emulator.getTexts().getValue("error.bots.max.inventory").replace("%amount%", BotManager.MAXIMUM_BOT_INVENTORY_SIZE + ""));
                    return;
                }
            }
            if (page instanceof PetsLayout) { // checks it's the petlayout
                if (!this.client.getHabbo().hasPermission(Permission.ACC_UNLIMITED_PETS) && this.client.getHabbo().getInventory().getPetsComponent().getPets().size() >= PetManager.MAXIMUM_PET_INVENTORY_SIZE) {
                    this.client.getHabbo().alert(Emulator.getTexts().getValue("error.pets.max.inventory").replace("%amount%", PetManager.MAXIMUM_PET_INVENTORY_SIZE + ""));
                    return;
                }
                String[] check = extraData.split("\n"); // splits the extradata
                if ((check.length != 3) || (check[0].length() < PET_NAME_LENGTH_MINIMUM) || (check[0].length() > PET_NAME_LENGTH_MAXIMUM) || (!StringUtils.isAlphanumeric(check[0])))// checks if there's 3 parts (always is with pets, if not it fucks them off)
                    return; // if it does it fucks off.
                }

            Emulator.getGameEnvironment().getCatalogManager().purchaseItem(page, item, this.client.getHabbo(), count, extraData, false);

        } else {
            this.client.sendResponse(new AlertPurchaseFailedComposer(AlertPurchaseFailedComposer.SERVER_ERROR).compose());
        }
    }
}
