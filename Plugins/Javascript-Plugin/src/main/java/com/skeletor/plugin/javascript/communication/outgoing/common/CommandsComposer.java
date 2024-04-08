package com.skeletor.plugin.javascript.communication.outgoing.common;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.commands.Command;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.skeletor.plugin.javascript.categories.Category;
import com.skeletor.plugin.javascript.communication.outgoing.OutgoingWebMessage;
import com.skeletor.plugin.javascript.main;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class CommandsComposer extends OutgoingWebMessage {
  public CommandsComposer(List<Command> commands) {
    super("commands");
    JsonArray json_cmd = new JsonArray();
    Collections.sort(commands, new Comparator<Command>() {
          public int compare(Command command2, Command command1) {
            return Emulator.getTexts().getValue("commands.description." + command2.permission, "commands.description." + command2.permission).compareTo(Emulator.getTexts().getValue("commands.description." + command1.permission, "commands.description." + command1.permission));
          }
        });
    List<Command> duplicateCommands = new ArrayList<>(commands);
    List<String> tempList = new ArrayList<>();
    boolean hasPermission = false;
    for (Category category : main.getCommandManager().getCommandCategories()) {
      tempList = new ArrayList<>();
      hasPermission = false;
      if (category.getPermissions().size() > 0) {
        for (String permission : category.getPermissions()) {
          for (Command command : commands) {
            if (command.permission.equals(permission)) {
              duplicateCommands.remove(command);
              String keys = "";
              if (Emulator.getConfig().getBoolean("categories.cmd_commandsc.show_keys")) {
                for (String key : command.keys) {
                  if (keys.equals("")) {
                    keys = "(" + key;
                  } else {
                    keys = keys + " " + key;
                  } 
                } 
                keys = keys + ")";
              } 
              tempList.add(Emulator.getTexts().getValue("commands.description." + command.permission, "commands.description." + command.permission + " " + keys));
              hasPermission = true;
            } 
          } 
        } 
        if (hasPermission) {
          json_cmd.add(category.getName());
          for (String temp : tempList)
            json_cmd.add(temp); 
        } 
      } 
    } 
    if (duplicateCommands.size() > 0 && Emulator.getConfig().getBoolean("categories.cmd_commandsc.show_others")) {
      json_cmd.add(Emulator.getTexts().getValue("commands.generic.cmd_commandsc.others"));
      for (Command command : duplicateCommands) {
        String keys = "";
        if (Emulator.getConfig().getBoolean("categories.cmd_commandsc.show_keys")) {
          for (String key : command.keys) {
            if (keys.equals("")) {
              keys = "(" + key;
            } else {
              keys = keys + " " + key;
            } 
          } 
          keys = keys + ")";
        } 
        json_cmd.add(Emulator.getTexts().getValue("commands.description." + command.permission, "commands.description." + command.permission) + " " + keys);
      } 
    } 
    this.data.add("commands", (JsonElement)json_cmd);
  }
}
