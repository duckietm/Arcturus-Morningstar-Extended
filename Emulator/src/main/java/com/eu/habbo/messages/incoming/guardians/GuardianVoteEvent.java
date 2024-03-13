package com.eu.habbo.messages.incoming.guardians;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.guides.GuardianTicket;
import com.eu.habbo.habbohotel.guides.GuardianVoteType;
import com.eu.habbo.messages.incoming.MessageHandler;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class GuardianVoteEvent extends MessageHandler {

    @Override
    public void handle() throws Exception {
        int voteType = this.packet.readInt();

        GuardianTicket ticket = Emulator.getGameEnvironment().getGuideManager().getTicketForGuardian(this.client.getHabbo());

        if (ticket != null) {
            GuardianVoteType type = GuardianVoteType.NOT_VOTED;

            if (voteType == 0) {
                type = GuardianVoteType.ACCEPTABLY;
            } else if (voteType == 1) {
                type = GuardianVoteType.BADLY;
            } else if (voteType == 2) {
                type = GuardianVoteType.AWFULLY;
            } else {
                log.error("Uknown vote type: " + voteType);
            }

            ticket.vote(this.client.getHabbo(), type);
        }
    }
}
