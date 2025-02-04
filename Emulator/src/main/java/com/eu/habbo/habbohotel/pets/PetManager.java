package com.eu.habbo.habbohotel.pets;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.gameclients.GameClient;
import com.eu.habbo.habbohotel.items.Item;
import com.eu.habbo.habbohotel.items.interactions.pets.InteractionNest;
import com.eu.habbo.habbohotel.items.interactions.pets.InteractionPetDrink;
import com.eu.habbo.habbohotel.items.interactions.pets.InteractionPetFood;
import com.eu.habbo.habbohotel.items.interactions.pets.InteractionPetToy;
import com.eu.habbo.habbohotel.pets.actions.*;
import com.eu.habbo.habbohotel.rooms.Room;
import com.eu.habbo.habbohotel.rooms.RoomTile;
import com.eu.habbo.habbohotel.rooms.RoomUnit;
import com.eu.habbo.habbohotel.users.Habbo;
import gnu.trove.map.TIntIntMap;
import gnu.trove.map.hash.THashMap;
import gnu.trove.map.hash.TIntIntHashMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.procedure.TIntObjectProcedure;
import gnu.trove.set.hash.THashSet;
import org.apache.commons.math3.distribution.NormalDistribution;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

public class PetManager {
    public static int MAXIMUM_PET_INVENTORY_SIZE = 25;
    private static final Logger LOGGER = LoggerFactory.getLogger(PetManager.class);
    public static final int[] experiences = new int[]{100, 200, 400, 600, 900, 1300, 1800, 2400, 3200, 4300, 5700, 7600, 10100, 13300, 17500, 23000, 30200, 39600, 51900};
    static int[] skins = new int[]{0, 1, 6, 7};
    public final THashMap<Integer, PetAction> petActions = new THashMap<Integer, PetAction>() {
        {
            this.put(0, new ActionFree());
            this.put(1, new ActionSit());
            this.put(2, new ActionDown());
            this.put(3, new ActionHere());
            this.put(4, new ActionBeg());
            this.put(5, new ActionPlayDead());
            this.put(6, new ActionStay());
            this.put(7, new ActionFollow());
            this.put(8, new ActionStand());
            this.put(9, new ActionJump());
            this.put(10, new ActionSpeak());
            this.put(11, new ActionPlay());
            this.put(12, new ActionSilent());
            this.put(13, new ActionNest());
            this.put(14, new ActionDrink());
            this.put(15, new ActionFollowLeft());
            this.put(16, new ActionFollowRight());
            this.put(17, new ActionPlayFootball());
            this.put(24, new ActionMoveForward());
            this.put(25, new ActionTurnLeft());
            this.put(26, new ActionTurnRight());
            this.put(27, new ActionRelax());
            this.put(28, new ActionCroak());
            this.put(29, new ActionDip());
            this.put(30, new ActionWave());
            this.put(35, new ActionWings());
            this.put(36, new ActionBreatheFire());
            this.put(38, new ActionTorch());
            this.put(43, new ActionEat());
            this.put(46, new ActionBreed());

        }
    };
    private final THashMap<Integer, THashSet<PetRace>> petRaces;
    private final THashMap<Integer, PetData> petData;
    private final TIntIntMap breedingPetType;
    private final THashMap<Integer, TIntObjectHashMap<ArrayList<PetBreedingReward>>> breedingReward;


    public PetManager() {
        long millis = System.currentTimeMillis();

        this.petRaces = new THashMap<>();
        this.petData = new THashMap<>();
        this.breedingPetType = new TIntIntHashMap();
        this.breedingReward = new THashMap<>();

        reloadPetData();

        LOGGER.info("Pet Manager -> Loaded! ({} MS)", System.currentTimeMillis() - millis);
    }

    public static int getLevel(int experience) {
        int index = -1;

        for (int i = 0; i < experiences.length; i++) {
            if (experiences[i] > experience) {
                index = i;
                break;
            }
        }

        if(index == -1) { index = experiences.length; }
        return index + 1;
    }

    public static int maxEnergy(int level) {
        //TODO: Add energy calculation.
        return 100 * level;
    }

    public static int randomBody(int minimumRarity, boolean isRare) {
        int randomRarity = isRare ? random(Math.max(minimumRarity - 1, 0), (MonsterplantPet.bodyRarity.size() - minimumRarity) + (minimumRarity - 1), 2.0) : random(Math.max(minimumRarity - 1, 0), MonsterplantPet.bodyRarity.size(), 2.0);

        return MonsterplantPet.bodyRarity.get(MonsterplantPet.bodyRarity.keySet().toArray()[randomRarity]).getValue();
    }

    public static int randomColor(int minimumRarity, boolean isRare) {
        int randomRarity = isRare ? random(Math.max(minimumRarity - 1, 0), (MonsterplantPet.colorRarity.size() - minimumRarity) + (minimumRarity - 1), 2.0) : random(Math.max(minimumRarity - 1, 0), MonsterplantPet.colorRarity.size(), 2.0);

        return MonsterplantPet.colorRarity.get(MonsterplantPet.colorRarity.keySet().toArray()[randomRarity]).getValue();
    }

    public static int random(int low, int high, double bias) {
        double r = Math.random();
        r = Math.pow(r, bias);
        return (int) (low + (high - low) * r);
    }

    public static Pet loadPet(ResultSet set) throws SQLException {
        if (set.getInt("type") == 15)
            return new HorsePet(set);
        else if (set.getInt("type") == 16)
            return new MonsterplantPet(set);
        else if (set.getInt("type") == 26 || set.getInt("type") == 27)
            return new GnomePet(set);
        else
            return new Pet(set);
    }

    public static NormalDistribution getNormalDistributionForBreeding(int levelOne, int levelTwo) {
        return getNormalDistributionForBreeding((levelOne + levelTwo) / 2);
    }

    public static NormalDistribution getNormalDistributionForBreeding(double avgLevel) {
        return new NormalDistribution(avgLevel, (20 - (avgLevel / 2)) / 2);
    }

    public void reloadPetData() {
        this.petRaces.clear();
        this.petData.clear();
        this.breedingPetType.clear();
        this.breedingReward.clear();

        try (Connection connection = Emulator.getDatabase().getDataSource().getConnection()) {
            this.loadRaces(connection);
            this.loadPetData(connection);
            this.loadPetCommands(connection);
            this.loadPetBreeding(connection);
        } catch (SQLException e) {
            LOGGER.error("Caught SQL exception", e);
            LOGGER.error("Pet Manager -> Failed to load!");
        }
    }

    private void loadRaces(Connection connection) {
        this.petRaces.clear();

        try (Statement statement = connection.createStatement(); ResultSet set = statement.executeQuery("SELECT * FROM pet_breeds ORDER BY race, color_one, color_two ASC")) {
            while (set.next()) {
                if (this.petRaces.get(set.getInt("race")) == null)
                    this.petRaces.put(set.getInt("race"), new THashSet<>());

                this.petRaces.get(set.getInt("race")).add(new PetRace(set));
            }
        } catch (SQLException e) {
            LOGGER.error("Caught SQL exception", e);
        }
    }

    private void loadPetData(Connection connection) {
        try (Statement statement = connection.createStatement(); ResultSet set = statement.executeQuery("SELECT * FROM pet_actions ORDER BY pet_type ASC")) {
            while (set.next()) {
                this.petData.put(set.getInt("pet_type"), new PetData(set));
            }
        } catch (SQLException e) {
            LOGGER.error("Caught SQL exception", e);
        }

        this.loadPetItems(connection);

        this.loadPetVocals(connection);
    }

    private void loadPetItems(Connection connection) {
        try (Statement statement = connection.createStatement(); ResultSet set = statement.executeQuery("SELECT * FROM pet_items")) {
            while (set.next()) {
                Item baseItem = Emulator.getGameEnvironment().getItemManager().getItem(set.getInt("item_id"));

                if (baseItem != null) {
                    if (set.getInt("pet_id") == -1) {
                        if (baseItem.getInteractionType().getType() == InteractionNest.class)
                            PetData.generalNestItems.add(baseItem);
                        else if (baseItem.getInteractionType().getType() == InteractionPetFood.class)
                            PetData.generalFoodItems.add(baseItem);
                        else if (baseItem.getInteractionType().getType() == InteractionPetDrink.class)
                            PetData.generalDrinkItems.add(baseItem);
                        else if (baseItem.getInteractionType().getType() == InteractionPetToy.class)
                            PetData.generalToyItems.add(baseItem);
                    } else {
                        PetData data = this.getPetData(set.getInt("pet_id"));

                        if (data != null) {
                            if (baseItem.getInteractionType().getType() == InteractionNest.class)
                                data.addNest(baseItem);
                            else if (baseItem.getInteractionType().getType() == InteractionPetFood.class)
                                data.addFoodItem(baseItem);
                            else if (baseItem.getInteractionType().getType() == InteractionPetDrink.class)
                                data.addDrinkItem(baseItem);
                            else if (baseItem.getInteractionType().getType() == InteractionPetToy.class)
                                data.addToyItem(baseItem);
                        }
                    }
                }
            }
        } catch (SQLException e) {
            LOGGER.error("Caught SQL exception", e);
        }
    }

    private void loadPetVocals(Connection connection) {
        try (Statement statement = connection.createStatement(); ResultSet set = statement.executeQuery("SELECT * FROM pet_vocals")) {
            while (set.next()) {
                if (set.getInt("pet_id") >= 0) {
                    if (this.petData.containsKey(set.getInt("pet_id"))) {
                        PetVocalsType petVocalsType = PetVocalsType.valueOf(set.getString("type").toUpperCase());

                        if (petVocalsType != null) {
                            this.petData.get(set.getInt("pet_id")).petVocals.get(petVocalsType).add(new PetVocal(set.getString("message")));
                        } else {
                            LOGGER.error("Unknown pet vocal type {}", set.getString("type"));
                        }
                    } else {
                        LOGGER.error("Missing pet_actions table entry for pet id {}", set.getInt("pet_id"));
                    }
                } else {
                    if (!PetData.generalPetVocals.containsKey(PetVocalsType.valueOf(set.getString("type").toUpperCase())))
                        PetData.generalPetVocals.put(PetVocalsType.valueOf(set.getString("type").toUpperCase()), new THashSet<>());

                    PetData.generalPetVocals.get(PetVocalsType.valueOf(set.getString("type").toUpperCase())).add(new PetVocal(set.getString("message")));
                }
            }
        } catch (SQLException e) {
            LOGGER.error("Caught SQL exception", e);
        }
    }

    private void loadPetCommands(Connection connection) {
        THashMap<Integer, PetCommand> commandsList = new THashMap<>();
        try (Statement statement = connection.createStatement(); ResultSet set = statement.executeQuery("SELECT * FROM pet_commands_data")) {
            while (set.next()) {
                commandsList.put(set.getInt("command_id"), new PetCommand(set, this.petActions.get(set.getInt("command_id"))));
            }
        } catch (SQLException e) {
            LOGGER.error("Caught SQL exception", e);
        }

        try (Statement statement = connection.createStatement(); ResultSet set = statement.executeQuery("SELECT * FROM pet_commands ORDER BY pet_id ASC")) {
            while (set.next()) {
                PetData data = this.petData.get(set.getInt("pet_id"));

                if (data != null) {
                    data.getPetCommands().add(commandsList.get(set.getInt("command_id")));
                }
            }
        } catch (SQLException e) {
            LOGGER.error("Caught SQL exception", e);
        }
    }

    private void loadPetBreeding(Connection connection) {
        try (Statement statement = connection.createStatement(); ResultSet set = statement.executeQuery("SELECT * FROM pet_breeding")) {
            while (set.next()) {
                this.breedingPetType.put(set.getInt("pet_id"), set.getInt("offspring_id"));
            }
        } catch (SQLException e) {
            LOGGER.error("Caught SQL exception", e);
        }

        try (Statement statement = connection.createStatement(); ResultSet set = statement.executeQuery("SELECT * FROM pet_breeding_races")) {
            while (set.next()) {
                PetBreedingReward reward = new PetBreedingReward(set);
                if (!this.breedingReward.containsKey(reward.petType)) {
                    this.breedingReward.put(reward.petType, new TIntObjectHashMap<>());
                }

                if (!this.breedingReward.get(reward.petType).containsKey(reward.rarityLevel)) {
                    this.breedingReward.get(reward.petType).put(reward.rarityLevel, new ArrayList<>());
                }

                this.breedingReward.get(reward.petType).get(reward.rarityLevel).add(reward);
            }
        } catch (SQLException e) {
            LOGGER.error("Caught SQL exception", e);
        }
    }

    public THashSet<PetRace> getBreeds(String petName) {
        if (!petName.startsWith("a0 pet")) {
            LOGGER.error("Pet {} not found. Make sure it matches the pattern \"a0 pet<pet_id>\"!", petName);
            return null;
        }

        try {
            int petId = Integer.parseInt(petName.split("t")[1]);
            return this.petRaces.get(petId);
        } catch (Exception e) {
            LOGGER.error("Caught exception", e);
        }

        return null;
    }

    public TIntObjectHashMap<ArrayList<PetBreedingReward>> getBreedingRewards(int petType) {
        return this.breedingReward.get(petType);
    }

    public int getRarityForOffspring(Pet pet) {
        final int[] rarityLevel = {0};

        TIntObjectHashMap<ArrayList<PetBreedingReward>> offspringList = this.breedingReward.get(pet.getPetData().getType());

        offspringList.forEachEntry(new TIntObjectProcedure<ArrayList<PetBreedingReward>>() {
            @Override
            public boolean execute(int i, ArrayList<PetBreedingReward> petBreedingRewards) {
                for (PetBreedingReward reward : petBreedingRewards) {
                    if (reward.breed == pet.getRace()) {
                        rarityLevel[0] = i;
                        return false;
                    }
                }

                return true;
            }
        });

        return 4 - rarityLevel[0];
    }

    public PetData getPetData(int type) {
        synchronized (this.petData) {
            if (this.petData.containsKey(type)) {
                return this.petData.get(type);
            } else {
                try (Connection connection = Emulator.getDatabase().getDataSource().getConnection()) {
                    LOGGER.error("Missing petdata for type {}. Adding this to the database...", type);
                    try (PreparedStatement statement = connection.prepareStatement("INSERT INTO pet_actions (pet_type) VALUES (?)")) {
                        statement.setInt(1, type);
                        statement.execute();
                    }

                    try (PreparedStatement statement = connection.prepareStatement("SELECT * FROM pet_actions WHERE pet_type = ? LIMIT 1")) {
                        statement.setInt(1, type);
                        try (ResultSet set = statement.executeQuery()) {
                            if (set.next()) {
                                PetData petData = new PetData(set);
                                this.petData.put(type, petData);
                                LOGGER.error("Missing petdata for type {} added to the database!", type);
                                return petData;
                            }
                        }
                    }
                } catch (SQLException e) {
                    LOGGER.error("Caught SQL exception", e);
                }
            }
        }

        return null;
    }

    public PetData getPetData(String petName) {
        synchronized (this.petData) {
            for (Map.Entry<Integer, PetData> entry : this.petData.entrySet()) {
                if (entry.getValue().getName().equalsIgnoreCase(petName)) {
                    return entry.getValue();
                }
            }
        }

        return null;
    }

    public Collection<PetData> getPetData() {
        return this.petData.values();
    }

    public Pet createPet(Item item, String name, String race, String color, GameClient client) {
        int type = Integer.parseInt(item.getName().toLowerCase().replace("a0 pet", ""));

        if (this.petData.containsKey(type)) {
            Pet pet;
            if (type == 15)
                pet = new HorsePet(type, Integer.parseInt(race), color, name, client.getHabbo().getHabboInfo().getId());
            else if (type == 16)
                pet = this.createMonsterplant(null, client.getHabbo(), false, null, 0);
            else
                pet = new Pet(type,
                        Integer.parseInt(race),
                        color,
                        name,
                        client.getHabbo().getHabboInfo().getId()
                );

            pet.needsUpdate = true;
            pet.run();
            return pet;
        }
        return null;
    }

    public Pet createPet(int type, String name, GameClient client) {
        return this.createPet(type, Emulator.getRandom().nextInt(this.petRaces.get(type).size() + 1), name, client);
    }

    public Pet createPet(int type, int race, String name, GameClient client) {
        if (this.petData.containsKey(type)) {
            Pet pet = new Pet(type, race, "FFFFFF", name, client.getHabbo().getHabboInfo().getId());
            pet.needsUpdate = true;
            pet.run();
            return pet;
        }
        return null;
    }

    public MonsterplantPet createMonsterplant(Room room, Habbo habbo, boolean rare, RoomTile t, int minimumRarity) {
        MonsterplantPet pet = new MonsterplantPet(
                habbo.getHabboInfo().getId(),   //Owner ID
                randomBody(minimumRarity, rare),
                randomColor(minimumRarity, rare),
                Emulator.getRandom().nextInt(12) + 1,
                Emulator.getRandom().nextInt(11),
                Emulator.getRandom().nextInt(12) + 1,
                Emulator.getRandom().nextInt(11),
                Emulator.getRandom().nextInt(12) + 1,
                Emulator.getRandom().nextInt(11)
        );

        pet.setUserId(habbo.getHabboInfo().getId());
        pet.setRoom(room);
        pet.setRoomUnit(new RoomUnit());
        pet.getRoomUnit().setPathFinderRoom(room);
        pet.needsUpdate = true;
        pet.run();
        return pet;
    }

    public Pet createGnome(String name, Room room, Habbo habbo) {
        Pet pet = new GnomePet(26, 0, "FFFFFF", name, habbo.getHabboInfo().getId(),
                "5 " +
                        "0 -1 " + this.randomGnomeSkinColor() + " " +
                        "1 10" + (1 + Emulator.getRandom().nextInt(2)) + " " + this.randomGnomeColor() + " " +
                        "2 201 " + this.randomGnomeColor() + " " +
                        "3 30" + (1 + Emulator.getRandom().nextInt(2)) + " " + this.randomGnomeColor() + " " +
                        "4 40" + Emulator.getRandom().nextInt(2) + " " + this.randomGnomeColor()
        );

        pet.setUserId(habbo.getHabboInfo().getId());
        pet.setRoom(room);
        pet.setRoomUnit(new RoomUnit());
        pet.getRoomUnit().setPathFinderRoom(room);
        pet.needsUpdate = true;
        pet.run();

        return pet;
    }

    public Pet createLeprechaun(String name, Room room, Habbo habbo) {
        Pet pet = new GnomePet(27, 0, "FFFFFF", name, habbo.getHabboInfo().getId(),
                "5 " +
                        "0 -1 0 " +
                        "1 102 19 " +
                        "2 201 27 " +
                        "3 302 23 " +
                        "4 401 27"
        );

        pet.setUserId(habbo.getHabboInfo().getId());
        pet.setRoom(room);
        pet.setRoomUnit(new RoomUnit());
        pet.getRoomUnit().setPathFinderRoom(room);
        pet.needsUpdate = true;
        pet.run();

        return pet;
    }

    private int randomGnomeColor() {
        int color = 19;

        while (color == 19 || color == 27) {
            color = Emulator.getRandom().nextInt(34);
        }

        return color;
    }

    private int randomLeprechaunColor() {
        return Emulator.getRandom().nextInt(2) == 1 ? 19 : 27;
    }

    private int randomGnomeSkinColor() {
        return skins[Emulator.getRandom().nextInt(skins.length)];
    }

    public boolean deletePet(Pet pet) {
        try (Connection connection = Emulator.getDatabase().getDataSource().getConnection(); PreparedStatement statement = connection.prepareStatement("DELETE FROM users_pets WHERE id = ? LIMIT 1")) {
            statement.setInt(1, pet.getId());
            return statement.execute();
        } catch (SQLException e) {
            LOGGER.error("Caught SQL exception", e);
        }

        return false;
    }
}