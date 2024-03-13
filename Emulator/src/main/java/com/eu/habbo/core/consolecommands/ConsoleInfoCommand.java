package com.eu.habbo.core.consolecommands;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.catalog.CatalogManager;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.TimeUnit;

@Slf4j
public class ConsoleInfoCommand extends ConsoleCommand {
    public ConsoleInfoCommand() {
        super("info", "Show current statistics.");
    }

    @Override
    public void handle(String[] args) throws Exception {
        int seconds = Emulator.getIntUnixTimestamp() - Emulator.getTimeStarted();
        int day = (int) TimeUnit.SECONDS.toDays(seconds);
        long hours = TimeUnit.SECONDS.toHours(seconds) - (day * 24);
        long minute = TimeUnit.SECONDS.toMinutes(seconds) - (TimeUnit.SECONDS.toHours(seconds) * 60);
        long second = TimeUnit.SECONDS.toSeconds(seconds) - (TimeUnit.SECONDS.toMinutes(seconds) * 60);

        log.info("Emulator version: " + Emulator.version);
        log.info("Emulator build: " + Emulator.build);

        log.info("");

        log.info("Hotel Statistics");
        log.info("- Users: " + Emulator.getGameEnvironment().getHabboManager().getOnlineCount());
        log.info("- Rooms: " + Emulator.getGameEnvironment().getRoomManager().getActiveRooms().size());
        log.info("- Shop:  " + Emulator.getGameEnvironment().getCatalogManager().catalogPages.size() + " pages and " + CatalogManager.catalogItemAmount + " items.");
        log.info("- Furni: " + Emulator.getGameEnvironment().getItemManager().getItems().size() + " items.");
        log.info("");
        log.info("Server Statistics");
        log.info("- Uptime: " + day + (day > 1 ? " days, " : " day, ") + hours + (hours > 1 ? " hours, " : " hour, ") + minute + (minute > 1 ? " minutes, " : " minute, ") + second + (second > 1 ? " seconds!" : " second!"));
        log.info("- RAM Usage: " + (Emulator.getRuntime().totalMemory() - Emulator.getRuntime().freeMemory()) / (1024 * 1024) + "/" + (Emulator.getRuntime().freeMemory()) / (1024 * 1024) + "MB");
        log.info("- CPU Cores: " + Emulator.getRuntime().availableProcessors());
        log.info("- Total Memory: " + Emulator.getRuntime().maxMemory() / (1024 * 1024) + "MB");
    }
}