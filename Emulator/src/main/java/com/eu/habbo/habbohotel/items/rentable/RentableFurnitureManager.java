package com.eu.habbo.habbohotel.items.rentable;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.catalog.CatalogItem;
import com.eu.habbo.habbohotel.catalog.CatalogPage;
import com.eu.habbo.habbohotel.catalog.CatalogPaymentService;
import com.eu.habbo.habbohotel.items.FurnitureType;
import com.eu.habbo.habbohotel.items.Item;
import com.eu.habbo.habbohotel.rooms.Room;
import com.eu.habbo.habbohotel.users.Habbo;
import com.eu.habbo.habbohotel.users.HabboItem;
import com.eu.habbo.messages.outgoing.inventory.RemoveHabboItemComposer;
import com.eu.habbo.messages.outgoing.rooms.items.RemoveFloorItemComposer;
import com.eu.habbo.messages.outgoing.rooms.items.RemoveWallItemComposer;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Rented furniture (catalog offers with {@code rent_days > 0}).
 *
 * <p>Every loaded {@link HabboItem} with a rent period registers itself here;
 * a periodic sweep removes the ones whose period is over, from the room or
 * the inventory they sit in, and deletes them. The manager also answers the
 * official extend / buy-out flow: the rent offer of a furni type prices an
 * extension, its plain purchase offer prices the buy-out.
 */
public final class RentableFurnitureManager implements Runnable {
    private static final Logger LOGGER = LoggerFactory.getLogger(RentableFurnitureManager.class);
    private static final long SWEEP_INTERVAL_MS = 30_000L;

    private static final Map<Integer, HabboItem> TRACKED = new ConcurrentHashMap<>();

    private volatile boolean disposed;

    public RentableFurnitureManager() {
        Emulator.getThreading().run(this, SWEEP_INTERVAL_MS);
    }

    /** Remembers an item that has a rent period; items without one are ignored. */
    public static void track(HabboItem item) {
        if (item == null || item.getId() <= 0 || !item.hasRentPeriod()) {
            return;
        }
        TRACKED.put(item.getId(), item);
    }

    public static void untrack(int itemId) {
        TRACKED.remove(itemId);
    }

    public static int trackedCount() {
        return TRACKED.size();
    }

    @Override
    public void run() {
        if (this.disposed) {
            return;
        }
        try {
            int now = Emulator.getIntUnixTimestamp();
            for (HabboItem item : TRACKED.values()) {
                if (!item.hasRentPeriod()) {
                    TRACKED.remove(item.getId());
                } else if (item.getExpiresTimestamp() <= now) {
                    expire(item);
                }
            }
        } catch (Exception e) {
            LOGGER.error("Rentable furniture sweep failed", e);
        } finally {
            if (!this.disposed) {
                Emulator.getThreading().run(this, SWEEP_INTERVAL_MS);
            }
        }
    }

    /** Removes an expired rental from wherever it is and deletes it. */
    public static void expire(HabboItem item) {
        TRACKED.remove(item.getId());

        Room room = item.getRoomId() > 0
                ? Emulator.getGameEnvironment().getRoomManager().getRoom(item.getRoomId())
                : null;
        if (room != null) {
            room.removeHabboItem(item);
            item.onPickUp(room);
            if (item.getBaseItem().getType() == FurnitureType.FLOOR) {
                room.sendComposer(new RemoveFloorItemComposer(item, true).compose());
            } else if (item.getBaseItem().getType() == FurnitureType.WALL) {
                room.sendComposer(new RemoveWallItemComposer(item).compose());
            }
        }

        Habbo owner = Emulator.getGameServer().getGameClientManager().getHabbo(item.getUserId());
        if (owner != null) {
            owner.getInventory().getItemsComponent().removeHabboItem(item);
            if (owner.getClient() != null) {
                owner.getClient().sendResponse(new RemoveHabboItemComposer(item.getId()));
            }
        }

        Emulator.getGameEnvironment().getItemManager().deleteItem(item);
    }

    /**
     * The catalog offer selling exactly {@code baseItem}: the rental offer when
     * {@code rentOffer} is set, the plain purchase offer otherwise. Offers the
     * client can open by id (have_offer) win over hidden ones.
     */
    public static CatalogItem findOffer(Item baseItem, boolean rentOffer) {
        if (baseItem == null) {
            return null;
        }
        CatalogItem hidden = null;
        for (CatalogPage page : Emulator.getGameEnvironment()
                .getCatalogManager()
                .getCatalogPagesMap(null)
                .values()) {
            for (CatalogItem offer : page.getCatalogItems().values()) {
                if (!sellsExactly(offer, baseItem, rentOffer)) {
                    continue;
                }
                if (offer.isHaveOffer()) {
                    return offer;
                }
                if (hidden == null) {
                    hidden = offer;
                }
            }
        }
        return hidden;
    }

    static boolean sellsExactly(CatalogItem offer, Item baseItem, boolean rentOffer) {
        if (offer == null || offer.isRentOffer() != rentOffer || offer.getAmount() != 1 || offer.isLimited()) {
            return false;
        }
        Map<Integer, Integer> bundle = offer.getBundle();
        return bundle != null && bundle.size() == 1 && bundle.containsKey(baseItem.getId());
    }

    /**
     * Charges the extension (rent offer) or the buy-out (purchase offer) of a
     * rented item the habbo owns and updates its expiry. Returns false when
     * there is no matching offer or the habbo cannot pay.
     */
    public static boolean extendOrBuyout(Habbo habbo, HabboItem item, boolean buyout) {
        if (habbo == null || item == null || !item.hasRentPeriod()) {
            return false;
        }
        if (item.getUserId() != habbo.getHabboInfo().getId()) {
            return false;
        }

        CatalogItem offer = findOffer(item.getBaseItem(), !buyout);
        if (offer == null) {
            return false;
        }
        if (!CatalogPaymentService.tryTake(habbo, offer.getCredits(), offer.getPointsType(), offer.getPoints())) {
            return false;
        }

        int now = Emulator.getIntUnixTimestamp();
        if (buyout) {
            item.setExpiresTimestamp(RentableFurniture.NEVER);
            TRACKED.remove(item.getId());
        } else {
            item.setExpiresTimestamp(RentableFurniture.extend(item.getExpiresTimestamp(), now, offer.getRentDays()));
            track(item);
        }
        item.needsUpdate(true);
        Emulator.getThreading().run(item);
        return true;
    }

    public void dispose() {
        this.disposed = true;
        TRACKED.clear();
    }
}
