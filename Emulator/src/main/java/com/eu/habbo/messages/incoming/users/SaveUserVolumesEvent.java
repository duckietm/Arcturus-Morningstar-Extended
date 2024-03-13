package com.eu.habbo.messages.incoming.users;

import com.eu.habbo.Emulator;
import com.eu.habbo.messages.incoming.MessageHandler;
import com.eu.habbo.plugin.events.users.UserSavedSettingsEvent;

public class SaveUserVolumesEvent extends MessageHandler {
    @Override
    public void handle() throws Exception {
        int system = this.packet.readInt();
        int furni = this.packet.readInt();
        int trax = this.packet.readInt();

        this.client.getHabbo().getHabboStats().volumeSystem = system;
        this.client.getHabbo().getHabboStats().volumeFurni = furni;
        this.client.getHabbo().getHabboStats().volumeTrax = trax;

        Emulator.getPluginManager().fireEvent(new UserSavedSettingsEvent(this.client.getHabbo()));
    }
}
