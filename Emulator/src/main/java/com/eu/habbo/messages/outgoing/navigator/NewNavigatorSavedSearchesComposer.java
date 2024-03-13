package com.eu.habbo.messages.outgoing.navigator;

import com.eu.habbo.habbohotel.navigation.NavigatorSavedSearch;
import com.eu.habbo.messages.ServerMessage;
import com.eu.habbo.messages.outgoing.MessageComposer;
import com.eu.habbo.messages.outgoing.Outgoing;

import java.util.HashSet;
import java.util.List;

public class NewNavigatorSavedSearchesComposer extends MessageComposer {
    private final List<NavigatorSavedSearch> searches;

    public NewNavigatorSavedSearchesComposer(List<NavigatorSavedSearch> searches) {
        this.searches = searches;
    }

    @Override
    protected ServerMessage composeInternal() {
        this.response.init(Outgoing.NewNavigatorSavedSearchesComposer);
        this.response.appendInt(this.searches.size());

        for (NavigatorSavedSearch search : this.searches) {
            this.response.appendInt(search.getId());
            this.response.appendString(search.getSearchCode());
            this.response.appendString(search.getFilter() == null ? "" : search.getFilter());
            this.response.appendString("");
        }

        return this.response;
    }
}
