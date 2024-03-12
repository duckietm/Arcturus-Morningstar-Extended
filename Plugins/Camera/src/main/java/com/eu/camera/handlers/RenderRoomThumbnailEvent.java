package com.eu.camera.handlers;

import com.eu.habbo.Emulator;
import com.eu.habbo.messages.incoming.MessageHandler;
import com.eu.habbo.habbohotel.rooms.Room;
import com.eu.habbo.habbohotel.users.Habbo;
import com.eu.habbo.habbohotel.users.HabboInfo;
import com.eu.habbo.habbohotel.users.HabboStats;
import com.eu.habbo.messages.outgoing.camera.CameraRoomThumbnailSavedComposer;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import io.netty.buffer.ByteBufUtil;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public class RenderRoomThumbnailEvent extends MessageHandler {
    public static final int CAMERA_RENDER_DELAY = Emulator.getConfig().getInt("camera.render.delay", 5);

    private ByteBuf image = null;

    @Override
    public void handle() {
        try {
            make();
        } finally {
            if (this.image != null) {
                this.image.release();
            }
        }
    }

    private void make() {
        Habbo habbo = this.client.getHabbo();
        if (!habbo.hasPermission("acc_camera")) {
            habbo.alert(Emulator.getTexts().getValue("camera.permission"));
            return;
        }

        HabboStats habboStats = habbo.getHabboStats();

        int timestamp = Emulator.getIntUnixTimestamp();

        if (habboStats.cache.containsKey("camera_render_cooldown")) {
            int cameraTimestamp = (int) habboStats.cache.get("camera_render_cooldown");
            if (timestamp - cameraTimestamp < CAMERA_RENDER_DELAY) {
                String alertMessage = Emulator.getTexts().getValue("camera.wait")
                        .replace("%seconds%", Integer.toString(timestamp - cameraTimestamp));
                habbo.alert(alertMessage);
                return;
            }
        }
        habboStats.cache.put("camera_render_cooldown", (Object) timestamp);

        HabboInfo habboInfo = habbo.getHabboInfo();

        Room room = habboInfo.getCurrentRoom();
        if (room == null || !room.isOwner(habbo)) {
            return;
        }

        final int count = this.packet.readInt();
        this.image = this.packet.getBuffer().readBytes(count);
        if (this.image == null || !isValidImage(this.image)) {
            return;
        }

        BufferedImage theImage = null;
        try (ByteBufInputStream in = new ByteBufInputStream(this.image)) {
            theImage = ImageIO.read(in);
        } catch (IOException e) {
            e.printStackTrace();
            habbo.alert(Emulator.getTexts().getValue("camera.error.creation"));
            return;
        }

        File imageFile = new File(Emulator.getConfig().getValue("imager.location.output.thumbnail") + room.getId() + ".png");

        try {
            ImageIO.write(theImage, "png", imageFile);
        } catch (IOException e) {
            e.printStackTrace();
            habbo.alert(Emulator.getTexts().getValue("camera.error.creation"));
            return;
        }

        this.client.sendResponse(new CameraRoomThumbnailSavedComposer());
    }

    private boolean isValidImage(ByteBuf imageBuffer) {
        byte[] imageBytes = ByteBufUtil.getBytes(imageBuffer, 0, 4, true);
        return (imageBytes != null && imageBytes.length >= 4 &&
                imageBytes[0] == -119 && imageBytes[1] == 80 && imageBytes[2] == 78 && imageBytes[3] == 71);
    }
}
