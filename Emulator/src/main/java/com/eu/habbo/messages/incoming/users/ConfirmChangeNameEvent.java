package com.eu.habbo.messages.incoming.users;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.rooms.Room;
import com.eu.habbo.habbohotel.users.HabboInfo;
import com.eu.habbo.habbohotel.users.HabboManager;
import com.eu.habbo.messages.incoming.MessageHandler;
import com.eu.habbo.messages.outgoing.rooms.users.ChangeNameUpdatedComposer;
import com.eu.habbo.messages.outgoing.rooms.users.RoomUserNameChangedComposer;
import com.eu.habbo.messages.outgoing.users.ChangeNameCheckResultComposer;
import com.eu.habbo.messages.outgoing.users.UserDataComposer;
import com.eu.habbo.plugin.events.users.UserNameChangedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class ConfirmChangeNameEvent extends MessageHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(ConfirmChangeNameEvent.class);

    public static final List<String> changingUsernames = new ArrayList<>(2);

    @Override
    public void handle() throws Exception {
        if (!this.client.getHabbo().getHabboStats().allowNameChange)
            return;

        String name = this.packet.readString();

        if (name.equalsIgnoreCase(this.client.getHabbo().getHabboInfo().getUsername())) {
            this.client.getHabbo().getHabboStats().allowNameChange = false;
            this.client.sendResponse(new ChangeNameUpdatedComposer(this.client.getHabbo()));
            this.client.sendResponse(new RoomUserNameChangedComposer(this.client.getHabbo()).compose());
            this.client.sendResponse(new UserDataComposer(this.client.getHabbo()));
            return;
        }

        if (name.equals(this.client.getHabbo().getHabboStats().changeNameChecked)) {
            HabboInfo habboInfo = HabboManager.getOfflineHabboInfo(name);

            if (habboInfo == null) {
                synchronized (changingUsernames) {
                    if (changingUsernames.contains(name))
                        return;

                    changingUsernames.add(name);
                }

                String oldName = this.client.getHabbo().getHabboInfo().getUsername();
                this.client.getHabbo().getHabboStats().allowNameChange = false;
                this.client.getHabbo().getHabboInfo().setUsername(name);
                this.client.getHabbo().getHabboInfo().run();

                Emulator.getPluginManager().fireEvent(new UserNameChangedEvent(this.client.getHabbo(), oldName));
                for (Room room : Emulator.getGameEnvironment().getRoomManager().getRoomsForHabbo(this.client.getHabbo())) {
                    room.setOwnerName(name);
                    room.setNeedsUpdate(true);
                    room.save();
                }

                synchronized (changingUsernames) {
                    changingUsernames.remove(name);
                }

                this.client.sendResponse(new ChangeNameUpdatedComposer(this.client.getHabbo()));

                if (this.client.getHabbo().getHabboInfo().getCurrentRoom() != null) {
                    this.client.getHabbo().getHabboInfo().getCurrentRoom().sendComposer(new RoomUserNameChangedComposer(this.client.getHabbo()).compose());
                } else {
                    this.client.sendResponse(new RoomUserNameChangedComposer(this.client.getHabbo()).compose());
                }

                this.client.getHabbo().getMessenger().connectionChanged(this.client.getHabbo(), true, this.client.getHabbo().getHabboInfo().getCurrentRoom() != null);
                this.client.getHabbo().getClient().sendResponse(new UserDataComposer(this.client.getHabbo()));

                try (Connection connection = Emulator.getDatabase().getDataSource().getConnection(); PreparedStatement statement = connection.prepareStatement("INSERT INTO namechange_log (user_id, old_name, new_name, timestamp) VALUES (?, ?, ?, ?) ")) {
                    statement.setInt(1, this.client.getHabbo().getHabboInfo().getId());
                    statement.setString(2, oldName);
                    statement.setString(3, name);
                    statement.setInt(4, Emulator.getIntUnixTimestamp());
                    statement.execute();
                } catch (SQLException e) {
                    LOGGER.error("Caught SQL exception", e);
                }
            } else {
                this.client.sendResponse(new ChangeNameCheckResultComposer(ChangeNameCheckResultComposer.TAKEN_WITH_SUGGESTIONS, name, new ArrayList<>()));
            }
        }
    }
}
