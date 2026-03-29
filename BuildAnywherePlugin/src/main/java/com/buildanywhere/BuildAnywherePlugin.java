package com.buildanywhere;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.commands.CommandHandler;
import com.eu.habbo.habbohotel.users.Habbo;
import com.eu.habbo.plugin.EventHandler;
import com.eu.habbo.plugin.EventListener;
import com.eu.habbo.plugin.HabboPlugin;
import com.eu.habbo.plugin.events.emulator.EmulatorLoadedEvent;
import com.eu.habbo.plugin.events.furniture.FurnitureBuildheightEvent;
import com.eu.habbo.plugin.events.furniture.FurnitureMovedEvent;
import com.eu.habbo.plugin.events.furniture.FurniturePlacedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class BuildAnywherePlugin extends HabboPlugin implements EventListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(BuildAnywherePlugin.class);
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

    @Override
    public void onEnable() throws Exception {
        LOGGER.info("[BuildAnywhere] Plugin onEnable() called!");
        Emulator.getPluginManager().registerEvents(this, this);
        LOGGER.info("[BuildAnywhere] Events registered. Waiting for EmulatorLoadedEvent...");
    }

    @Override
    public void onDisable() throws Exception {
        enabledUsers.clear();
    }

    @Override
    public boolean hasPermission(Habbo habbo, String key) {
        if ("cmd_build_anywhere".equals(key)) {
            return habbo.getHabboInfo().getRank().getId() >= 7;
        }
        return false;
    }

    @EventHandler
    public void onEmulatorLoaded(EmulatorLoadedEvent event) {
        LOGGER.info("[BuildAnywhere] EmulatorLoadedEvent received! Registering command...");

        Emulator.getTexts().register("commands.keys.cmd_build_anywhere", "buildanywhere;ba");
        Emulator.getTexts().register("commands.succes.cmd_build_anywhere.enabled", "Build Anywhere ENABLED! You can now place furniture anywhere.");
        Emulator.getTexts().register("commands.succes.cmd_build_anywhere.disabled", "Build Anywhere DISABLED.");
        Emulator.getTexts().register("commands.description.cmd_build_anywhere", ":buildanywhere - Place furniture anywhere in the room.");

        CommandHandler.addCommand(new BuildAnywhereCommand());
        LOGGER.info("[BuildAnywhere] Command registered successfully!");
    }

    @EventHandler
    public void onFurniturePlaced(FurniturePlacedEvent event) {
        try {
            if (event.habbo != null && event.habbo.getHabboInfo() != null && hasEnabled(event.habbo.getHabboInfo().getId())) {
                event.setPluginHelper(true);
            }
        } catch (Exception ignored) {}
    }

    @EventHandler
    public void onFurnitureMoved(FurnitureMovedEvent event) {
        try {
            if (event.habbo != null && event.habbo.getHabboInfo() != null && hasEnabled(event.habbo.getHabboInfo().getId())) {
                event.setPluginHelper(true);
            }
        } catch (Exception ignored) {}
    }

    @EventHandler
    public void onFurnitureBuildHeight(FurnitureBuildheightEvent event) {
        try {
            if (event.habbo != null && event.habbo.getHabboInfo() != null && hasEnabled(event.habbo.getHabboInfo().getId())) {
                event.setNewHeight(event.newHeight);
            }
        } catch (Exception ignored) {}
    }
}
