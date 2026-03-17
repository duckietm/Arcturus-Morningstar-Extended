package com.eu.habbo.messages.incoming.camera;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.rooms.Room;
import com.eu.habbo.habbohotel.users.Habbo;
import com.eu.habbo.habbohotel.users.HabboInfo;
import com.eu.habbo.habbohotel.users.HabboStats;
import com.eu.habbo.messages.incoming.MessageHandler;
import com.eu.habbo.messages.outgoing.camera.CameraURLComposer;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import io.netty.buffer.ByteBufUtil;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public class CameraRoomPictureEvent extends MessageHandler {
    public static int CAMERA_RENDER_DELAY = 5;

    private ByteBuf image = null;

    @Override
    public void handle() {
        try {
            this.make();
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

        HabboInfo habboInfo = habbo.getHabboInfo();
        HabboStats habboStats = habbo.getHabboStats();
        int timestamp = Emulator.getIntUnixTimestamp();

        if (habboStats.cache.containsKey("camera_render_cooldown")) {
            int cameraTimestamp = (Integer) habboStats.cache.get("camera_render_cooldown");
            if (timestamp - cameraTimestamp < CAMERA_RENDER_DELAY) {
                String alertMessage = Emulator.getTexts().getValue("camera.wait").replace("%seconds%", Integer.toString(timestamp - cameraTimestamp));
                habbo.alert(alertMessage);
                if (habboInfo.getPhotoURL() != null) {
                    String[] splittedPhotoURL = habboInfo.getPhotoURL().split("/");
                    if (splittedPhotoURL.length > 0) {
                        this.client.sendResponse(new CameraURLComposer(splittedPhotoURL[splittedPhotoURL.length - 1]));
                    }
                }
                return;
            }
        }

        habboStats.cache.put("camera_render_cooldown", timestamp);
        Room room = habboInfo.getCurrentRoom();
        if (room == null) return;

        int count = this.packet.readInt();
        this.image = this.packet.getBuffer().readBytes(count);
        if (this.image == null) return;

        byte[] imageBytes = ByteBufUtil.getBytes(this.image, 0, 4, true);
        if (imageBytes == null || imageBytes.length < 4 || !isPNG(imageBytes)) return;

        BufferedImage theImage;
        try (ByteBufInputStream in = new ByteBufInputStream(this.image)) {
            theImage = ImageIO.read(in);
        } catch (IOException e) {
            handleImageProcessingError(habbo);
            return;
        }

        String fileName = habboInfo.getId() + "_" + timestamp;
        String URL = fileName + ".png";
        String URLsmall = fileName + "_small.png";
        String base = Emulator.getConfig().getValue("camera.url");
        String json = Emulator.getConfig().getValue("camera.extradata")
                .replace("%timestamp%", Integer.toString(timestamp))
                .replace("%room_id%", Integer.toString(room.getId()))
                .replace("%url%", base + URL);
        habboInfo.setPhotoURL(base + URL);
        habboInfo.setPhotoTimestamp(timestamp);
        habboInfo.setPhotoRoomId(room.getId());
        habboInfo.setPhotoJSON(json);

        File imageFile = new File(Emulator.getConfig().getValue("imager.location.output.camera") + URL);
        File smallImageFile = new File(Emulator.getConfig().getValue("imager.location.output.camera") + URLsmall);

        try {
            ImageIO.write(theImage, "png", imageFile);
            Image smallImage = theImage.getScaledInstance(theImage.getWidth(null) / 2, theImage.getHeight(null) / 2, Image.SCALE_SMOOTH);
            BufferedImage bi = new BufferedImage(smallImage.getWidth(null), smallImage.getHeight(null), BufferedImage.TYPE_INT_ARGB);
            Graphics2D graphics2D = bi.createGraphics();
            graphics2D.drawImage(smallImage, 0, 0, null);
            graphics2D.dispose();
            ImageIO.write(bi, "png", smallImageFile);
        } catch (IOException e) {
            handleImageProcessingError(habbo);
            return;
        }

        this.client.sendResponse(new CameraURLComposer(URL));
    }

    private boolean isPNG(byte[] bytes) {
        return bytes[0] == (byte) 0x89 && bytes[1] == 0x50 && bytes[2] == 0x4E && bytes[3] == 0x47;
    }

    private void handleImageProcessingError(Habbo habbo) {
        habbo.alert(Emulator.getTexts().getValue("camera.error.creation"));
    }
}
