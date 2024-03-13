package com.eu.camera;

import com.eu.camera.handlers.PublishPhotoEvent;
import com.eu.camera.handlers.PurchasePhotoEvent;
import com.eu.camera.handlers.RenderRoomEvent;
import com.eu.camera.handlers.RenderRoomThumbnailEvent;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.users.Habbo;
import com.eu.habbo.messages.PacketManager;
import com.eu.habbo.messages.incoming.Incoming;
import com.eu.habbo.messages.incoming.MessageHandler;
import com.eu.habbo.plugin.EventHandler;
import com.eu.habbo.plugin.EventListener;
import com.eu.habbo.plugin.HabboPlugin;
import com.eu.habbo.plugin.events.emulator.EmulatorLoadedEvent;

import gnu.trove.map.hash.THashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.lang.reflect.Field;

public class Main extends HabboPlugin implements EventListener {
    private static final Logger LOGGER = LoggerFactory.getLogger(Emulator.class);

    @Override
    public void onEnable() throws Exception {
        Emulator.getPluginManager().registerEvents(this, this);

        if (Emulator.isReady && !Emulator.isShuttingDown) {
            this.onEmulatorLoadedEvent(null);
        }
    }

    @Override
    public void onDisable() throws Exception {
        PacketManager packetManager = Emulator.getGameServer().getPacketManager();
        Field incomingField = PacketManager.class.getDeclaredField("incoming");
        incomingField.setAccessible(true);
        @SuppressWarnings("unchecked")
        THashMap<Integer, Class<? extends MessageHandler>> incoming = (THashMap<Integer, Class<? extends MessageHandler>>)incomingField.get(packetManager);

        // Removes the custom handlers for these packets.
        incoming.remove(Incoming.CameraPublishToWebEvent, PublishPhotoEvent.class);
        incoming.remove(Incoming.CameraPurchaseEvent, PurchasePhotoEvent.class);
        incoming.remove(Incoming.CameraRoomPictureEvent, RenderRoomEvent.class);
        incoming.remove(Incoming.CameraRoomThumbnailEvent, RenderRoomThumbnailEvent.class);
    }

    @Override
    public boolean hasPermission(Habbo habbo, String string) {
        return false;
    }

    @EventHandler
    public void onEmulatorLoadedEvent(EmulatorLoadedEvent event) throws NoSuchFieldException, IllegalArgumentException, IllegalAccessException, Exception {
        // Adds missing SQLs if they are not found.
        Emulator.getConfig().register("camera.url", "http://localhost/camera/");
        Emulator.getConfig().register("imager.location.output.camera", "C:/inetpub/wwwroot/public/camera/");
        Emulator.getConfig().register("imager.location.output.thumbnail", "C:/inetpub/wwwroot/camera/thumbnails/");
        Emulator.getConfig().register("camera.price.points.publish", "1");
        Emulator.getConfig().register("camera.price.points.publish.type", "5");
        Emulator.getConfig().register("camera.publish.delay", "180");
        Emulator.getConfig().register("camera.price.credits", "2");
        Emulator.getConfig().register("camera.price.points", "0");
        Emulator.getConfig().register("camera.price.points.type", "5");
        Emulator.getConfig().register("camera.render.delay", "5");
        Emulator.getTexts().register("camera.permission", "You don't have permission to use the camera!");
        Emulator.getTexts().register("camera.wait", "Please wait %seconds% seconds before making another picture.");
        Emulator.getTexts().register("camera.error.creation", "Failed to create your picture. *sadpanda*");

        PacketManager packetManager = Emulator.getGameServer().getPacketManager();
        Field incomingField = PacketManager.class.getDeclaredField("incoming");
        incomingField.setAccessible(true);
        @SuppressWarnings("unchecked")
        THashMap<Integer, Class<? extends MessageHandler>> incoming = (THashMap<Integer, Class<? extends MessageHandler>>)incomingField.get(packetManager);

        // Removes the current handlers for these packets.
        incoming.remove(Incoming.CameraPublishToWebEvent);
        incoming.remove(Incoming.CameraPurchaseEvent);
        incoming.remove(Incoming.CameraRoomPictureEvent);
        incoming.remove(Incoming.CameraRoomThumbnailEvent);

        // Adds the new handlers for these packets.
        packetManager.registerHandler(Incoming.CameraPublishToWebEvent, PublishPhotoEvent.class);
        packetManager.registerHandler(Incoming.CameraPurchaseEvent, PurchasePhotoEvent.class);
        packetManager.registerHandler(Incoming.CameraRoomPictureEvent, RenderRoomEvent.class);
        packetManager.registerHandler(Incoming.CameraRoomThumbnailEvent, RenderRoomThumbnailEvent.class);

        // Create the output directories if they don't exist.
        File outputDir = new File(Emulator.getConfig().getValue("imager.location.output.thumbnail"));
        if (!outputDir.exists()) {
            outputDir.mkdirs();
        }

        LOGGER.info("[Camera] Plugin has loaded!");
    }
}
