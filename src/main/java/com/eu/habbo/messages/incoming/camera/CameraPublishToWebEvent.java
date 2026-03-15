package com.eu.habbo.messages.incoming.camera;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.users.Habbo;
import com.eu.habbo.messages.incoming.MessageHandler;
import com.eu.habbo.messages.outgoing.camera.CameraPublishWaitMessageComposer;
import com.eu.habbo.messages.outgoing.catalog.NotEnoughPointsTypeComposer;
import com.eu.habbo.plugin.events.users.UserPublishPictureEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class CameraPublishToWebEvent extends MessageHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(CameraPublishToWebEvent.class);

    public static int CAMERA_PUBLISH_POINTS = 5;
    public static int CAMERA_PUBLISH_POINTS_TYPE = 0;

    @Override
    public void handle() throws Exception {
        Habbo habbo = this.client.getHabbo();

        if (habbo == null) return;
        if (habbo.getHabboInfo().getPhotoTimestamp() == 0) return;
        if (habbo.getHabboInfo().getPhotoJSON().isEmpty()) return;
        if (!habbo.getHabboInfo().getPhotoJSON().contains(habbo.getHabboInfo().getPhotoTimestamp() + "")) return;

        if (habbo.getHabboInfo().getCurrencyAmount(CameraPublishToWebEvent.CAMERA_PUBLISH_POINTS_TYPE) < CameraPublishToWebEvent.CAMERA_PUBLISH_POINTS) {
            this.client.sendResponse(new NotEnoughPointsTypeComposer(false, true, CameraPublishToWebEvent.CAMERA_PUBLISH_POINTS));
            return;
        }

        int timestamp = Emulator.getIntUnixTimestamp();

        boolean isOk = false;
        int cooldownLeft = Math.max(0, Emulator.getConfig().getInt("camera.publish.delay") - (timestamp - this.client.getHabbo().getHabboInfo().getWebPublishTimestamp()));

        if (cooldownLeft == 0) {
            UserPublishPictureEvent publishPictureEvent = new UserPublishPictureEvent(this.client.getHabbo(), this.client.getHabbo().getHabboInfo().getPhotoURL(), timestamp, this.client.getHabbo().getHabboInfo().getPhotoRoomId());

            if (!Emulator.getPluginManager().fireEvent(publishPictureEvent).isCancelled()) {
                try (Connection connection = Emulator.getDatabase().getDataSource().getConnection(); PreparedStatement statement = connection.prepareStatement("INSERT INTO camera_web (user_id, room_id, timestamp, url) VALUES (?, ?, ?, ?)")) {
                    statement.setInt(1, this.client.getHabbo().getHabboInfo().getId());
                    statement.setInt(2, publishPictureEvent.roomId);
                    statement.setInt(3, publishPictureEvent.timestamp);
                    statement.setString(4, publishPictureEvent.URL);
                    statement.execute();

                    this.client.getHabbo().getHabboInfo().setWebPublishTimestamp(timestamp);
                    this.client.getHabbo().givePoints(CameraPublishToWebEvent.CAMERA_PUBLISH_POINTS_TYPE, -CameraPublishToWebEvent.CAMERA_PUBLISH_POINTS);

                    isOk = true;
                } catch (SQLException e) {
                    LOGGER.error("Caught SQL exception", e);
                }
            }
        }

        this.client.sendResponse(new CameraPublishWaitMessageComposer(isOk, cooldownLeft, isOk ? this.client.getHabbo().getHabboInfo().getPhotoURL() : ""));
    }
}