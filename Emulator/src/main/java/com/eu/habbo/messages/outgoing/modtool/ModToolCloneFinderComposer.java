package com.eu.habbo.messages.outgoing.modtool;

import com.eu.habbo.messages.ServerMessage;
import com.eu.habbo.messages.outgoing.MessageComposer;
import com.eu.habbo.messages.outgoing.Outgoing;
import java.util.List;

/**
 * CUSTOM 10100 - the accounts sharing the target's IPs / machine id, for the mod tools clone finder.
 *
 * <pre>
 * int    targetId            string targetName
 * string ipCurrent           string ipRegister           string machineId   (the target's own values)
 * int    count
 * loop:  int id, string username, int rank, string rankName, bool online, int lastOnline,
 *        string ipCurrent, string ipRegister, string machineId, int matchMask (1 ip current, 2 ip register, 4 machine)
 * </pre>
 */
public class ModToolCloneFinderComposer extends MessageComposer {

    public static final int MATCH_IP_CURRENT = 1;
    public static final int MATCH_IP_REGISTER = 2;
    public static final int MATCH_MACHINE = 4;

    public record Match(
            int id,
            String username,
            int rank,
            String rankName,
            boolean online,
            int lastOnline,
            String ipCurrent,
            String ipRegister,
            String machineId,
            int matchMask) {}

    private final int targetId;
    private final String targetName;
    private final String ipCurrent;
    private final String ipRegister;
    private final String machineId;
    private final List<Match> matches;

    public ModToolCloneFinderComposer(int targetId, String targetName, String ipCurrent, String ipRegister, String machineId, List<Match> matches) {
        this.targetId = targetId;
        this.targetName = targetName;
        this.ipCurrent = ipCurrent;
        this.ipRegister = ipRegister;
        this.machineId = machineId;
        this.matches = matches;
    }

    @Override
    protected ServerMessage composeInternal() {
        this.response.init(Outgoing.ModToolCloneFinderComposer);
        this.response.appendInt(this.targetId);
        this.response.appendString(this.targetName);
        this.response.appendString(this.ipCurrent);
        this.response.appendString(this.ipRegister);
        this.response.appendString(this.machineId);
        this.response.appendInt(this.matches.size());

        for (Match match : this.matches) {
            this.response.appendInt(match.id());
            this.response.appendString(match.username());
            this.response.appendInt(match.rank());
            this.response.appendString(match.rankName());
            this.response.appendBoolean(match.online());
            this.response.appendInt(match.lastOnline());
            this.response.appendString(match.ipCurrent());
            this.response.appendString(match.ipRegister());
            this.response.appendString(match.machineId());
            this.response.appendInt(match.matchMask());
        }

        return this.response;
    }
}
