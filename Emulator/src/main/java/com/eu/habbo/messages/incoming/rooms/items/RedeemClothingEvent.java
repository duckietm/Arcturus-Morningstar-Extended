package com.eu.habbo.messages.incoming.rooms.items;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.catalog.ClothItem;
import com.eu.habbo.habbohotel.items.interactions.InteractionClothing;
import com.eu.habbo.habbohotel.rooms.RoomTile;
import com.eu.habbo.habbohotel.users.HabboItem;
import com.eu.habbo.messages.incoming.MessageHandler;
import com.eu.habbo.messages.outgoing.generic.alerts.BubbleAlertComposer;
import com.eu.habbo.messages.outgoing.generic.alerts.BubbleAlertKeys;
import com.eu.habbo.messages.outgoing.rooms.UpdateStackHeightComposer;
import com.eu.habbo.messages.outgoing.rooms.items.RemoveFloorItemComposer;
import com.eu.habbo.messages.outgoing.users.UserClothesComposer;
import com.eu.habbo.threading.runnables.QueryDeleteHabboItem;
import lombok.extern.slf4j.Slf4j;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

@Slf4j
public class RedeemClothingEvent extends MessageHandler {
    @Override
    public void handle() throws Exception {
        int itemId = this.packet.readInt();

        if (this.client.getHabbo().getHabboInfo().getCurrentRoom() != null &&
                this.client.getHabbo().getHabboInfo().getCurrentRoom().hasRights(this.client.getHabbo())) {
            HabboItem item = this.client.getHabbo().getHabboInfo().getCurrentRoom().getHabboItem(itemId);

            if (item != null && item.getUserId() == this.client.getHabbo().getHabboInfo().getId()) {
                if (item instanceof InteractionClothing) {
                    ClothItem clothing = Emulator.getGameEnvironment().getCatalogManager().getClothing(item.getBaseItem().getName());

                    if (clothing != null) {
                        if (!this.client.getHabbo().getInventory().getWardrobeComponent().getClothing().contains(clothing.id)) {
                            item.setRoomId(0);
                            RoomTile tile = this.client.getHabbo().getHabboInfo().getCurrentRoom().getLayout().getTile(item.getX(), item.getY());
                            this.client.getHabbo().getHabboInfo().getCurrentRoom().removeHabboItem(item);
                            this.client.getHabbo().getHabboInfo().getCurrentRoom().updateTile(tile);
                            this.client.getHabbo().getHabboInfo().getCurrentRoom().sendComposer(new UpdateStackHeightComposer(tile.x, tile.y, tile.z, tile.relativeHeight()).compose());
                            this.client.getHabbo().getHabboInfo().getCurrentRoom().sendComposer(new RemoveFloorItemComposer(item, true).compose());
                            Emulator.getThreading().run(new QueryDeleteHabboItem(item.getId()));

                            try (Connection connection = Emulator.getDatabase().getDataSource().getConnection(); PreparedStatement statement = connection.prepareStatement("INSERT INTO users_clothing (user_id, clothing_id) VALUES (?, ?)")) {
                                statement.setInt(1, this.client.getHabbo().getHabboInfo().getId());
                                statement.setInt(2, clothing.id);
                                statement.execute();
                            } catch (SQLException e) {
                                log.error("Caught SQL exception", e);
                            }

                            this.client.getHabbo().getInventory().getWardrobeComponent().getClothing().add(clothing.id);
                            this.client.getHabbo().getInventory().getWardrobeComponent().getClothingSets().addAll(clothing.setId);
                            this.client.sendResponse(new UserClothesComposer(this.client.getHabbo()));
                            this.client.sendResponse(new BubbleAlertComposer(BubbleAlertKeys.FIGURESET_REDEEMED.key));

                        } else {
                            this.client.sendResponse(new BubbleAlertComposer(BubbleAlertKeys.FIGURESET_OWNED_ALREADY.key));
                        }
                    } else {
                        log.error("[Catalog] No definition in catalog_clothing found for clothing name " + item.getBaseItem().getName() + ". Could not redeem clothing!");
                    }
                }
            }
        }
    }
}
