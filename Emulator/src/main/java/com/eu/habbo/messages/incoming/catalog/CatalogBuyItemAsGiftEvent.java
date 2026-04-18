package com.eu.habbo.messages.incoming.catalog;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.achievements.AchievementManager;
import com.eu.habbo.habbohotel.catalog.CatalogItem;
import com.eu.habbo.habbohotel.catalog.CatalogLimitedConfiguration;
import com.eu.habbo.habbohotel.catalog.CatalogManager;
import com.eu.habbo.habbohotel.catalog.CatalogPage;
import com.eu.habbo.habbohotel.items.FurnitureType;
import com.eu.habbo.habbohotel.items.Item;
import com.eu.habbo.habbohotel.items.interactions.*;
import com.eu.habbo.habbohotel.modtool.ScripterManager;
import com.eu.habbo.habbohotel.permissions.Permission;
import com.eu.habbo.habbohotel.users.Habbo;
import com.eu.habbo.habbohotel.users.HabboBadge;
import com.eu.habbo.habbohotel.users.HabboItem;
import com.eu.habbo.messages.incoming.MessageHandler;
import com.eu.habbo.messages.outgoing.catalog.*;
import com.eu.habbo.messages.outgoing.generic.alerts.BubbleAlertComposer;
import com.eu.habbo.messages.outgoing.generic.alerts.BubbleAlertKeys;
import com.eu.habbo.messages.outgoing.generic.alerts.GenericAlertComposer;
import com.eu.habbo.messages.outgoing.generic.alerts.HotelWillCloseInMinutesComposer;
import com.eu.habbo.messages.outgoing.inventory.AddHabboItemComposer;
import com.eu.habbo.messages.outgoing.inventory.InventoryRefreshComposer;
import com.eu.habbo.threading.runnables.ShutdownEmulator;
import gnu.trove.map.hash.THashMap;
import gnu.trove.set.hash.THashSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Calendar;

public class CatalogBuyItemAsGiftEvent extends MessageHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(CatalogBuyItemAsGiftEvent.class);

    @Override
    public int getRatelimit() {
        return 500;
    }

    @Override
    public void handle() throws Exception {
        LOGGER.error("DEBUG GIFT: entered CatalogBuyItemAsGiftEvent.handle()");

        if (Emulator.getIntUnixTimestamp() - this.client.getHabbo().getHabboStats().lastGiftTimestamp >= CatalogManager.PURCHASE_COOLDOWN) {
            this.client.getHabbo().getHabboStats().lastGiftTimestamp = Emulator.getIntUnixTimestamp();

            if (ShutdownEmulator.timestamp > 0) {
                LOGGER.error("DEBUG GIFT: emulator closing");
                this.client.sendResponse(new HotelWillCloseInMinutesComposer((ShutdownEmulator.timestamp - Emulator.getIntUnixTimestamp()) / 60));
                this.client.sendResponse(new AlertPurchaseFailedComposer(AlertPurchaseFailedComposer.SERVER_ERROR).compose());
                return;
            }

            if (this.client.getHabbo().getHabboStats().isPurchasingFurniture) {
                LOGGER.error("DEBUG GIFT: isPurchasingFurniture already true");
                this.client.sendResponse(new AlertPurchaseFailedComposer(AlertPurchaseFailedComposer.SERVER_ERROR).compose());
                return;
            } else {
                this.client.getHabbo().getHabboStats().isPurchasingFurniture = true;
            }

            try {
                int pageId = this.packet.readInt();
                int itemId = this.packet.readInt();
                String extraData = this.packet.readString();
                String username = this.packet.readString();
                String message = Emulator.getGameEnvironment().getWordFilter().filter(this.packet.readString(), this.client.getHabbo());
                int spriteId = this.packet.readInt();
                int color = this.packet.readInt();
                int ribbonId = this.packet.readInt();
                boolean showName = this.packet.readBoolean();

                LOGGER.error(
                        "DEBUG GIFT: pageId={}, itemId={}, extraData={}, username={}, spriteId={}, color={}, ribbonId={}, showName={}, message={}",
                        pageId, itemId, extraData, username, spriteId, color, ribbonId, showName, message
                );

                int userId = 0;

                if (!Emulator.getGameEnvironment().getCatalogManager().giftWrappers.containsKey(spriteId)
                        && !Emulator.getGameEnvironment().getCatalogManager().giftFurnis.containsKey(spriteId)) {
                    LOGGER.error("DEBUG GIFT: invalid spriteId for gift wrapper/furni -> {}", spriteId);
                    this.client.sendResponse(new AlertPurchaseFailedComposer(AlertPurchaseFailedComposer.SERVER_ERROR).compose());
                    return;
                }

                if (!GiftConfigurationComposer.BOX_TYPES.contains(color) || !GiftConfigurationComposer.RIBBON_TYPES.contains(ribbonId)) {
                    LOGGER.error("DEBUG GIFT: invalid color/ribbon -> color={}, ribbonId={}", color, ribbonId);
                    this.client.sendResponse(new AlertPurchaseFailedComposer(AlertPurchaseFailedComposer.SERVER_ERROR).compose());
                    return;
                }

                if (message.length() > Emulator.getConfig().getInt("hotel.gifts.length.max", 300)) {
                    message = message.substring(0, Emulator.getConfig().getInt("hotel.gifts.length.max", 300));
                }

                Integer iItemId = Emulator.getGameEnvironment().getCatalogManager().giftWrappers.get(spriteId);

                if (iItemId == null) {
                    iItemId = Emulator.getGameEnvironment().getCatalogManager().giftFurnis.get(spriteId);
                }

                if (iItemId == null) {
                    LOGGER.error("DEBUG GIFT: iItemId null for spriteId={}", spriteId);
                    this.client.sendResponse(new AlertPurchaseFailedComposer(AlertPurchaseFailedComposer.SERVER_ERROR).compose());
                    return;
                }

                Item giftItem = Emulator.getGameEnvironment().getItemManager().getItem(iItemId);

                if (giftItem == null) {
                    LOGGER.error("DEBUG GIFT: direct giftItem null, trying random fallback. iItemId={}", iItemId);
                    giftItem = Emulator.getGameEnvironment().getItemManager().getItem(
                            (Integer) Emulator.getGameEnvironment().getCatalogManager().giftFurnis.values().toArray()[
                                    Emulator.getRandom().nextInt(Emulator.getGameEnvironment().getCatalogManager().giftFurnis.size())
                                    ]
                    );

                    if (giftItem == null) {
                        LOGGER.error("DEBUG GIFT: fallback giftItem also null");
                        this.client.sendResponse(new AlertPurchaseFailedComposer(AlertPurchaseFailedComposer.SERVER_ERROR).compose());
                        return;
                    }
                }

                try (Connection connection = Emulator.getDatabase().getDataSource().getConnection()) {
                    Habbo habbo = Emulator.getGameEnvironment().getHabboManager().getHabbo(username);

                    if (habbo == null) {
                        LOGGER.error("DEBUG GIFT: target user not online, checking DB -> {}", username);
                        try (PreparedStatement statement = connection.prepareStatement("SELECT id FROM users WHERE username = ?")) {
                            statement.setString(1, username);

                            try (ResultSet set = statement.executeQuery()) {
                                if (set.next()) {
                                    userId = set.getInt(1);
                                }
                            }
                        } catch (SQLException e) {
                            LOGGER.error("Caught SQL exception", e);
                        }
                    } else {
                        userId = habbo.getHabboInfo().getId();
                    }

                    if (userId == 0) {
                        LOGGER.error("DEBUG GIFT: receiver not found -> {}", username);
                        this.client.sendResponse(new GiftReceiverNotFoundComposer());
                        return;
                    }

                    CatalogPage page = Emulator.getGameEnvironment().getCatalogManager().catalogPages.get(pageId);

                    if (page == null) {
                        LOGGER.error("DEBUG GIFT: page null -> {}", pageId);
                        this.client.sendResponse(new AlertPurchaseFailedComposer(AlertPurchaseFailedComposer.SERVER_ERROR).compose());
                        return;
                    }

                    if (page.getRank() > this.client.getHabbo().getHabboInfo().getRank().getId() || !page.isEnabled() || !page.isVisible()) {
                        LOGGER.error("DEBUG GIFT: page access denied. pageRank={}, userRank={}, enabled={}, visible={}",
                                page.getRank(),
                                this.client.getHabbo().getHabboInfo().getRank().getId(),
                                page.isEnabled(),
                                page.isVisible());
                        this.client.sendResponse(new AlertPurchaseUnavailableComposer(AlertPurchaseUnavailableComposer.ILLEGAL));
                        return;
                    }

                    CatalogItem item = page.getCatalogItem(itemId);

                    if (item == null) {
                        LOGGER.error("DEBUG GIFT: catalog item null -> {}", itemId);
                        this.client.sendResponse(new AlertPurchaseFailedComposer(AlertPurchaseFailedComposer.SERVER_ERROR).compose());
                        return;
                    }

                    if (item.isClubOnly() && !this.client.getHabbo().getHabboStats().hasActiveClub()) {
                        LOGGER.error("DEBUG GIFT: item requires club -> itemId={}", itemId);
                        this.client.sendResponse(new AlertPurchaseUnavailableComposer(AlertPurchaseUnavailableComposer.REQUIRES_CLUB));
                        return;
                    }

                    for (Item baseItem : item.getBaseItems()) {
                        if (!baseItem.allowGift()) {
                            LOGGER.error("DEBUG GIFT: base item not giftable -> baseItemId={}, name={}", baseItem.getId(), baseItem.getName());
                            this.client.sendResponse(new AlertPurchaseUnavailableComposer(AlertPurchaseUnavailableComposer.ILLEGAL));
                            return;
                        }
                    }

                    if (item.isLimited()) {
                        if (item.getLimitedStack() == item.getLimitedSells()) {
                            LOGGER.error("DEBUG GIFT: LTD sold out -> itemId={}", itemId);
                            this.client.sendResponse(new AlertLimitedSoldOutComposer());
                            return;
                        }
                    }

                    int totalCredits = item.getCredits();
                    int totalPoints = item.getPoints();

                    if (totalCredits > this.client.getHabbo().getHabboInfo().getCredits()
                            || totalPoints > this.client.getHabbo().getHabboInfo().getCurrencyAmount(item.getPointsType())) {
                        LOGGER.error("DEBUG GIFT: not enough currency. creditsNeeded={}, creditsHave={}, pointsNeeded={}, pointsHave={}, pointsType={}",
                                totalCredits,
                                this.client.getHabbo().getHabboInfo().getCredits(),
                                totalPoints,
                                this.client.getHabbo().getHabboInfo().getCurrencyAmount(item.getPointsType()),
                                item.getPointsType());
                        this.client.sendResponse(new AlertPurchaseUnavailableComposer(AlertPurchaseUnavailableComposer.ILLEGAL));
                        return;
                    }

                    CatalogLimitedConfiguration limitedConfiguration = null;
                    int limitedStack = 0;
                    int limitedNumber = 0;

                    if (item.isLimited()) {
                        if (Emulator.getGameEnvironment().getCatalogManager().getLimitedConfig(item).available() == 0) {
                            LOGGER.error("DEBUG GIFT: LTD available=0 -> itemId={}", itemId);
                            this.client.sendResponse(new AlertLimitedSoldOutComposer());
                            return;
                        }

                        if (Emulator.getConfig().getBoolean("hotel.catalog.ltd.limit.enabled")) {
                            int ltdLimit = Emulator.getConfig().getInt("hotel.purchase.ltd.limit.daily.total");
                            if (this.client.getHabbo().getHabboStats().totalLtds() >= ltdLimit) {
                                LOGGER.error("DEBUG GIFT: sender reached daily total LTD limit");
                                this.client.getHabbo().alert(
                                        Emulator.getTexts().getValue("error.catalog.buy.limited.daily.total")
                                                .replace("%itemname%", item.getBaseItems().iterator().next().getFullName())
                                                .replace("%limit%", ltdLimit + "")
                                );
                                return;
                            }

                            ltdLimit = Emulator.getConfig().getInt("hotel.purchase.ltd.limit.daily.item");
                            if (this.client.getHabbo().getHabboStats().totalLtds(item.getId()) >= ltdLimit) {
                                LOGGER.error("DEBUG GIFT: sender reached daily LTD item limit");
                                this.client.getHabbo().alert(
                                        Emulator.getTexts().getValue("error.catalog.buy.limited.daily.item")
                                                .replace("%itemname%", item.getBaseItems().iterator().next().getFullName())
                                                .replace("%limit%", ltdLimit + "")
                                );
                                return;
                            }
                        }

                        limitedConfiguration = Emulator.getGameEnvironment().getCatalogManager().getLimitedConfig(item);

                        if (limitedConfiguration == null) {
                            limitedConfiguration = Emulator.getGameEnvironment().getCatalogManager().createOrUpdateLimitedConfig(item);
                        }

                        limitedNumber = limitedConfiguration.getNumber();
                        limitedStack = limitedConfiguration.getTotalSet();
                        this.client.getHabbo().getHabboStats().addLtdLog(item.getId(), Emulator.getIntUnixTimestamp());
                    }

                    THashSet<HabboItem> itemsList = new THashSet<>();

                    boolean badgeFound = false;
                    for (Item baseItem : item.getBaseItems()) {
                        if (baseItem.getType() == FurnitureType.BADGE) {
                            if (habbo != null) {
                                if (habbo.getInventory().getBadgesComponent().hasBadge(baseItem.getName())) {
                                    badgeFound = true;
                                }
                            } else {
                                int c = 0;
                                try (PreparedStatement statement = connection.prepareStatement("SELECT COUNT(*) as c FROM users_badges WHERE user_id = ? AND badge_code LIKE ?")) {
                                    statement.setInt(1, userId);
                                    statement.setString(2, baseItem.getName());

                                    try (ResultSet rSet = statement.executeQuery()) {
                                        if (rSet.next()) {
                                            c = rSet.getInt("c");
                                        }
                                    }
                                }

                                if (c != 0) {
                                    badgeFound = true;
                                }
                            }
                        }
                    }

                    if (badgeFound) {
                        LOGGER.error("DEBUG GIFT: receiver already has badge");
                        this.client.sendResponse(new AlertPurchaseFailedComposer(AlertPurchaseFailedComposer.ALREADY_HAVE_BADGE));
                        return;
                    }

                    if (item.getAmount() > 1 || item.getBaseItems().size() > 1) {
                        LOGGER.error("DEBUG GIFT: unsupported multi amount/baseItems. amount={}, baseItems={}", item.getAmount(), item.getBaseItems().size());
                        this.client.sendResponse(new AlertPurchaseFailedComposer(AlertPurchaseFailedComposer.SERVER_ERROR).compose());
                        return;
                    }

                    for (Item baseItem : item.getBaseItems()) {
                        if (item.getItemAmount(baseItem.getId()) > 1) {
                            LOGGER.error("DEBUG GIFT: unsupported item amount > 1 for baseItemId={}", baseItem.getId());
                            this.client.sendResponse(new AlertPurchaseFailedComposer(AlertPurchaseFailedComposer.SERVER_ERROR).compose());
                            return;
                        }

                        for (int k = 0; k < item.getItemAmount(baseItem.getId()); k++) {
                            if (!baseItem.getName().contains("avatar_effect")) {
                                if (baseItem.getType() == FurnitureType.BADGE) {
                                    if (!badgeFound) {
                                        if (habbo != null) {
                                            HabboBadge badge = new HabboBadge(0, baseItem.getName(), 0, habbo);
                                            Emulator.getThreading().run(badge);
                                            habbo.getInventory().getBadgesComponent().addBadge(badge);
                                        } else {
                                            try (PreparedStatement statement = connection.prepareStatement("INSERT INTO users_badges (user_id, badge_code) VALUES (?, ?)")) {
                                                statement.setInt(1, userId);
                                                statement.setString(2, baseItem.getName());
                                                statement.execute();
                                            }
                                        }

                                        badgeFound = true;
                                    }
                                } else if (item.getName().startsWith("rentable_bot_")) {
                                    LOGGER.error("DEBUG GIFT: rentable bot gifts not supported");
                                    this.client.sendResponse(new AlertPurchaseFailedComposer(AlertPurchaseFailedComposer.SERVER_ERROR).compose());
                                    return;
                                } else if (Item.isPet(baseItem)) {
                                    LOGGER.error("DEBUG GIFT: pet gifts not supported");
                                    this.client.sendResponse(new AlertPurchaseFailedComposer(AlertPurchaseFailedComposer.SERVER_ERROR).compose());
                                    return;
                                } else {
                                    if (baseItem.getInteractionType().getType() == InteractionTrophy.class
                                            || baseItem.getInteractionType().getType() == InteractionBadgeDisplay.class) {
                                        if (baseItem.getInteractionType().getType() == InteractionBadgeDisplay.class
                                                && habbo != null
                                                && !habbo.getClient().getHabbo().getInventory().getBadgesComponent().hasBadge(extraData)) {
                                            ScripterManager.scripterDetected(
                                                    habbo.getClient(),
                                                    Emulator.getTexts().getValue("scripter.warning.catalog.badge_display")
                                                            .replace("%username%", habbo.getClient().getHabbo().getHabboInfo().getUsername())
                                                            .replace("%badge%", extraData)
                                            );
                                            extraData = "UMAD";
                                        }

                                        extraData = this.client.getHabbo().getHabboInfo().getUsername()
                                                + (char) 9
                                                + Calendar.getInstance().get(Calendar.DAY_OF_MONTH)
                                                + "-"
                                                + (Calendar.getInstance().get(Calendar.MONTH) + 1)
                                                + "-"
                                                + Calendar.getInstance().get(Calendar.YEAR)
                                                + (char) 9
                                                + extraData;
                                    }

                                    if (baseItem.getInteractionType().getType() == InteractionTeleport.class
                                            || baseItem.getInteractionType().getType() == InteractionTeleportTile.class) {

                                        HabboItem teleportOne = Emulator.getGameEnvironment().getItemManager().createItem(userId, baseItem, limitedStack, limitedNumber, extraData);
                                        HabboItem teleportTwo = Emulator.getGameEnvironment().getItemManager().createItem(userId, baseItem, limitedStack, limitedNumber, extraData);

                                        if (teleportOne == null || teleportTwo == null) {
                                            LOGGER.error("DEBUG GIFT: teleport creation failed. baseItemId={}, teleportOneNull={}, teleportTwoNull={}",
                                                    baseItem.getId(), teleportOne == null, teleportTwo == null);
                                            this.client.sendResponse(new AlertPurchaseFailedComposer(AlertPurchaseFailedComposer.SERVER_ERROR));
                                            return;
                                        }

                                        Emulator.getGameEnvironment().getItemManager().insertTeleportPair(teleportOne.getId(), teleportTwo.getId());
                                        itemsList.add(teleportOne);
                                        itemsList.add(teleportTwo);

                                    } else if (baseItem.getInteractionType().getType() == InteractionHopper.class) {
                                        HabboItem habboItem = Emulator.getGameEnvironment().getItemManager().createItem(userId, baseItem, limitedNumber, limitedNumber, extraData);

                                        if (habboItem == null) {
                                            LOGGER.error("DEBUG GIFT: hopper creation failed. baseItemId={}", baseItem.getId());
                                            this.client.sendResponse(new AlertPurchaseFailedComposer(AlertPurchaseFailedComposer.SERVER_ERROR));
                                            return;
                                        }

                                        Emulator.getGameEnvironment().getItemManager().insertHopper(habboItem);
                                        itemsList.add(habboItem);

                                    } else if (baseItem.getInteractionType().getType() == InteractionGuildFurni.class
                                            || baseItem.getInteractionType().getType() == InteractionGuildGate.class) {
                                        HabboItem createdItem = Emulator.getGameEnvironment().getItemManager().createItem(userId, baseItem, limitedStack, limitedNumber, extraData);

                                        if (createdItem == null) {
                                            LOGGER.error("DEBUG GIFT: guild item creation failed. baseItemId={}", baseItem.getId());
                                            this.client.sendResponse(new AlertPurchaseFailedComposer(AlertPurchaseFailedComposer.SERVER_ERROR));
                                            return;
                                        }

                                        if (!(createdItem instanceof InteractionGuildFurni)) {
                                            LOGGER.error("DEBUG GIFT: created guild item has wrong class -> {}", createdItem.getClass().getName());
                                            this.client.sendResponse(new AlertPurchaseFailedComposer(AlertPurchaseFailedComposer.SERVER_ERROR));
                                            return;
                                        }

                                        InteractionGuildFurni habboItem = (InteractionGuildFurni) createdItem;
                                        habboItem.setExtradata("");
                                        habboItem.needsUpdate(true);

                                        int guildId;
                                        try {
                                            guildId = Integer.parseInt(extraData);
                                        } catch (Exception e) {
                                            LOGGER.error("Caught exception", e);
                                            this.client.sendResponse(new AlertPurchaseFailedComposer(AlertPurchaseFailedComposer.SERVER_ERROR));
                                            return;
                                        }

                                        Emulator.getThreading().run(habboItem);
                                        Emulator.getGameEnvironment().getGuildManager().setGuild(habboItem, guildId);
                                        itemsList.add(habboItem);
                                    } else {
                                        HabboItem habboItem = Emulator.getGameEnvironment().getItemManager().createItem(userId, baseItem, limitedStack, limitedNumber, extraData);

                                        if (habboItem == null) {
                                            LOGGER.error("DEBUG GIFT: normal item creation failed. baseItemId={}, baseItemName={}", baseItem.getId(), baseItem.getName());
                                            this.client.sendResponse(new AlertPurchaseFailedComposer(AlertPurchaseFailedComposer.SERVER_ERROR));
                                            return;
                                        }

                                        itemsList.add(habboItem);
                                    }
                                }
                            } else {
                                LOGGER.error("DEBUG GIFT: avatar_effect not supported");
                                this.client.sendResponse(new AlertPurchaseFailedComposer(AlertPurchaseFailedComposer.SERVER_ERROR));
                                this.client.sendResponse(new GenericAlertComposer(Emulator.getTexts().getValue("error.catalog.buy.not_yet")));
                                return;
                            }
                        }
                    }

                    if (itemsList.isEmpty()) {
                        LOGGER.error("DEBUG GIFT: itemsList empty before giftData");
                        this.client.sendResponse(new AlertPurchaseFailedComposer(AlertPurchaseFailedComposer.SERVER_ERROR));
                        return;
                    }

                    StringBuilder giftData = new StringBuilder(itemsList.size() + "\t");

                    for (HabboItem i : itemsList) {
                        if (i == null) {
                            LOGGER.error("DEBUG GIFT: null HabboItem detected inside itemsList");
                            this.client.sendResponse(new AlertPurchaseFailedComposer(AlertPurchaseFailedComposer.SERVER_ERROR));
                            return;
                        }

                        giftData.append(i.getId()).append("\t");
                    }

                    giftData.append(color)
                            .append("\t")
                            .append(ribbonId)
                            .append("\t")
                            .append(showName ? "1" : "0")
                            .append("\t")
                            .append(message.replace("\t", ""))
                            .append("\t")
                            .append(this.client.getHabbo().getHabboInfo().getUsername())
                            .append("\t")
                            .append(this.client.getHabbo().getHabboInfo().getLook());

                    HabboItem gift = Emulator.getGameEnvironment().getItemManager().createGift(username, giftItem, giftData.toString(), 0, 0);

                    if (gift == null) {
                        LOGGER.error("DEBUG GIFT: createGift returned null");
                        this.client.sendResponse(new AlertPurchaseFailedComposer(AlertPurchaseFailedComposer.SERVER_ERROR));
                        return;
                    }

                    if (limitedConfiguration != null) {
                        for (HabboItem itm : itemsList) {
                            if (itm == null) {
                                LOGGER.error("DEBUG GIFT: null item before limitedSold()");
                                this.client.sendResponse(new AlertPurchaseFailedComposer(AlertPurchaseFailedComposer.SERVER_ERROR));
                                return;
                            }
                            limitedConfiguration.limitedSold(item.getId(), this.client.getHabbo(), itm);
                        }
                    }

                    if (this.client.getHabbo().getHabboInfo().getId() != userId) {
                        AchievementManager.progressAchievement(
                                this.client.getHabbo(),
                                Emulator.getGameEnvironment().getAchievementManager().getAchievement("GiftGiver")
                        );
                    }

                    if (habbo != null) {
                        habbo.getClient().sendResponse(new AddHabboItemComposer(gift));
                        habbo.getClient().getHabbo().getInventory().getItemsComponent().addItem(gift);
                        habbo.getClient().sendResponse(new InventoryRefreshComposer());

                        THashMap<String, String> keys = new THashMap<>();
                        keys.put("display", "BUBBLE");
                        keys.put("image", "${image.library.url}notifications/gift.gif");
                        keys.put("message", Emulator.getTexts().getValue("generic.gift.received.anonymous"));

                        if (showName) {
                            keys.put("message", Emulator.getTexts().getValue("generic.gift.received")
                                    .replace("%username%", this.client.getHabbo().getHabboInfo().getUsername()));
                        }

                        habbo.getClient().sendResponse(new BubbleAlertComposer(BubbleAlertKeys.RECEIVED_BADGE.key, keys));
                    }

                    if (this.client.getHabbo().getHabboInfo().getId() != userId) {
                        AchievementManager.progressAchievement(
                                userId,
                                Emulator.getGameEnvironment().getAchievementManager().getAchievement("GiftReceiver")
                        );
                    }

                    if (!this.client.getHabbo().hasPermission(Permission.ACC_INFINITE_CREDITS)) {
                        if (totalCredits > 0) {
                            this.client.getHabbo().giveCredits(-totalCredits);
                        }
                    }

                    if (totalPoints > 0) {
                        if (item.getPointsType() == 0 && !this.client.getHabbo().hasPermission(Permission.ACC_INFINITE_PIXELS)) {
                            this.client.getHabbo().givePixels(-totalPoints);
                        } else if (!this.client.getHabbo().hasPermission(Permission.ACC_INFINITE_POINTS)) {
                            this.client.getHabbo().givePoints(item.getPointsType(), -totalPoints);
                        }
                    }

                    LOGGER.error("DEBUG GIFT: success sending PurchaseOKComposer");
                    this.client.sendResponse(new PurchaseOKComposer(item));
                }
            } catch (Exception e) {
                LOGGER.error("Exception caught", e);
                this.client.sendResponse(new AlertPurchaseFailedComposer(AlertPurchaseFailedComposer.SERVER_ERROR));
            } finally {
                this.client.getHabbo().getHabboStats().isPurchasingFurniture = false;
            }
        } else {
            LOGGER.error("DEBUG GIFT: cooldown blocked purchase");
            this.client.sendResponse(new AlertPurchaseFailedComposer(AlertPurchaseFailedComposer.SERVER_ERROR));
        }
    }
}