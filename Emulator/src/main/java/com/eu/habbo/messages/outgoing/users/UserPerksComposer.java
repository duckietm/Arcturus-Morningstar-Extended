package com.eu.habbo.messages.outgoing.users;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.permissions.Permission;
import com.eu.habbo.habbohotel.users.Habbo;
import com.eu.habbo.messages.ServerMessage;
import com.eu.habbo.messages.outgoing.MessageComposer;
import com.eu.habbo.messages.outgoing.Outgoing;

public class UserPerksComposer extends MessageComposer {
    private final Habbo habbo;

    public UserPerksComposer(Habbo habbo) {
        this.habbo = habbo;
    }

    @Override
    protected ServerMessage composeInternal() {
        this.response.init(Outgoing.UserPerksComposer);
        this.response.appendInt(15);

        this.response.appendString("USE_GUIDE_TOOL");
        this.response.appendString("requirement.unfulfilled.helper_level_4");
        this.response.appendBoolean(Emulator.getGameEnvironment().getPermissionsManager().hasPermission(this.habbo, Permission.ACC_HELPER_USE_GUIDE_TOOL));

        this.response.appendString("GIVE_GUIDE_TOURS");
        this.response.appendString("");
        this.response.appendBoolean(Emulator.getGameEnvironment().getPermissionsManager().hasPermission(this.habbo, "acc_helper_give_guide_tours"));

        this.response.appendString("JUDGE_CHAT_REVIEWS");
        this.response.appendString("requirement.unfulfilled.helper_level_6");
        this.response.appendBoolean(Emulator.getGameEnvironment().getPermissionsManager().hasPermission(this.habbo, "acc_helper_judge_chat_reviews"));

        this.response.appendString("VOTE_IN_COMPETITIONS");
        this.response.appendString("requirement.unfulfilled.helper_level_2");
        this.response.appendBoolean(true);

        this.response.appendString("CALL_ON_HELPERS");
        this.response.appendString("");
        this.response.appendBoolean(true);

        this.response.appendString("CITIZEN");
        this.response.appendString("");
        this.response.appendBoolean(true);

        this.response.appendString("TRADE");
        this.response.appendString("requirement.unfulfilled.no_trade_lock");
        this.response.appendBoolean(this.habbo.getHabboStats().allowTrade());

        this.response.appendString("HEIGHTMAP_EDITOR_BETA");
        this.response.appendString("requirement.unfulfilled.feature_disabled");
        this.response.appendBoolean(Emulator.getGameEnvironment().getPermissionsManager().hasPermission(this.habbo, Permission.ACC_FLOORPLAN_EDITOR));

        this.response.appendString("BUILDER_AT_WORK");
        this.response.appendString("");
        this.response.appendBoolean(true);

        this.response.appendString("CALL_ON_HELPERS");
        this.response.appendString("");
        this.response.appendBoolean(true);

        this.response.appendString("CAMERA");
        this.response.appendString("");
        this.response.appendBoolean(Emulator.getGameEnvironment().getPermissionsManager().hasPermission(this.habbo, "acc_camera"));

        this.response.appendString("NAVIGATOR_PHASE_TWO_2014");
        this.response.appendString("");
        this.response.appendBoolean(true);

        this.response.appendString("MOUSE_ZOOM");
        this.response.appendString("");
        this.response.appendBoolean(true);

        this.response.appendString("NAVIGATOR_ROOM_THUMBNAIL_CAMERA");
        this.response.appendString("");
        this.response.appendBoolean(true);

        this.response.appendString("HABBO_CLUB_OFFER_BETA");
        this.response.appendString("");
        this.response.appendBoolean(true);

        return this.response;
    }
}
