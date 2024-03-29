package com.eu.habbo.habbohotel.items;

import lombok.extern.slf4j.Slf4j;
import java.util.Arrays;

@Slf4j
public class RandomStateParams {
    private int states = -1;
    private int delay = -1;

    public RandomStateParams(String customparams) throws Exception {
        Arrays.stream(customparams.split(",")).forEach(pair -> {
            String[] keyValue = pair.split("=");

            if (keyValue.length != 2) return;

            switch (keyValue[0]) {
                case "states":
                    this.states = Integer.parseInt(keyValue[1]);
                    break;
                case "delay":
                    this.delay = Integer.parseInt(keyValue[1]);
                    break;
                default:
                    log.warn("RandomStateParams: unknown key: " + keyValue[0]);
                    break;
            }
        });

        if (this.states < 0) throw new Exception("RandomStateParams: states not defined");
        if (this.delay < 0) throw new Exception("RandomStateParams: states not defined");
    }

    public int getStates() {
        return states;
    }

    public int getDelay() {
        return delay;
    }
}
