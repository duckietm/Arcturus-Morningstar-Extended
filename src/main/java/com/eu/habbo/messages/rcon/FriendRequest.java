package com.eu.habbo.messages.rcon;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.messenger.Messenger;
import com.eu.habbo.habbohotel.users.Habbo;
import com.eu.habbo.habbohotel.users.HabboInfo;
import com.eu.habbo.habbohotel.users.HabboManager;
import com.eu.habbo.messages.ServerMessage;
import com.eu.habbo.messages.outgoing.MessageComposer;
import com.eu.habbo.messages.outgoing.Outgoing;
import com.eu.habbo.messages.outgoing.friends.FriendRequestComposer;
import com.google.gson.Gson;

public class FriendRequest extends RCONMessage<FriendRequest.JSON> {
    public FriendRequest() {
        super(FriendRequest.JSON.class);
    }

    @Override
    public void handle(Gson gson, JSON json) {
        if (!Messenger.friendRequested(json.user_id, json.target_id)) {
            Messenger.makeFriendRequest(json.user_id, json.target_id);

            Habbo target = Emulator.getGameEnvironment().getHabboManager().getHabbo(json.target_id);
            if (target != null) {
                Habbo from = Emulator.getGameEnvironment().getHabboManager().getHabbo(json.user_id);

                if (from != null) {
                    target.getClient().sendResponse(new FriendRequestComposer(from));
                } else {
                    final HabboInfo info = HabboManager.getOfflineHabboInfo(json.user_id);

                    if (info != null) {
                        target.getClient().sendResponse(new MessageComposer() {
                            @Override
                            protected ServerMessage composeInternal() {
                                this.response.init(Outgoing.FriendRequestComposer);
                                this.response.appendInt(info.getId());
                                this.response.appendString(info.getUsername());
                                this.response.appendString(info.getLook());
                                return this.response;
                            }
                        });
                    }
                }
            }
        }
    }

    static class JSON {

        public int user_id;


        public int target_id;
    }
}