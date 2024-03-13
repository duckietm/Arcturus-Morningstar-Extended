package com.eu.habbo.habbohotel.commands;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.catalog.CatalogManager;
import com.eu.habbo.habbohotel.gameclients.GameClient;
import com.eu.habbo.habbohotel.users.HabboManager;
import com.eu.habbo.messages.outgoing.generic.alerts.MessagesForYouComposer;

import java.util.Collections;
import java.util.concurrent.TimeUnit;


public class AboutCommand extends Command {
    public AboutCommand() {
        super(null, new String[]{"about", "info", "online", "server"});
    }
    public static String credits = "Arcturus Morningstar is an opensource project based on Arcturus By TheGeneral \n" +
            "The Following people have all contributed to this emulator:\n" +
            " TheGeneral\n Beny\n Alejandro\n Capheus\n Skeletor\n Harmonic\n Mike\n Remco\n zGrav \n Quadral \n Harmony\n Swirny\n ArpyAge\n Mikkel\n Rodolfo\n Rasmus\n Kitt Mustang\n Snaiker\n nttzx\n necmi\n Dome\n Jose Flores\n Cam\n Oliver\n Narzo\n Tenshie\n MartenM\n Ridge\n SenpaiDipper\n Snaiker\n Thijmen";
    @Override
    public boolean handle(GameClient gameClient, String[] params) {

        Emulator.getRuntime().gc();

        int seconds = Emulator.getIntUnixTimestamp() - Emulator.getTimeStarted();
        int day = (int) TimeUnit.SECONDS.toDays(seconds);
        long hours = TimeUnit.SECONDS.toHours(seconds) - (day * 24);
        long minute = TimeUnit.SECONDS.toMinutes(seconds) - (TimeUnit.SECONDS.toHours(seconds) * 60);
        long second = TimeUnit.SECONDS.toSeconds(seconds) - (TimeUnit.SECONDS.toMinutes(seconds) * 60);

        String message = "<b>" + Emulator.version + "</b>\r\n";

        if (Emulator.getConfig().getBoolean("info.shown", true)) {
            message += "<b>Hotel Statistics</b>\r" +
                    "- Online Users: " + Emulator.getGameEnvironment().getHabboManager().getOnlineCount() + "\r" +
                    "- Active Rooms: " + Emulator.getGameEnvironment().getRoomManager().getActiveRooms().size() + "\r" +
                    "- Shop:  " + Emulator.getGameEnvironment().getCatalogManager().catalogPages.size() + " pages and " + CatalogManager.catalogItemAmount + " items. \r" +
                    "- Furni: " + Emulator.getGameEnvironment().getItemManager().getItems().size() + " item definitions" + "\r" +
                    "\n" +
                    "<b>Server Statistics</b>\r" +
                    "- Uptime: " + day + (day > 1 ? " days, " : " day, ") + hours + (hours > 1 ? " hours, " : " hour, ") + minute + (minute > 1 ? " minutes, " : " minute, ") + second + (second > 1 ? " seconds!" : " second!") + "\r" +
                    "- RAM Usage: " + (Emulator.getRuntime().totalMemory() - Emulator.getRuntime().freeMemory()) / (1024 * 1024) + "/" + (Emulator.getRuntime().freeMemory()) / (1024 * 1024) + "MB\r" +
                    "- CPU Cores: " + Emulator.getRuntime().availableProcessors() + "\r" +
                    "- Total Memory: " + Emulator.getRuntime().maxMemory() / (1024 * 1024) + "MB" + "\r\n";
        }

        message += "\r" +

                "<b>Thanks for using Arcturus. Report issues on the forums. http://arcturus.wf \r\r" +
                "    - The General";
        gameClient.getHabbo().alert(message);
        gameClient.sendResponse(new MessagesForYouComposer(Collections.singletonList(credits)));
        return true;
    }
}
