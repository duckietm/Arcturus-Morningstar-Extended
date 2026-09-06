package com.eu.habbo.messages.incoming.camera;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.achievements.AchievementManager;
import com.eu.habbo.habbohotel.items.FurnitureType;
import com.eu.habbo.habbohotel.items.Item;
import com.eu.habbo.habbohotel.users.Habbo;
import com.eu.habbo.habbohotel.users.HabboInfo;
import com.eu.habbo.habbohotel.users.HabboItem;
import com.eu.habbo.messages.incoming.MessageHandler;
import com.eu.habbo.messages.outgoing.camera.CameraPurchaseSuccesfullComposer;
import com.eu.habbo.messages.outgoing.catalog.NotEnoughPointsTypeComposer;
import com.eu.habbo.messages.outgoing.inventory.AddHabboItemComposer;
import com.eu.habbo.messages.outgoing.inventory.InventoryRefreshComposer;
import com.eu.habbo.plugin.events.users.UserPurchasePictureEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Locale;

public class CameraPurchaseEvent extends MessageHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(CameraPurchaseEvent.class);

    public static int CAMERA_PURCHASE_CREDITS = 2;
    public static int CAMERA_PURCHASE_POINTS = 0;
    public static int CAMERA_PURCHASE_POINTS_TYPE = 5;
    private static final String PHOTO_SIZE_SMALL = "small";
    private static final String PHOTO_SIZE_LARGE = "large";

    @Override
    public void handle() {
        String requestedPhotoSize = this.packet.readString().trim().toLowerCase(Locale.ROOT);

        Habbo habbo = this.client.getHabbo();
        HabboInfo habboInfo = habbo.getHabboInfo();

        int purchaseCredits = purchaseCredits();
        int purchasePoints = purchasePoints();
        int purchasePointsType = purchasePointsType();

        if (habboInfo.getCredits() < purchaseCredits) {
            habbo.alert("You don't have enough credits!");
            this.client.sendResponse(new NotEnoughPointsTypeComposer(true, false, 0));
            return;
        }

        if (purchasePoints > 0 && habboInfo.getCurrencyAmount(purchasePointsType) < purchasePoints) {
            String alertMessage = "You don't have enough "
                    + Emulator.getTexts().getValue("seasonal.name." + purchasePointsType, "currency") + "!";
            habbo.alert(alertMessage);
            this.client.sendResponse(new NotEnoughPointsTypeComposer(false, true, purchasePointsType));
            return;
        }

        if (habboInfo.getPhotoTimestamp() == 0
                || habboInfo.getPhotoJSON() == null
                || habboInfo.getPhotoJSON().isEmpty()
                || !habboInfo.getPhotoJSON().contains(Integer.toString(habboInfo.getPhotoTimestamp()))) {
            LOGGER.warn(
                    "Camera purchase for {} aborted: no valid rendered photo in state (timestamp={}, jsonEmpty={}). Take a photo again before buying.",
                    habboInfo.getUsername(),
                    habboInfo.getPhotoTimestamp(),
                    habboInfo.getPhotoJSON() == null || habboInfo.getPhotoJSON().isEmpty());
            return;
        }

        if (habboInfo.getCurrentRoom() == null) {
            LOGGER.warn("Camera purchase for {} aborted: user is not in a room.", habboInfo.getUsername());
            return;
        }

        if (Emulator.getPluginManager()
                .fireEvent(new UserPurchasePictureEvent(
                        habbo,
                        habboInfo.getPhotoURL(),
                        habboInfo.getCurrentRoom().getId(),
                        habboInfo.getPhotoTimestamp()))
                .isCancelled()) return;

        int cameraItemId = getCameraItemId(requestedPhotoSize);
        Item item = Emulator.getGameEnvironment().getItemManager().getItem(cameraItemId);
        if (item == null
                || item.getType() != FurnitureType.WALL
                || !item.getInteractionType().getName().equals("external_image")) {
            LOGGER.warn(
                    "Camera purchase for {} aborted: requested size '{}' resolved to item {} which is {} (need a wall item with interaction type 'external_image').",
                    habboInfo.getUsername(),
                    requestedPhotoSize,
                    cameraItemId,
                    item == null
                            ? "not found"
                            : "interaction '" + item.getInteractionType().getName() + "'");
            habbo.alert(Emulator.getTexts().getValue("camera.error.creation"));
            return;
        }

        HabboItem photoItem = Emulator.getGameEnvironment()
                .getItemManager()
                .createItem(habboInfo.getId(), item, 0, 0, habboInfo.getPhotoJSON());
        if (photoItem == null) {
            LOGGER.warn(
                    "Camera purchase for {} aborted: createItem returned null for camera.item_id={}.",
                    habboInfo.getUsername(),
                    cameraItemId);
            habbo.alert(Emulator.getTexts().getValue("camera.error.creation"));
            return;
        }

        photoItem.setExtradata(photoItem.getExtradata().replace("%id%", Integer.toString(photoItem.getId())));
        photoItem.needsUpdate(true);
        habbo.getInventory().getItemsComponent().addItem(photoItem);

        this.client.sendResponse(new CameraPurchaseSuccesfullComposer());
        this.client.sendResponse(new AddHabboItemComposer(photoItem));
        this.client.sendResponse(new InventoryRefreshComposer());

        if (purchaseCredits > 0) habbo.giveCredits(-purchaseCredits);
        if (purchasePoints > 0) habbo.givePoints(purchasePointsType, -purchasePoints);

        AchievementManager.progressAchievement(
                habbo, Emulator.getGameEnvironment().getAchievementManager().getAchievement("CameraPhotoCount"));
    }

    /**
     * The camera.price.* settings are what RequestCameraConfigurationEvent advertises,
     * so the purchase must charge the same values (the statics stay as fallbacks).
     */
    public static int purchaseCredits() {
        return Emulator.getConfig().getInt("camera.price.credits", CAMERA_PURCHASE_CREDITS);
    }

    public static int purchasePoints() {
        return Emulator.getConfig().getInt("camera.price.points", CAMERA_PURCHASE_POINTS);
    }

    public static int purchasePointsType() {
        return Emulator.getConfig().getInt("camera.price.points.type", CAMERA_PURCHASE_POINTS_TYPE);
    }

    static int getCameraItemId(String requestedPhotoSize) {
        if (PHOTO_SIZE_SMALL.equals(requestedPhotoSize)) {
            return Emulator.getConfig().getInt("camera.item_id.small");
        }

        if (PHOTO_SIZE_LARGE.equals(requestedPhotoSize)) {
            return Emulator.getConfig().getInt("camera.item_id.large");
        }

        return Emulator.getConfig().getInt("camera.item_id");
    }
}
