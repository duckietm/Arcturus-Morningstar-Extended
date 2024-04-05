package com.eu.habbo.habbohotel.users.inventory;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.pets.Pet;
import com.eu.habbo.habbohotel.pets.PetManager;
import com.eu.habbo.habbohotel.users.Habbo;
import gnu.trove.TCollections;
import gnu.trove.iterator.TIntObjectIterator;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.NoSuchElementException;
import java.util.Set;

public class PetsComponent {
    private static final Logger LOGGER = LoggerFactory.getLogger(PetsComponent.class);
    private final TIntObjectMap<Pet> pets = TCollections.synchronizedMap(new TIntObjectHashMap<>());

    public PetsComponent(Habbo habbo) {
        this.loadPets(habbo);
    }

    private void loadPets(Habbo habbo) {
        synchronized (this.pets) {
            try (Connection connection = Emulator.getDatabase().getDataSource().getConnection(); PreparedStatement statement = connection.prepareStatement("SELECT * FROM users_pets WHERE user_id = ? AND room_id = 0")) {
                statement.setInt(1, habbo.getHabboInfo().getId());

                try (ResultSet set = statement.executeQuery()) {
                    while (set.next()) {
                        this.pets.put(set.getInt("id"), PetManager.loadPet(set));
                    }
                }
            } catch (SQLException e) {
                LOGGER.error("Caught SQL exception", e);
            }
        }
    }

    public Pet getPet(int id) {
        return this.pets.get(id);
    }

    public void addPet(Pet pet) {
        synchronized (this.pets) {
            this.pets.put(pet.getId(), pet);
        }
    }

    public void addPets(Set<Pet> pets) {
        synchronized (this.pets) {
            for (Pet p : pets) {
                this.pets.put(p.getId(), p);
            }
        }
    }

    public void removePet(Pet pet) {
        synchronized (this.pets) {
            this.pets.remove(pet.getId());
        }
    }

    public TIntObjectMap<Pet> getPets() {
        return this.pets;
    }

    public int getPetsCount() {
        return this.pets.size();
    }

    public void dispose() {
        synchronized (this.pets) {
            TIntObjectIterator<Pet> petIterator = this.pets.iterator();

            for (int i = this.pets.size(); i-- > 0; ) {
                try {
                    petIterator.advance();
                } catch (NoSuchElementException e) {
                    break;
                }
                if (petIterator.value().needsUpdate)
                    Emulator.getThreading().run(petIterator.value());
            }
        }
    }
}
