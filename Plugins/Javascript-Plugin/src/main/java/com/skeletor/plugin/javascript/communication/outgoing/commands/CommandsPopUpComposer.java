package com.skeletor.plugin.javascript.communication.outgoing.commands;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.commands.Command;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import com.skeletor.plugin.javascript.communication.outgoing.OutgoingWebMessage;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class CommandsPopUpComposer extends OutgoingWebMessage {
  public CommandsPopUpComposer(List<Command> commands, boolean mod) {
    super("commands_pop_up");
    JsonArray json_cmd = new JsonArray();
    Collections.sort(commands, new Comparator<Command>() {
          public int compare(Command command2, Command command1) {
            return Emulator.getTexts().getValue("commands.description." + command2.permission, "commands.description." + command2.permission).compareTo(Emulator.getTexts().getValue("commands.description." + command1.permission, "commands.description." + command1.permission));
          }
        });
    for (Command c : commands)
      json_cmd.add(Emulator.getTexts().getValue("commands.description." + c.permission, "commands.description." + c.permission)); 
    this.data.add("commands", (JsonElement)json_cmd);
    this.data.add("mod", (JsonElement)new JsonPrimitive(Boolean.valueOf(mod)));
  }
}
