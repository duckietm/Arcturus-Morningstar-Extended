package org.krews.plugin.nitro;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.users.Habbo;
import com.eu.habbo.plugin.EventHandler;
import com.eu.habbo.plugin.EventListener;
import com.eu.habbo.plugin.HabboPlugin;
import com.eu.habbo.plugin.events.emulator.EmulatorLoadedEvent;
import com.eu.habbo.plugin.events.users.UserGetIPAddressEvent;
import io.netty.channel.Channel;
import io.netty.util.AttributeKey;
import org.krews.plugin.nitro.websockets.NetworkChannelInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class main extends HabboPlugin implements EventListener {
    private static final Logger LOGGER = LoggerFactory.getLogger(main.class);
    public static final AttributeKey<String> WS_IP = AttributeKey.valueOf("WS_IP");

    public void onEnable() throws Exception {
        Emulator.getPluginManager().registerEvents(this, this);
        if(Emulator.isReady && !Emulator.isShuttingDown) {
            this.onEmulatorLoadedEvent(null);
        }
    }

    public void onDisable() throws Exception {

    }

    public boolean hasPermission(Habbo habbo, String s) {
        return false;
    }

    @EventHandler
    public void onEmulatorLoadedEvent (EmulatorLoadedEvent e) throws InterruptedException {
        //add missing db entry
        Emulator.getConfig().register("websockets.whitelist", "localhost");
        Emulator.getConfig().register("ws.nitro.host", "0.0.0.0");
        Emulator.getConfig().register("ws.nitro.port", "2096");
        Emulator.getConfig().register("ws.nitro.ip.header", "");

        NetworkChannelInitializer wsChannelHandler = new NetworkChannelInitializer();
        Emulator.getGameServer().getServerBootstrap().childHandler(wsChannelHandler);

        Emulator.getGameServer().getServerBootstrap().bind(Emulator.getConfig().getValue("ws.nitro.host", "0.0.0.0"), Emulator.getConfig().getInt("ws.nitro.port", 2096)).sync();

        LOGGER.info("OFFICIAL PLUGIN - Nitro Websockets has started!");
        LOGGER.info("Nitro Websockets Listening on " + (wsChannelHandler.isSSL() ? "wss://" : "ws://") + Emulator.getConfig().getValue("ws.nitro.host", "0.0.0.0") + ":" + Emulator.getConfig().getInt("ws.nitro.port", 2096));
    }

    @EventHandler
    public void onUserGetIPEvent(UserGetIPAddressEvent e) {
        Channel channel = e.habbo.getClient().getChannel();
        if(channel != null && channel.hasAttr(main.WS_IP)) {
            String ip = channel.attr(main.WS_IP).get();
            if(!ip.isEmpty()) {
                e.setUpdatedIp(ip);
            }
        }
    }
}
