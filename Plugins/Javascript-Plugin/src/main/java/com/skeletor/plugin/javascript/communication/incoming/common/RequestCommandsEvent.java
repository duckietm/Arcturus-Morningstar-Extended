package com.skeletor.plugin.javascript.communication.incoming.common;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.commands.Command;
import com.eu.habbo.habbohotel.gameclients.GameClient;
import com.eu.habbo.messages.outgoing.MessageComposer;
import com.skeletor.plugin.javascript.communication.incoming.IncomingWebMessage;
import com.skeletor.plugin.javascript.communication.outgoing.OutgoingWebMessage;
import com.skeletor.plugin.javascript.communication.outgoing.common.CommandsComposer;
import com.skeletor.plugin.javascript.override_packets.outgoing.JavascriptCallbackComposer;
import java.util.List;

public class RequestCommandsEvent extends IncomingWebMessage<RequestCommandsEvent.JSONRequestCommandsEvent> {
  public RequestCommandsEvent() {
    super(JSONRequestCommandsEvent.class);
  }
  
  public void handle(GameClient client, JSONRequestCommandsEvent message) {
    List<Command> commands = Emulator.getGameEnvironment().getCommandHandler().getCommandsForRank(client.getHabbo().getHabboInfo().getRank().getId());
    CommandsComposer commandsComposer = new CommandsComposer(commands);
    client.sendResponse((MessageComposer)new JavascriptCallbackComposer((OutgoingWebMessage)commandsComposer));
  }
  
  static class JSONRequestCommandsEvent {
    boolean idk;
  }
}
