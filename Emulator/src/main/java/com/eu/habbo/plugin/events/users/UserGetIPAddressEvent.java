package com.eu.habbo.plugin.events.users;

import com.eu.habbo.habbohotel.users.Habbo;

public class UserGetIPAddressEvent extends UserEvent{
    public final String oldIp;

    private String updatedIp;
    private boolean changedIP = false;

    public UserGetIPAddressEvent(Habbo habbo, String ip) {
        super(habbo);
        this.oldIp = ip;
    }

    public void setUpdatedIp(String updatedIp) {
        this.updatedIp = updatedIp;
        this.changedIP = true;
    }

    public boolean hasChangedIP() {
        return changedIP;
    }

    public String getUpdatedIp() {
        return updatedIp;
    }
}
