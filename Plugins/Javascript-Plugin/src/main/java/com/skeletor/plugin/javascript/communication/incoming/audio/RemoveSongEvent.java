package com.skeletor.plugin.javascript.communication.incoming.audio;

import com.eu.habbo.habbohotel.gameclients.GameClient;
import com.eu.habbo.habbohotel.rooms.Room;
import com.skeletor.plugin.javascript.audio.RoomAudioManager;
import com.skeletor.plugin.javascript.audio.RoomPlaylist;
import com.skeletor.plugin.javascript.communication.incoming.IncomingWebMessage;
import com.skeletor.plugin.javascript.communication.outgoing.OutgoingWebMessage;
import com.skeletor.plugin.javascript.communication.outgoing.audio.RemoveSongComposer;
import com.skeletor.plugin.javascript.override_packets.outgoing.JavascriptCallbackComposer;

public class RemoveSongEvent extends IncomingWebMessage<RemoveSongEvent.JSONRemoveSongEvent> {
  public RemoveSongEvent() {
    super(JSONRemoveSongEvent.class);
  }
  
  public void handle(GameClient client, JSONRemoveSongEvent message) {
    Room room = client.getHabbo().getHabboInfo().getCurrentRoom();
    if (room == null)
      return; 
    if (room.hasRights(client.getHabbo())) {
      RoomPlaylist roomPlaylist = RoomAudioManager.getInstance().getPlaylistForRoom(room.getId());
      roomPlaylist.removeSong(message.index);
      room.sendComposer((new JavascriptCallbackComposer((OutgoingWebMessage)new RemoveSongComposer(message.index))).compose());
    } 
  }
  
  public static class JSONRemoveSongEvent {
    public int index;
  }
}
