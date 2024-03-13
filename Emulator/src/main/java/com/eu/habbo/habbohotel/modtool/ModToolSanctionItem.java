package com.eu.habbo.habbohotel.modtool;

public class ModToolSanctionItem {
    public int id;
    public int habboId;
    public int sanctionLevel;
    public int probationTimestamp;
    public boolean isMuted;
    public int muteDuration;
    public int tradeLockedUntil;
    public String reason;

    public ModToolSanctionItem(int id, int habboId, int sanctionLevel, int probationTimestamp, boolean isMuted, int muteDuration, int tradeLockedUntil, String reason) {
        this.id = id;
        this.habboId = habboId;
        this.sanctionLevel = sanctionLevel;
        this.probationTimestamp = probationTimestamp;
        this.isMuted = isMuted;
        this.muteDuration = muteDuration;
        this.tradeLockedUntil = tradeLockedUntil;
        this.reason = reason;
    }


}
