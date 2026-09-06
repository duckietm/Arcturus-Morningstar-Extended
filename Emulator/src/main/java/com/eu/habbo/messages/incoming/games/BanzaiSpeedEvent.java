package com.eu.habbo.messages.incoming.games;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.games.battlebanzai.BanzaiSpeedService;
import com.eu.habbo.habbohotel.permissions.Permission;
import com.eu.habbo.habbohotel.users.Habbo;
import com.eu.habbo.messages.incoming.MessageHandler;
import com.eu.habbo.messages.outgoing.games.BanzaiSpeedsComposer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * CUSTOM packet 10095: the banzai panel asks for the current timings (mode 0) or staff sets them (mode 1).
 *
 * <p>The timings are hotel-wide, so a change is echoed to every staff member with the panel open rather than
 * only to whoever moved the slider.
 */
public class BanzaiSpeedEvent extends MessageHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(BanzaiSpeedEvent.class);
    private static final int MODE_LIST = 0;
    private static final int MODE_SET = 1;

    @Override
    public void handle() throws Exception {
        Habbo habbo = this.client.getHabbo();
        if (habbo == null) return;

        int mode = this.packet.readInt();
        boolean canEdit = habbo.hasPermission(Permission.ACC_CATALOGFURNI);

        if (mode == MODE_SET) {
            int count = this.packet.readInt();
            int[] values = new int[BanzaiSpeedService.KEYS.size()];

            // Always drain what was sent, even when it will be discarded, or the rest of the stream desyncs.
            for (int index = 0; index < count; index++) {
                int value = this.packet.readInt();
                if (canEdit && index < values.length) values[index] = value;
            }

            if (canEdit && count == values.length && BanzaiSpeedService.apply(values)) {
                LOGGER.info("Banzai speeds set by {}", habbo.getHabboInfo().getUsername());

                for (Habbo online : Emulator.getGameEnvironment().getHabboManager().getOnlineHabbos().values()) {
                    if (online == null || online.getClient() == null) continue;
                    if (!online.hasPermission(Permission.ACC_CATALOGFURNI)) continue;

                    online.getClient().sendResponse(new BanzaiSpeedsComposer(true));
                }

                return;
            }
        }

        this.client.sendResponse(new BanzaiSpeedsComposer(canEdit));
    }
}
