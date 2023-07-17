package com.eu.habbo.messages.incoming.rooms.pets;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.items.Item;
import com.eu.habbo.habbohotel.pets.MonsterplantPet;
import com.eu.habbo.habbohotel.pets.Pet;
import com.eu.habbo.habbohotel.rooms.Room;
import com.eu.habbo.habbohotel.users.HabboItem;
import com.eu.habbo.messages.incoming.MessageHandler;
import com.eu.habbo.messages.outgoing.rooms.items.AddFloorItemComposer;
import lombok.extern.slf4j.Slf4j;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

@Slf4j
public class CompostMonsterplantEvent extends MessageHandler {

    @Override
    public void handle() throws Exception {
        int petId = this.packet.readInt();

        Room room = this.client.getHabbo().getHabboInfo().getCurrentRoom();
        Pet pet = room.getPet(petId);

        if (pet != null) {
            if (pet instanceof MonsterplantPet) {
                if (pet.getUserId() == this.client.getHabbo().getHabboInfo().getId()) {
                    if (((MonsterplantPet) pet).isDead()) {
                        Item baseItem = Emulator.getGameEnvironment().getItemManager().getItem("mnstr_compost");

                        if (baseItem != null) {
                            HabboItem compost = Emulator.getGameEnvironment().getItemManager().createItem(pet.getUserId(), baseItem, 0, 0, "");
                            compost.setX(pet.getRoomUnit().getX());
                            compost.setY(pet.getRoomUnit().getY());
                            compost.setZ(pet.getRoomUnit().getZ());
                            compost.setRotation(pet.getRoomUnit().getBodyRotation().getValue());
                            room.addHabboItem(compost);
                            room.sendComposer(new AddFloorItemComposer(compost, this.client.getHabbo().getHabboInfo().getUsername()).compose());
                        }

                        pet.removeFromRoom();
                        try (Connection connection = Emulator.getDatabase().getDataSource().getConnection(); PreparedStatement statement = connection.prepareStatement("DELETE FROM users_pets WHERE id = ? LIMIT 1")) {
                            statement.setInt(1, pet.getId());
                            statement.executeUpdate();
                        } catch (SQLException e) {
                            log.error("Caught SQL exception", e);
                        }
                    }
                }
            }
        }
    }
}