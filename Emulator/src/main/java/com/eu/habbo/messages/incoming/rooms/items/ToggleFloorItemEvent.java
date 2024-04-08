package com.eu.habbo.messages.incoming.rooms.items;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.items.interactions.InteractionDice;
import com.eu.habbo.habbohotel.items.interactions.InteractionWired;
import com.eu.habbo.habbohotel.items.interactions.pets.InteractionMonsterPlantSeed;
import com.eu.habbo.habbohotel.pets.MonsterplantPet;
import com.eu.habbo.habbohotel.rooms.Room;
import com.eu.habbo.habbohotel.users.HabboItem;
import com.eu.habbo.messages.incoming.MessageHandler;
import com.eu.habbo.messages.outgoing.rooms.items.RemoveFloorItemComposer;
import com.eu.habbo.messages.outgoing.rooms.pets.PetPackageComposer;
import com.eu.habbo.messages.outgoing.rooms.users.RoomUserStatusComposer;
import com.eu.habbo.plugin.Event;
import com.eu.habbo.plugin.events.furniture.FurniturePickedUpEvent;
import com.eu.habbo.plugin.events.furniture.FurnitureToggleEvent;
import com.eu.habbo.threading.runnables.QueryDeleteHabboItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ToggleFloorItemEvent extends MessageHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(ToggleFloorItemEvent.class);

    @Override
    public void handle() throws Exception {
        try {
            Room room = this.client.getHabbo().getHabboInfo().getCurrentRoom();

            if (room == null)
                return;

            int itemId = this.packet.readInt();
            int state = this.packet.readInt();

            HabboItem item = room.getHabboItem(itemId);

            if (item == null || item instanceof InteractionDice)
                return;

            Event furnitureToggleEvent = new FurnitureToggleEvent(item, this.client.getHabbo(), state);
            Emulator.getPluginManager().fireEvent(furnitureToggleEvent);

            if (furnitureToggleEvent.isCancelled())
                return;

            /*
            if (item.getBaseItem().getName().equalsIgnoreCase("totem_planet")) {
                THashSet<HabboItem> items = room.getItemsAt(room.getLayout().getTile(item.getX(), item.getY()));
                HabboItem totemLeg = null;
                HabboItem totemHead = null;

                for (HabboItem totemItem : items) {
                    if (totemLeg != null && totemHead != null) {
                        break;
                    }
                    if (totemItem.getBaseItem().getName().equalsIgnoreCase("totem_leg")) {
                        totemLeg = totemItem;
                    }
                    if (totemItem.getBaseItem().getName().equalsIgnoreCase("totem_head")) {
                        totemHead = totemItem;
                    }
                }

                if (totemHead != null && totemLeg != null) {
                    if (item.getExtradata().equals("2")) {
                        if (totemLeg.getExtradata() == null || totemHead.getExtradata() == null)
                            return;

                        if (totemLeg.getExtradata().equals("2") && totemHead.getExtradata().equals("5")) {
                            room.giveEffect(this.client.getHabbo(), 23, -1);
                            return;
                        }

                        if (totemLeg.getExtradata().equals("10") && totemHead.getExtradata().equals("9")) {
                            room.giveEffect(this.client.getHabbo(), 26, -1);
                            return;
                        }
                    } else if (item.getExtradata().equals("0")) {
                        if (totemLeg.getExtradata().equals("7") && totemHead.getExtradata().equals("10")) {
                            room.giveEffect(this.client.getHabbo(), 24, -1);
                            return;
                        }

                    } else if (item.getExtradata().equals("1")) {
                        if (totemLeg.getExtradata().equals("9") && totemHead.getExtradata().equals("12")) {
                            room.giveEffect(this.client.getHabbo(), 25, -1);
                            return;
                        }
                    }
                }
            }*/

            //Do not move to onClick(). Wired could trigger it.
            if (item instanceof InteractionMonsterPlantSeed) {
                Emulator.getThreading().run(new QueryDeleteHabboItem(item.getId()));
                int rarity = 0;

                boolean isRare = item.getBaseItem().getName().contains("rare");

                if ((!item.getExtradata().isEmpty() && Integer.valueOf(item.getExtradata()) - 1 < 0) || item.getExtradata().isEmpty()) {
                    rarity = isRare ? InteractionMonsterPlantSeed.randomGoldenRarityLevel() : InteractionMonsterPlantSeed.randomRarityLevel();
                }
                else {
                    try {
                        rarity = Integer.valueOf(item.getExtradata()) - 1;
                    } catch (Exception e) {
                    }
                }
                MonsterplantPet pet = Emulator.getGameEnvironment().getPetManager().createMonsterplant(room, this.client.getHabbo(), isRare, room.getLayout().getTile(item.getX(), item.getY()), rarity);
                room.sendComposer(new RemoveFloorItemComposer(item, true).compose());
                room.removeHabboItem(item);
                room.updateTile(room.getLayout().getTile(item.getX(), item.getY()));
                room.placePet(pet, item.getX(), item.getY(), item.getZ(), item.getRotation());
                pet.cycle();
                room.sendComposer(new RoomUserStatusComposer(pet.getRoomUnit()).compose());
                return;
            }

            if (
                    (item.getBaseItem().getName().equalsIgnoreCase("val11_present") ||
                            item.getBaseItem().getName().equalsIgnoreCase("gnome_box") ||
                            item.getBaseItem().getName().equalsIgnoreCase("leprechaun_box") ||
                            item.getBaseItem().getName().equalsIgnoreCase("velociraptor_egg") ||
                            item.getBaseItem().getName().equalsIgnoreCase("pterosaur_egg") ||
                            item.getBaseItem().getName().equalsIgnoreCase("petbox_epic")) && room.getCurrentPets().size() < Room.MAXIMUM_PETS) {
                this.client.sendResponse(new PetPackageComposer(item));
                return;
            }

            item.onClick(this.client, room, new Object[]{state});

            if (item instanceof InteractionWired) {
                this.client.getHabbo().getRoomUnit().setGoalLocation(this.client.getHabbo().getRoomUnit().getCurrentLocation());
            }
        } catch (Exception e) {
            LOGGER.error("Caught exception", e);
        }
    }
}
