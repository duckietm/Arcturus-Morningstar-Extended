package com.eu.habbo.habbohotel.pets;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.items.Item;
import com.eu.habbo.habbohotel.items.interactions.pets.InteractionNest;
import com.eu.habbo.habbohotel.items.interactions.pets.InteractionPetDrink;
import com.eu.habbo.habbohotel.items.interactions.pets.InteractionPetFood;
import com.eu.habbo.habbohotel.items.interactions.pets.InteractionPetToy;
import com.eu.habbo.habbohotel.users.HabboItem;
import gnu.trove.map.hash.THashMap;
import gnu.trove.set.hash.THashSet;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PetData implements Comparable<PetData> {

    public static final String BLINK = "eyb";
    public static final String SPEAK = "spk";
    public static final String EAT = "eat";
    public static final String PLAYFUL = "pla";
    public static final List<Item> generalDrinkItems = new ArrayList<>();
    public static final List<Item> generalFoodItems = new ArrayList<>();
    public static final List<Item> generalNestItems = new ArrayList<>();
    public static final List<Item> generalToyItems = new ArrayList<>();
    public static final THashMap<PetVocalsType, THashSet<PetVocal>> generalPetVocals = new THashMap<>();
    public String[] actionsHappy;
    public String[] actionsTired;
    public String[] actionsRandom;
    public THashMap<PetVocalsType, THashSet<PetVocal>> petVocals;
    public boolean canSwim;
    private int type;
    private String name;
    private List<PetCommand> petCommands;
    private List<Item> nestItems;
    private List<Item> foodItems;
    private List<Item> drinkItems;
    private List<Item> toyItems;
    private int offspringType;

    public PetData(ResultSet set) throws SQLException {
        this.load(set);
    }

    public void load(ResultSet set) throws SQLException {
        this.type = set.getInt("pet_type");
        this.name = set.getString("pet_name");
        this.offspringType = set.getInt("offspring_type");
        this.actionsHappy = set.getString("happy_actions").split(";");
        this.actionsTired = set.getString("tired_actions").split(";");
        this.actionsRandom = set.getString("random_actions").split(";");
        this.canSwim = set.getString("can_swim").equalsIgnoreCase("1");

        this.reset();
    }

    public List<PetCommand> getPetCommands() {
        return this.petCommands;
    }

    public void setPetCommands(List<PetCommand> petCommands) {
        this.petCommands = petCommands;
    }

    public int getType() {
        return this.type;
    }


    public String getName() {
        return this.name;
    }


    public int getOffspringType() {
        return this.offspringType;
    }


    public void addNest(Item nest) {
        if (nest != null)
            this.nestItems.add(nest);
    }


    public List<Item> getNests() {
        return this.nestItems;
    }


    public boolean haveNest(HabboItem nest) {
        return this.haveNest(nest.getBaseItem());
    }


    boolean haveNest(Item nest) {
        return PetData.generalNestItems.contains(nest) || this.nestItems.contains(nest);
    }


    public HabboItem randomNest(THashSet<InteractionNest> items) {
        List<HabboItem> nestList = new ArrayList<>();

        for (InteractionNest nest : items) {
            if (this.haveNest(nest)) {
                nestList.add(nest);
            }
        }

        if (!nestList.isEmpty()) {
            Collections.shuffle(nestList);

            return nestList.get(0);
        }

        return null;
    }


    public void addFoodItem(Item food) {
        this.foodItems.add(food);
    }


    public List<Item> getFoodItems() {
        return this.foodItems;
    }


    public boolean haveFoodItem(HabboItem food) {
        return this.haveFoodItem(food.getBaseItem());
    }


    boolean haveFoodItem(Item food) {
        return this.foodItems.contains(food) || PetData.generalFoodItems.contains(food);
    }


    public HabboItem randomFoodItem(THashSet<InteractionPetFood> items) {
        List<HabboItem> foodList = new ArrayList<>();

        for (InteractionPetFood food : items) {
            if (this.haveFoodItem(food)) {
                foodList.add(food);
            }
        }

        if (!foodList.isEmpty()) {
            Collections.shuffle(foodList);
            return foodList.get(0);
        }

        return null;
    }


    public void addDrinkItem(Item item) {
        this.drinkItems.add(item);
    }


    public List<Item> getDrinkItems() {
        return this.drinkItems;
    }


    public boolean haveDrinkItem(HabboItem item) {
        return this.haveDrinkItem(item.getBaseItem());
    }


    boolean haveDrinkItem(Item item) {
        return this.drinkItems.contains(item) || PetData.generalDrinkItems.contains(item);
    }


    public HabboItem randomDrinkItem(THashSet<InteractionPetDrink> items) {
        List<HabboItem> drinkList = new ArrayList<>();

        for (InteractionPetDrink drink : items) {
            if (this.haveDrinkItem(drink)) {
                drinkList.add(drink);
            }
        }

        if (!drinkList.isEmpty()) {
            Collections.shuffle(drinkList);
            return drinkList.get(0);
        }

        return null;
    }


    public void addToyItem(Item toy) {
        this.toyItems.add(toy);
    }


    public List<Item> getToyItems() {
        return this.toyItems;
    }


    public boolean haveToyItem(HabboItem toy) {
        return this.haveToyItem(toy.getBaseItem());
    }


    public boolean haveToyItem(Item toy) {
        return this.toyItems.contains(toy) || PetData.generalToyItems.contains(toy);
    }


    public HabboItem randomToyItem(THashSet<InteractionPetToy> toys) {
        List<HabboItem> toyList = new ArrayList<>();

        for (InteractionPetToy toy : toys) {
            if (this.haveToyItem(toy)) {
                toyList.add(toy);
            }
        }

        if (!toyList.isEmpty()) {
            Collections.shuffle(toyList);
            return toyList.get(0);
        }

        return null;
    }


    public PetVocal randomVocal(PetVocalsType type) {
        //TODO: Remove this useless copying.
        List<PetVocal> vocals = new ArrayList<>();

        if (this.petVocals.get(type) != null)
            vocals.addAll(this.petVocals.get(type));

        if (PetData.generalPetVocals.get(type) != null)
            vocals.addAll(PetData.generalPetVocals.get(type));

        if (vocals.isEmpty())
            return null;

        return vocals.get(Emulator.getRandom().nextInt(vocals.size()));
    }

    @Override
    public int compareTo(PetData o) {
        return this.getType() - o.getType();
    }

    public void reset() {
        this.petCommands = new ArrayList<>();
        this.nestItems = new ArrayList<>();
        this.foodItems = new ArrayList<>();
        this.drinkItems = new ArrayList<>();
        this.toyItems = new ArrayList<>();

        this.petVocals = new THashMap<>();

        for (PetVocalsType type : PetVocalsType.values()) {
            this.petVocals.put(type, new THashSet<>());
        }

        if (PetData.generalPetVocals.isEmpty()) {
            for (PetVocalsType type : PetVocalsType.values()) {
                PetData.generalPetVocals.put(type, new THashSet<>());
            }
        }
    }
}
