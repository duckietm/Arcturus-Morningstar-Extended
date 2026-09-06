package com.eu.habbo.messages.outgoing.users;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.permissions.Permission;
import com.eu.habbo.habbohotel.permissions.Rank;
import com.eu.habbo.habbohotel.rooms.Room;
import com.eu.habbo.habbohotel.users.Habbo;
import com.eu.habbo.habbohotel.users.HabboInfo;
import com.eu.habbo.messages.ServerMessage;
import com.eu.habbo.messages.outgoing.MessageComposer;
import com.eu.habbo.messages.outgoing.Outgoing;
import java.util.Comparator;
import java.util.List;

/**
 * CUSTOM packet 10088: everyone online with rank, look, room and idle state, for the searchable ":online" window.
 * The first boolean tells the client whether the viewer is staff (extra actions in the window).
 */
public class OnlineUsersComposer extends MessageComposer {
    private final Habbo viewer;

    public OnlineUsersComposer(Habbo viewer) {
        this.viewer = viewer;
    }

    public static boolean isStaff(Habbo habbo) {
        if (habbo == null || habbo.getHabboInfo() == null || habbo.getHabboInfo().getRank() == null) return false;
        int minStaffRank = Emulator.getConfig().getInt("commands.cmd_staffonline.min_rank", 4);
        return habbo.getHabboInfo().getRank().getId() >= minStaffRank
                || habbo.hasPermission(Permission.ACC_SUPPORTTOOL);
    }

    @Override
    protected ServerMessage composeInternal() {
        this.response.init(Outgoing.OnlineUsersComposer);

        List<Habbo> online = Emulator.getGameEnvironment()
                .getHabboManager()
                .getOnlineHabbos()
                .values()
                .stream()
                .filter(habbo -> habbo != null && habbo.isOnline() && habbo.getHabboInfo() != null)
                .sorted(Comparator.comparing(
                        habbo -> habbo.getHabboInfo().getUsername(), String.CASE_INSENSITIVE_ORDER))
                .toList();

        this.response.appendBoolean(isStaff(this.viewer));
        this.response.appendInt(online.size());

        for (Habbo habbo : online) {
            HabboInfo info = habbo.getHabboInfo();
            Rank rank = info.getRank();
            Room room = info.getCurrentRoom();

            this.response.appendInt(info.getId());
            this.response.appendString(info.getUsername());
            this.response.appendString(info.getLook() == null ? "" : info.getLook());
            this.response.appendString(info.getGender() == null ? "M" : info.getGender().name());
            this.response.appendString(info.getMotto() == null ? "" : info.getMotto());
            this.response.appendInt(rank == null ? 0 : rank.getId());
            this.response.appendString(rank == null || rank.getName() == null ? "" : rank.getName());
            this.response.appendString(rank == null || rank.getBadge() == null ? "" : rank.getBadge());
            this.response.appendBoolean(isStaff(habbo));
            this.response.appendInt(room == null ? 0 : room.getId());
            this.response.appendString(room == null || room.getName() == null ? "" : room.getName());
            this.response.appendString(room == null || room.getOwnerName() == null ? "" : room.getOwnerName());
            this.response.appendBoolean(habbo.getRoomUnit() != null && habbo.getRoomUnit().isIdle());
        }

        return this.response;
    }
}
