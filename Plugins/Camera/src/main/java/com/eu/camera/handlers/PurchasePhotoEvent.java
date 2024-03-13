package com.eu.camera.handlers;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.achievements.AchievementManager;
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

public class PurchasePhotoEvent extends MessageHandler {
    public static final int CAMERA_PURCHASE_CREDITS = Emulator.getConfig().getInt("camera.price.credits", 2);
    public static final int CAMERA_PURCHASE_POINTS = Emulator.getConfig().getInt("camera.price.points", 0);
    public static final int CAMERA_PURCHASE_POINTS_TYPE = Emulator.getConfig().getInt("camera.price.points.type", 5);
    public static final int CAMERA_ITEM_ID = Emulator.getConfig().getInt("camera.item_id");
    public static final String EXTERNAL_IMAGE_INTERACTION = "external_image";

    @Override
    public void handle() {
        Habbo habbo = this.client.getHabbo();
        HabboInfo habboInfo = habbo.getHabboInfo();

        if (habboInfo.getCredits() < CAMERA_PURCHASE_CREDITS) {
            handleInsufficientCredits(habbo);
            return;
        }

        if (habboInfo.getCurrencyAmount(CAMERA_PURCHASE_POINTS_TYPE) < CAMERA_PURCHASE_POINTS) {
            handleInsufficientPoints(habbo);
            return;
        }

        if (!isValidPhoto(habboInfo)) {
            return;
        }

        if (Emulator.getPluginManager().fireEvent(new UserPurchasePictureEvent(habbo, habboInfo.getPhotoURL(), habboInfo.getCurrentRoom().getId(), habboInfo.getPhotoTimestamp())).isCancelled()) {
            return;
        }

        Item item = Emulator.getGameEnvironment().getItemManager().getItem(CAMERA_ITEM_ID);
        if (item == null || !item.getInteractionType().getName().equals(EXTERNAL_IMAGE_INTERACTION)) {
            return;
        }

        handlePurchasedPhoto(habbo, habboInfo, item);
    }

    private void handleInsufficientCredits(Habbo habbo) {
        habbo.alert("You don't have enough credits!");
        this.client.sendResponse(new NotEnoughPointsTypeComposer(true, false, 0));
    }

    private void handleInsufficientPoints(Habbo habbo) {
        String alertMessage = "You don't have enough " + Emulator.getTexts().getValue("seasonal.name." + Integer.toString(CAMERA_PURCHASE_POINTS_TYPE), "currency") + "!";
        habbo.alert(alertMessage);
        this.client.sendResponse(new NotEnoughPointsTypeComposer(false, true, CAMERA_PURCHASE_POINTS_TYPE));
    }

    private boolean isValidPhoto(HabboInfo habboInfo) {
        return habboInfo.getPhotoTimestamp() != 0 && !habboInfo.getPhotoJSON().isEmpty() && habboInfo.getPhotoJSON().contains(Integer.toString(habboInfo.getPhotoTimestamp()));
    }

    private void handlePurchasedPhoto(Habbo habbo, HabboInfo habboInfo, Item item) {
        HabboItem photoItem = Emulator.getGameEnvironment().getItemManager().createItem(habboInfo.getId(), item, 0, 0, habboInfo.getPhotoJSON());

        if (photoItem != null) {
            photoItem.setExtradata(photoItem.getExtradata().replace("%id%", Integer.toString(photoItem.getId())));
            photoItem.needsUpdate(true);

            habbo.getInventory().getItemsComponent().addItem(photoItem);

            this.client.sendResponse(new CameraPurchaseSuccesfullComposer());
            this.client.sendResponse(new AddHabboItemComposer(photoItem));
            this.client.sendResponse(new InventoryRefreshComposer());

            habbo.giveCredits(-CAMERA_PURCHASE_CREDITS);
            habbo.givePoints(CAMERA_PURCHASE_POINTS_TYPE, -CAMERA_PURCHASE_POINTS);
            AchievementManager.progressAchievement(habbo, Emulator.getGameEnvironment().getAchievementManager().getAchievement("CameraPhotoCount"));
        }
    }
}
