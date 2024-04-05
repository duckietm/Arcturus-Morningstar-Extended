package com.eu.habbo.core;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.users.Habbo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class GotwPointsScheduler extends Scheduler {

    private static final Logger LOGGER = LoggerFactory.getLogger(GotwPointsScheduler.class);

    public static boolean IGNORE_HOTEL_VIEW;
    public static boolean IGNORE_IDLED;
    public static String GOTW_POINTS_NAME;
    public static double HC_MODIFIER;

    public GotwPointsScheduler() { //TODO MOVE TO A PLUGIN. IS NOT PART OF OFFICIAL HABBO.

        super(Emulator.getConfig().getInt("hotel.auto.gotwpoints.interval"));
        this.reloadConfig();
    }

    public void reloadConfig() {
        if (Emulator.getConfig().getBoolean("hotel.auto.gotwpoints.enabled")) {
            IGNORE_HOTEL_VIEW = Emulator.getConfig().getBoolean("hotel.auto.gotwpoints.ignore.hotelview");
            IGNORE_IDLED = Emulator.getConfig().getBoolean("hotel.auto.gotwpoints.ignore.idled");
            HC_MODIFIER = Emulator.getConfig().getDouble("hotel.auto.gotwpoints.hc_modifier", 1.0);
            GOTW_POINTS_NAME =  Emulator.getConfig().getValue("hotel.auto.gotwpoints.name");

            if (this.disposed) {
                this.disposed = false;
                this.run();
            }
        } else {
            this.disposed = true;
        }
    }

    @Override
    public void run() {
        super.run();

        Habbo habbo;
        for (Map.Entry<Integer, Habbo> map : Emulator.getGameEnvironment().getHabboManager().getOnlineHabbos().entrySet()) {
            habbo = map.getValue();

            try {
                if (habbo != null) {
                    if (habbo.getHabboInfo().getCurrentRoom() == null && IGNORE_HOTEL_VIEW)
                        continue;

                    if (habbo.getRoomUnit().isIdle() && IGNORE_IDLED)
                        continue;

                    int type;
                    boolean found = false;
                    for (String s : Emulator.getConfig().getValue("seasonal.currency.names").split(";")) {
                        if (s.equalsIgnoreCase(GOTW_POINTS_NAME) || (GOTW_POINTS_NAME.startsWith(s) && Math.abs(s.length() - GOTW_POINTS_NAME.length()) < 3)) {
                            found = true;
                            break;
                        }
                    }
                    type = Emulator.getConfig().getInt("seasonal.currency." + GOTW_POINTS_NAME, -1);
                    if (found || type != -1) {
                        habbo.givePoints(type, (int)(habbo.getHabboInfo().getRank().getGotwTimerAmount() * (habbo.getHabboStats().hasActiveClub() ? HC_MODIFIER : 1.0)));
                    }
                }
            } catch (Exception e) {
                LOGGER.error("Caught exception", e);
            }
        }
    }

    public boolean isDisposed() {
        return this.disposed;
    }

    public void setDisposed(boolean disposed) {
        this.disposed = disposed;
    }
}
