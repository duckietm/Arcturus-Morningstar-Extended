package com.eu.habbo.habbohotel.users.subscriptions;

import com.eu.habbo.Emulator;
import gnu.trove.map.hash.THashMap;
import gnu.trove.set.hash.THashSet;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

@Slf4j
public class SubscriptionManager {

    public THashMap<String, Class<? extends Subscription>> types;

    public SubscriptionManager() {
        this.types = new THashMap<>();
    }

    public void init() {
        this.types.put(Subscription.HABBO_CLUB, SubscriptionHabboClub.class);
    }

    public void addSubscriptionType(String type, Class<? extends Subscription> clazz) {
        if(this.types.containsKey(type) || this.types.containsValue(clazz)) {
            throw new RuntimeException("Subscription Type must be unique. An class with type: " + clazz.getName() + " was already added OR the key: " + type + " is already in use.");
        }

        this.types.put(type, clazz);
    }

    public void removeSubscriptionType(String type) {
        this.types.remove(type);
    }

    public Class<? extends Subscription> getSubscriptionClass(String type) {
        if(!this.types.containsKey(type)) {
            log.debug("Can't find subscription class: {}", type);
            return Subscription.class;
        }

        return this.types.get(type);
    }

    public void dispose() {
        this.types.clear();
    }

    public THashSet<Subscription> getSubscriptionsForUser(int userId) {
        THashSet<Subscription> subscriptions = new THashSet<>();

        try (Connection connection = Emulator.getDatabase().getDataSource().getConnection();
             PreparedStatement statement = connection.prepareStatement("SELECT * FROM users_subscriptions WHERE user_id = ?")) {
            statement.setInt(1, userId);

            try (ResultSet set = statement.executeQuery()) {
                while (set.next()) {
                    Class<? extends Subscription> subClazz = Emulator.getGameEnvironment().getSubscriptionManager().getSubscriptionClass(set.getString("subscription_type"));
                    Constructor<? extends Subscription> c = subClazz.getConstructor(Integer.class, Integer.class, String.class, Integer.class, Integer.class, Boolean.class);
                    c.setAccessible(true);
                    Subscription subscription = c.newInstance(set.getInt("id"), set.getInt("user_id"), set.getString("subscription_type"), set.getInt("timestamp_start"), set.getInt("duration"), set.getInt("active") == 1);
                    subscriptions.add(subscription);
                }
            } catch (IllegalAccessException e) {
                log.error("IllegalAccessException", e);
            } catch (InstantiationException e) {
                log.error("InstantiationException", e);
            } catch (InvocationTargetException e) {
                log.error("InvocationTargetException", e);
            }
        } catch (SQLException e) {
            log.error("Caught SQL exception", e);
        }
        catch (NoSuchMethodException e) {
            log.error("Caught NoSuchMethodException", e);
        }

        return subscriptions;
    }
}
