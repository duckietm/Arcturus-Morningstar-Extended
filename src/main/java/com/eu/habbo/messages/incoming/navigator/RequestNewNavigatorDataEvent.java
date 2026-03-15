package com.eu.habbo.messages.incoming.navigator;

import com.eu.habbo.messages.incoming.MessageHandler;
import com.eu.habbo.messages.outgoing.navigator.*;

public class RequestNewNavigatorDataEvent extends MessageHandler {
    @Override
    public void handle() throws Exception {
        this.client.sendResponse(new NewNavigatorSettingsComposer(this.client.getHabbo().getHabboStats().navigatorWindowSettings));
        this.client.sendResponse(new NewNavigatorMetaDataComposer());
        this.client.sendResponse(new NewNavigatorLiftedRoomsComposer());
        this.client.sendResponse(new NewNavigatorCollapsedCategoriesComposer());
        this.client.sendResponse(new NewNavigatorSavedSearchesComposer(this.client.getHabbo().getHabboInfo().getSavedSearches()));
        this.client.sendResponse(new NewNavigatorEventCategoriesComposer());
    }
}
