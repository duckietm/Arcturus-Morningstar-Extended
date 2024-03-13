package com.eu.habbo.plugin.events.users;

import com.eu.habbo.habbohotel.users.Habbo;

public class UserExitRoomEvent extends UserEvent {
    public final UserExitRoomReason reason;

    public UserExitRoomEvent(Habbo habbo, UserExitRoomReason reason) {
        super(habbo);
        this.reason = reason;
    }


    public enum UserExitRoomReason {
        DOOR(false),
        KICKED_HABBO(false),
        KICKED_IDLE(true),
        TELEPORT(false);

        public final boolean cancellable;

        UserExitRoomReason(boolean cancellable) {
            this.cancellable = cancellable;
        }
    }
}