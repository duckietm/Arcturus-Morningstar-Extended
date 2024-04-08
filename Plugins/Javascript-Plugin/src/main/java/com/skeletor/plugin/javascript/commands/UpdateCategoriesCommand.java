package com.skeletor.plugin.javascript.commands;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.commands.Command;
import com.eu.habbo.habbohotel.gameclients.GameClient;
import com.eu.habbo.plugin.EventListener;
import com.skeletor.plugin.javascript.main;

public class UpdateCategoriesCommand extends Command implements EventListener {
  public UpdateCategoriesCommand(String permission, String[] keys) {
    super(permission, keys);
  }
  
  public boolean handle(GameClient gameClient, String[] strings) throws Exception {
    main.getCommandManager().reload();
    gameClient.getHabbo().whisper(Emulator.getTexts().getValue("categories.cmd_update_categories.success"));
    return true;
  }
}
