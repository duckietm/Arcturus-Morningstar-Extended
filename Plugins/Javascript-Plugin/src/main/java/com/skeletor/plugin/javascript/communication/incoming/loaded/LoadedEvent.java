package com.skeletor.plugin.javascript.communication.incoming.loaded;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.commands.Command;
import com.eu.habbo.habbohotel.gameclients.GameClient;
import com.eu.habbo.habbohotel.users.Habbo;
import com.eu.habbo.messages.outgoing.MessageComposer;
import com.skeletor.plugin.javascript.communication.incoming.IncomingWebMessage;
import com.skeletor.plugin.javascript.communication.outgoing.OutgoingWebMessage;
import com.skeletor.plugin.javascript.communication.outgoing.commands.CommandsPopUpComposer;
import com.skeletor.plugin.javascript.main;
import com.skeletor.plugin.javascript.override_packets.outgoing.JavascriptCallbackComposer;
import java.util.List;

public class LoadedEvent extends IncomingWebMessage<LoadedEvent.JSONLoadedEvent> {
  public LoadedEvent() {
    super(JSONLoadedEvent.class);
  }
  
  public void handle(GameClient client, JSONLoadedEvent message) {
    Habbo habbo = client.getHabbo();
    if (habbo == null)
      return; 
    (habbo.getHabboStats()).cache.put(main.USER_LOADED_EVENT, Boolean.valueOf(true));
    List<Command> commands = Emulator.getGameEnvironment().getCommandHandler().getCommandsForRank(client.getHabbo().getHabboInfo().getRank().getId());
    CommandsPopUpComposer commandsPopUpComposer = new CommandsPopUpComposer(commands, (habbo.getHabboInfo().getRank().getLevel() >= 5));
    client.sendResponse((MessageComposer)new JavascriptCallbackComposer((OutgoingWebMessage)commandsPopUpComposer));
  }
  
  static class JSONLoadedEvent {
    boolean idk;
  }
}
