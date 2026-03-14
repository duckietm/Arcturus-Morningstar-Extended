package com.eu.habbo.networking.camera.messages.incoming;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.achievements.AchievementManager;
import com.eu.habbo.habbohotel.users.Habbo;
import com.eu.habbo.messages.outgoing.camera.CameraURLComposer;
import com.eu.habbo.networking.camera.CameraIncomingMessage;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;

public class CameraResultURLEvent extends CameraIncomingMessage {
    public final static int STATUS_OK = 0;
    public final static int STATUS_ERROR = 1;

    public CameraResultURLEvent(Short header, ByteBuf body) {
        super(header, body);
    }

    @Override
    public void handle(Channel client) throws Exception {
        int userId = this.readInt();
        int status = this.readInt();
        String URL = this.readString();

        if (!Emulator.getConfig().getBoolean("camera.use.https", true)) {
            URL = URL.replace("https://", "http://");
        }

        int roomId = this.readInt();
        int timestamp = this.readInt();

        Habbo habbo = Emulator.getGameEnvironment().getHabboManager().getHabbo(userId);

        if (status == STATUS_ERROR) {
            if (habbo != null) {
                habbo.getHabboInfo().setPhotoTimestamp(0);
                habbo.getHabboInfo().setPhotoJSON("");
                habbo.getHabboInfo().setPhotoURL("");

                habbo.alert(Emulator.getTexts().getValue("camera.error.creation"));
                return;
            }
        }

        if (status == STATUS_OK) {
            if (habbo != null) {
                if (timestamp == habbo.getHabboInfo().getPhotoTimestamp()) {
                    AchievementManager.progressAchievement(habbo, Emulator.getGameEnvironment().getAchievementManager().getAchievement("CameraPhotoCount"), 1);
                    habbo.getClient().sendResponse(new CameraURLComposer(URL));
                    habbo.getHabboInfo().setPhotoJSON(habbo.getHabboInfo().getPhotoJSON().replace("%room_id%", roomId + "").replace("%url%", URL));
                    habbo.getHabboInfo().setPhotoURL(URL);
                }
            }
        }
    }
}