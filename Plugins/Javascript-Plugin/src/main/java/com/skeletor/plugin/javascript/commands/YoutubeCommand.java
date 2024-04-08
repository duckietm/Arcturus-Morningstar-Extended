package com.skeletor.plugin.javascript.commands;

import com.eu.habbo.habbohotel.commands.Command;
import com.eu.habbo.habbohotel.gameclients.GameClient;
import com.eu.habbo.habbohotel.rooms.Room;
import com.eu.habbo.habbohotel.rooms.RoomChatMessageBubbles;
import com.skeletor.plugin.javascript.communication.outgoing.OutgoingWebMessage;
import com.skeletor.plugin.javascript.communication.outgoing.common.YoutubeTVComposer;
import com.skeletor.plugin.javascript.override_packets.outgoing.JavascriptCallbackComposer;
import com.skeletor.plugin.javascript.utils.RegexUtility;

public class YoutubeCommand extends Command {
  public YoutubeCommand() {
    super("cmd_youtube", new String[] { "youtube" });
  }
  
  public boolean handle(GameClient gameClient, String[] strings) throws Exception {
    Room room = gameClient.getHabbo().getHabboInfo().getCurrentRoom();
    if (strings.length > 1) {
      String videoId = RegexUtility.getYouTubeId(strings[1]);
      int time = 0;
      if (videoId.isEmpty()) {
        gameClient.getHabbo().whisper("Invalid youtube url", RoomChatMessageBubbles.ALERT);
        return true;
      } 
      if (strings[1].contains("t="))
        try {
          String[] realParams = strings[1].split("\\?");
          if (realParams.length > 1) {
            String[] params = realParams[1].split("&");
            for (String param : params) {
              String[] split = param.split("=");
              if (split.length > 1 && 
                split[0].equals("t"))
                time = Integer.parseInt(split[1].replace("s", "")); 
            } 
          } 
        } catch (Exception exception) {} 
      YoutubeTVComposer youtubeTVComposer = new YoutubeTVComposer(videoId, Integer.valueOf(time));
      room.sendComposer((new JavascriptCallbackComposer((OutgoingWebMessage)youtubeTVComposer)).compose());
      return true;
    } 
    return true;
  }
}
