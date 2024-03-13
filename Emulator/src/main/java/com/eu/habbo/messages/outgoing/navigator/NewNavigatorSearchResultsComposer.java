package com.eu.habbo.messages.outgoing.navigator;

import com.eu.habbo.habbohotel.navigation.SearchResultList;
import com.eu.habbo.messages.ServerMessage;
import com.eu.habbo.messages.outgoing.MessageComposer;
import com.eu.habbo.messages.outgoing.Outgoing;

import java.util.List;

public class NewNavigatorSearchResultsComposer extends MessageComposer {
    private final String searchCode;
    private final String searchQuery;
    private final List<SearchResultList> resultList;

    public NewNavigatorSearchResultsComposer(String searchCode, String searchQuery, List<SearchResultList> resultList) {
        this.searchCode = searchCode;
        this.searchQuery = searchQuery;
        this.resultList = resultList;
    }

    @Override
    protected ServerMessage composeInternal() {
        this.response.init(Outgoing.NewNavigatorSearchResultsComposer);
        this.response.appendString(this.searchCode);
        this.response.appendString(this.searchQuery);

        this.response.appendInt(this.resultList.size()); //Count

        for (SearchResultList item : this.resultList) {
            item.serialize(this.response);
        }

        return this.response;
    }


}
