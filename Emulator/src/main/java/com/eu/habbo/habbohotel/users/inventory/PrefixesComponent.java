package com.eu.habbo.habbohotel.users.inventory;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.users.Habbo;
import com.eu.habbo.habbohotel.users.UserPrefix;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class PrefixesComponent {
    private static final Logger LOGGER = LoggerFactory.getLogger(PrefixesComponent.class);

    private final List<UserPrefix> prefixes = new ArrayList<>();
    private final Habbo habbo;

    public PrefixesComponent(Habbo habbo) {
        this.habbo = habbo;
        this.loadPrefixes();
    }

    private void loadPrefixes() {
        try (Connection connection = Emulator.getDatabase().getDataSource().getConnection();
             PreparedStatement statement = connection.prepareStatement("SELECT * FROM user_prefixes WHERE user_id = ?")) {
            statement.setInt(1, this.habbo.getHabboInfo().getId());
            try (ResultSet set = statement.executeQuery()) {
                while (set.next()) {
                    this.prefixes.add(new UserPrefix(set));
                }
            }
        } catch (SQLException e) {
            LOGGER.error("Caught SQL exception", e);
        }
    }

    public List<UserPrefix> getPrefixes() {
        synchronized (this.prefixes) {
            return new ArrayList<>(this.prefixes);
        }
    }

    public UserPrefix getActivePrefix() {
        synchronized (this.prefixes) {
            for (UserPrefix prefix : this.prefixes) {
                if (prefix.isActive()) return prefix;
            }
        }
        return null;
    }

    public UserPrefix getPrefix(int id) {
        synchronized (this.prefixes) {
            for (UserPrefix prefix : this.prefixes) {
                if (prefix.getId() == id) return prefix;
            }
        }
        return null;
    }

    public void addPrefix(UserPrefix prefix) {
        synchronized (this.prefixes) {
            this.prefixes.add(prefix);
        }
    }

    public void removePrefix(UserPrefix prefix) {
        synchronized (this.prefixes) {
            this.prefixes.remove(prefix);
        }
    }

    public void setActive(int prefixId) {
        synchronized (this.prefixes) {
            for (UserPrefix prefix : this.prefixes) {
                boolean shouldBeActive = prefix.getId() == prefixId;
                if (prefix.isActive() != shouldBeActive) {
                    prefix.setActive(shouldBeActive);
                    Emulator.getThreading().run(prefix);
                }
            }
        }
    }

    public void deactivateAll() {
        synchronized (this.prefixes) {
            for (UserPrefix prefix : this.prefixes) {
                if (prefix.isActive()) {
                    prefix.setActive(false);
                    Emulator.getThreading().run(prefix);
                }
            }
        }
    }

    public void dispose() {
        synchronized (this.prefixes) {
            this.prefixes.clear();
        }
    }
}
