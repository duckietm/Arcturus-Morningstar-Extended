package com.eu.habbo.messages.outgoing.commands;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.commands.Command;
import com.eu.habbo.messages.ServerMessage;
import com.eu.habbo.messages.outgoing.MessageComposer;
import com.eu.habbo.messages.outgoing.Outgoing;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class AvailableCommandsComposer extends MessageComposer {
    private final List<Command> commands;

    public AvailableCommandsComposer(List<Command> commands) {
        this.commands = commands;
    }

    @Override
    protected ServerMessage composeInternal() {
        this.response.init(Outgoing.AvailableCommandsComposer);

        // getCommandsForRank() has already removed commands the Habbo cannot
        // use. Publish every alias so Nitro autocomplete mirrors the emulator.
        Map<String, AvailableCommand> available = new LinkedHashMap<>();

        if (this.commands != null) {
            for (Command cmd : this.commands) {
                if (cmd == null || cmd.keys == null) continue;

                String description = Emulator.getTexts().getValueQuietly(
                        "commands.description." + cmd.permission,
                        cmd.permission);

                for (String rawKey : cmd.keys) {
                    if (rawKey == null) continue;

                    String key = rawKey.strip();

                    if (key.startsWith(":")) {
                        key = key.substring(1).strip();
                    }

                    if (key.isEmpty()) continue;

                    available.putIfAbsent(
                            key.toLowerCase(Locale.ROOT),
                            new AvailableCommand(key, description));
                }
            }
        }

        this.response.appendInt(available.size());

        for (AvailableCommand command : available.values()) {
            this.response.appendString(command.key());
            this.response.appendString(command.description());
        }

        return this.response;
    }

    /*
     * Kept for the upstream command contract/tests.
     * The Nitro composer itself intentionally exposes every alias above.
     */
    static List<Command> distinctByPrimaryKey(List<Command> commands) {
        LinkedHashMap<String, Command> byPrimaryKey = new LinkedHashMap<>();

        if (commands != null) {
            for (Command command : commands) {
                if (command == null || command.keys == null || command.keys.length == 0) {
                    continue;
                }

                String primaryKey = command.keys[0];

                if (primaryKey == null || primaryKey.isEmpty()) {
                    continue;
                }

                byPrimaryKey.putIfAbsent(primaryKey, command);
            }
        }

        return new ArrayList<>(byPrimaryKey.values());
    }

    private record AvailableCommand(String key, String description) {}
}