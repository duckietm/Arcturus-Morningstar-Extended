package com.eu.camera.handlers;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.users.Habbo;
import com.eu.habbo.habbohotel.users.HabboInfo;
import com.eu.habbo.messages.incoming.MessageHandler;
import com.eu.habbo.messages.outgoing.camera.CameraPublishWaitMessageComposer;
import com.eu.habbo.messages.outgoing.catalog.NotEnoughPointsTypeComposer;
import com.eu.habbo.plugin.events.users.UserPublishPictureEvent;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class PublishPhotoEvent extends MessageHandler {
    public static final int CAMERA_PUBLISH_POINTS = Emulator.getConfig().getInt("camera.price.points.publish", 1);
    public static final int CAMERA_PUBLISH_POINTS_TYPE = Emulator.getConfig().getInt("camera.price.points.publish.type", 5);
    public static final int CAMERA_PUBLISH_DELAY = Emulator.getConfig().getInt("camera.publish.delay", 180);

    @Override
    public void handle() {
        Habbo habbo = this.client.getHabbo();
        HabboInfo habboInfo = habbo.getHabboInfo();

        int points = habboInfo.getCurrencyAmount(CAMERA_PUBLISH_POINTS_TYPE);

        if (points < CAMERA_PUBLISH_POINTS) {
            String currencyName = Emulator.getTexts().getValue("seasonal.name." + Integer.toString(CAMERA_PUBLISH_POINTS_TYPE), "currency");
            String alertMessage = "You don't have enough " + currencyName + "!";
            habbo.alert(alertMessage);
            this.client.sendResponse(new NotEnoughPointsTypeComposer(false, true, CAMERA_PUBLISH_POINTS_TYPE));
            return;
        }

        int photoTimestamp = habboInfo.getPhotoTimestamp();
        String photoJSON = habboInfo.getPhotoJSON();

        if (photoTimestamp == 0 || photoJSON.isEmpty() || !photoJSON.contains(Integer.toString(photoTimestamp))) {
            return;
        }

        int currentTimestamp = Emulator.getIntUnixTimestamp();
        int timeSinceLastPublish = currentTimestamp - habboInfo.getWebPublishTimestamp();

        if (timeSinceLastPublish < CAMERA_PUBLISH_DELAY) {
            int wait = CAMERA_PUBLISH_DELAY - timeSinceLastPublish;
            this.client.sendResponse(new CameraPublishWaitMessageComposer(false, wait, habboInfo.getPhotoURL()));
        } else {
            UserPublishPictureEvent publishPictureEvent = new UserPublishPictureEvent(habbo, habboInfo.getPhotoURL(), currentTimestamp, habboInfo.getPhotoRoomId());

            if (!Emulator.getPluginManager().fireEvent(publishPictureEvent).isCancelled()) {
                try (Connection connection = Emulator.getDatabase().getDataSource().getConnection();
                     PreparedStatement statement = connection.prepareStatement("INSERT INTO camera_web (user_id, room_id, timestamp, url) VALUES (?, ?, ?, ?)")) {
                    statement.setInt(1, habboInfo.getId());
                    statement.setInt(2, publishPictureEvent.roomId);
                    statement.setInt(3, publishPictureEvent.timestamp);
                    statement.setString(4, publishPictureEvent.URL);
                    statement.execute();

                    habboInfo.setWebPublishTimestamp(currentTimestamp);

                    habbo.givePoints(CAMERA_PUBLISH_POINTS_TYPE, -CAMERA_PUBLISH_POINTS);
                } catch (SQLException throwable) {
                    throwable.printStackTrace();
                }
            }

            this.client.sendResponse(new CameraPublishWaitMessageComposer(true, 0, ""));
        }
    }
}
