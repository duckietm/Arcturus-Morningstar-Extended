package com.skeletor.plugin.javascript.commands;

import com.eu.habbo.habbohotel.commands.Command;
import com.eu.habbo.habbohotel.gameclients.GameClient;
import com.eu.habbo.habbohotel.rooms.Room;
import com.skeletor.plugin.javascript.communication.outgoing.OutgoingWebMessage;
import com.skeletor.plugin.javascript.communication.outgoing.common.TwitchVideoComposer;
import com.skeletor.plugin.javascript.main;
import com.skeletor.plugin.javascript.override_packets.outgoing.JavascriptCallbackComposer;

public class TwitchCommand extends Command {
  public TwitchCommand() {
    super("cmd_twitch", new String[] { "twitch" });
  }
  
  public boolean handle(GameClient gameClient, String[] strings) throws Exception {
    Room room = gameClient.getHabbo().getHabboInfo().getCurrentRoom();
    if ((room.hasRights(gameClient.getHabbo()) || gameClient.getHabbo().getHabboInfo().getRank().getName().equals("VIP")) && 
      strings.length > 1) {
      String videoId = strings[1];
      if (videoId.isEmpty()) {
        gameClient.getHabbo().whisper("You must supply the twitch channel/video");
        return true;
      } 
      main.addTwitchRoom(room.getId(), videoId);
      TwitchVideoComposer twitchVideoComposer = new TwitchVideoComposer(videoId);
      room.sendComposer((new JavascriptCallbackComposer((OutgoingWebMessage)twitchVideoComposer)).compose());
      return true;
    } 
    gameClient.getHabbo().whisper("You do not have permission to use this command in this room");
    return true;
  }
}
