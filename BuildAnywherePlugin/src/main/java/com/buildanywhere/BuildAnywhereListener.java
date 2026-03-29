package com.buildanywhere;

import com.eu.habbo.plugin.EventHandler;
import com.eu.habbo.plugin.EventListener;
import com.eu.habbo.plugin.events.furniture.FurnitureBuildheightEvent;
import com.eu.habbo.plugin.events.furniture.FurnitureMovedEvent;
import com.eu.habbo.plugin.events.furniture.FurniturePlacedEvent;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class BuildAnywhereListener implements EventListener {

    private static final Set<Integer> enabledUsers = Collections.newSetFromMap(new ConcurrentHashMap<>());

    public static boolean hasEnabled(int userId) {
        return enabledUsers.contains(userId);
    }

    public static void addUser(int userId) {
        enabledUsers.add(userId);
    }

    public static void removeUser(int userId) {
        enabledUsers.remove(userId);
    }

    private static boolean isEnabled(FurniturePlacedEvent event) {
        try {
            return event.habbo != null && event.habbo.getHabboInfo() != null && hasEnabled(event.habbo.getHabboInfo().getId());
        } catch (Exception e) {
            return false;
        }
    }

    private static boolean isEnabled(FurnitureMovedEvent event) {
        try {
            return event.habbo != null && event.habbo.getHabboInfo() != null && hasEnabled(event.habbo.getHabboInfo().getId());
        } catch (Exception e) {
            return false;
        }
    }

    private static boolean isEnabled(FurnitureBuildheightEvent event) {
        try {
            return event.habbo != null && event.habbo.getHabboInfo() != null && hasEnabled(event.habbo.getHabboInfo().getId());
        } catch (Exception e) {
            return false;
        }
    }

    @EventHandler
    public void onFurniturePlaced(FurniturePlacedEvent event) {
        if (isEnabled(event)) {
            event.setPluginHelper(true);
        }
    }

    @EventHandler
    public void onFurnitureMoved(FurnitureMovedEvent event) {
        if (isEnabled(event)) {
            event.setPluginHelper(true);
        }
    }

    @EventHandler
    public void onFurnitureBuildHeight(FurnitureBuildheightEvent event) {
        if (isEnabled(event)) {
            event.setNewHeight(event.newHeight);
        }
    }
}
