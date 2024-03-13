package com.eu.habbo.plugin.events.emulator;

import com.eu.habbo.plugin.Event;

public class SSOAuthenticationEvent extends Event {
    public final String sso;

    public SSOAuthenticationEvent(String sso) {
        this.sso = sso;
    }
}