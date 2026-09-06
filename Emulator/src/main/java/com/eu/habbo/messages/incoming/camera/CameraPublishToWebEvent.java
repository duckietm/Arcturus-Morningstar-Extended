package com.eu.habbo.messages.incoming.camera;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.users.Habbo;
import com.eu.habbo.habbohotel.users.HabboInfo;
import com.eu.habbo.messages.incoming.MessageHandler;
import com.eu.habbo.messages.outgoing.camera.CameraPublishWaitMessageComposer;
import com.eu.habbo.messages.outgoing.catalog.NotEnoughPointsTypeComposer;
import com.eu.habbo.plugin.events.users.UserPublishPictureEvent;
import com.eu.habbo.plugin.PluginEventInputGuard;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class CameraPublishToWebEvent extends MessageHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(CameraPublishToWebEvent.class);

    public static int CAMERA_PUBLISH_POINTS = 1;
    public static int CAMERA_PUBLISH_POINTS_TYPE = 5;
    public static int CAMERA_PUBLISH_DELAY = 180;

    @Override
    public void handle() {
        Habbo habbo = this.client.getHabbo();
        if (habbo == null) return;

        HabboInfo habboInfo = habbo.getHabboInfo();

        int publishPoints = publishPoints();
        int publishPointsType = publishPointsType();
        int publishDelay = publishDelay();

        int points = habboInfo.getCurrencyAmount(publishPointsType);
        if (points < publishPoints) {
            String currencyName = Emulator.getTexts().getValue("seasonal.name." + publishPointsType, "currency");
            habbo.alert("You don't have enough " + currencyName + "!");
            this.client.sendResponse(new NotEnoughPointsTypeComposer(false, true, publishPointsType));
            return;
        }

        int photoTimestamp = habboInfo.getPhotoTimestamp();
        String photoJSON = habboInfo.getPhotoJSON();
        if (photoTimestamp == 0 || photoJSON == null || photoJSON.isEmpty() || !photoJSON.contains(Integer.toString(photoTimestamp)))
            return;

        int currentTimestamp = Emulator.getIntUnixTimestamp();
        int timeSinceLastPublish = currentTimestamp - habboInfo.getWebPublishTimestamp();

        if (timeSinceLastPublish < publishDelay) {
            int wait = publishDelay - timeSinceLastPublish;
            this.client.sendResponse(new CameraPublishWaitMessageComposer(false, wait, habboInfo.getPhotoURL()));
        } else {
            UserPublishPictureEvent publishPictureEvent = new UserPublishPictureEvent(habbo, habboInfo.getPhotoURL(), currentTimestamp, habboInfo.getPhotoRoomId());

            if (!Emulator.getPluginManager().fireEvent(publishPictureEvent).isCancelled()) {
                if (!PluginEventInputGuard.isPositiveId(publishPictureEvent.roomId)
                        || Emulator.getGameEnvironment().getRoomManager().loadRoom(publishPictureEvent.roomId) == null) {
                    return;
                }

                try (Connection connection = Emulator.getDatabase().getDataSource().getConnection();
                     PreparedStatement statement = connection.prepareStatement("INSERT INTO camera_web (user_id, room_id, timestamp, url) VALUES (?, ?, ?, ?)")) {
                    statement.setInt(1, habboInfo.getId());
                    statement.setInt(2, publishPictureEvent.roomId);
                    statement.setInt(3, publishPictureEvent.timestamp);
                    statement.setString(4, publishPictureEvent.URL);
                    statement.execute();
                    habboInfo.setWebPublishTimestamp(currentTimestamp);
                    if (publishPoints > 0) habbo.givePoints(publishPointsType, -publishPoints);
                } catch (SQLException e) {
                    LOGGER.error("Caught SQL exception", e);
                }
            }
            this.client.sendResponse(new CameraPublishWaitMessageComposer(true, 0, ""));
        }
    }

    /**
     * Read the publish price from the same camera.* settings that
     * RequestCameraConfigurationEvent advertises (statics stay as fallbacks).
     */
    public static int publishPoints() {
        return Emulator.getConfig().getInt("camera.price.points.publish", CAMERA_PUBLISH_POINTS);
    }

    public static int publishPointsType() {
        return Emulator.getConfig().getInt("camera.price.points.publish.type", CAMERA_PUBLISH_POINTS_TYPE);
    }

    public static int publishDelay() {
        return Emulator.getConfig().getInt("camera.publish.delay", CAMERA_PUBLISH_DELAY);
    }
}
