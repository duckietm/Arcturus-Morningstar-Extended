package com.eu.habbo.messages.outgoing.hotelview;

import com.eu.habbo.habbohotel.hotelview.HallOfFame;
import com.eu.habbo.habbohotel.hotelview.HallOfFameWinner;
import com.eu.habbo.messages.ServerMessage;
import com.eu.habbo.messages.outgoing.MessageComposer;
import com.eu.habbo.messages.outgoing.Outgoing;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class HallOfFameComposer extends MessageComposer {
    private final HallOfFame hallOfFame;

    public HallOfFameComposer(HallOfFame hallOfFame) {
        this.hallOfFame = hallOfFame;
    }

    @Override
    protected ServerMessage composeInternal() {
        this.response.init(Outgoing.HallOfFameComposer);
        this.response.appendString(this.hallOfFame.getCompetitionName());
        this.response.appendInt(this.hallOfFame.getWinners().size());

        int count = 1;

        List<HallOfFameWinner> winners = new ArrayList<>(this.hallOfFame.getWinners().values());
        Collections.sort(winners);
        for (HallOfFameWinner winner : winners) {
            this.response.appendInt(winner.getId());
            this.response.appendString(winner.getUsername());
            this.response.appendString(winner.getLook());
            this.response.appendInt(count);
            this.response.appendInt(winner.getPoints());
            count++;
        }
        return this.response;
    }
}
