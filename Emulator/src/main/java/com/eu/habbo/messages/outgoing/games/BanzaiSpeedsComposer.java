package com.eu.habbo.messages.outgoing.games;

import com.eu.habbo.habbohotel.games.battlebanzai.BanzaiSpeedService;
import com.eu.habbo.messages.ServerMessage;
import com.eu.habbo.messages.outgoing.MessageComposer;
import com.eu.habbo.messages.outgoing.Outgoing;

/**
 * CUSTOM packet 10092: the hotel-wide Battle Banzai teleport timings, for the staff tuning panel.
 *
 * <p>Values are sent in BanzaiSpeedService.KEYS order together with the range the server will accept, so the
 * panel can bound its own inputs instead of guessing and having them silently clamped.
 */
public class BanzaiSpeedsComposer extends MessageComposer {
    private final boolean canEdit;

    public BanzaiSpeedsComposer(boolean canEdit) {
        this.canEdit = canEdit;
    }

    @Override
    protected ServerMessage composeInternal() {
        this.response.init(Outgoing.BanzaiSpeedsComposer);
        this.response.appendBoolean(this.canEdit);
        this.response.appendInt(BanzaiSpeedService.MINIMUM_MS);
        this.response.appendInt(BanzaiSpeedService.MAXIMUM_MS);

        int[] values = BanzaiSpeedService.current();
        this.response.appendInt(values.length);

        int index = 0;
        for (String key : BanzaiSpeedService.KEYS.keySet()) {
            this.response.appendString(key);
            this.response.appendInt(values[index++]);
        }

        return this.response;
    }
}
