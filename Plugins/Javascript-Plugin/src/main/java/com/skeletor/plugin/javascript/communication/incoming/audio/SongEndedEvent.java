package com.skeletor.plugin.javascript.communication.incoming.audio;

import com.eu.habbo.habbohotel.gameclients.GameClient;
import com.eu.habbo.habbohotel.rooms.Room;
import com.skeletor.plugin.javascript.audio.RoomAudioManager;
import com.skeletor.plugin.javascript.audio.RoomPlaylist;
import com.skeletor.plugin.javascript.communication.incoming.IncomingWebMessage;
import com.skeletor.plugin.javascript.communication.outgoing.OutgoingWebMessage;
import com.skeletor.plugin.javascript.communication.outgoing.audio.PlaySongComposer;
import com.skeletor.plugin.javascript.override_packets.outgoing.JavascriptCallbackComposer;

public class SongEndedEvent extends IncomingWebMessage<SongEndedEvent.JSONSongEndedEvent> {
  public SongEndedEvent() {
    super(JSONSongEndedEvent.class);
  }
  
  public void handle(GameClient client, JSONSongEndedEvent message) {
    Room room = client.getHabbo().getHabboInfo().getCurrentRoom();
    if (room == null)
      return; 
    if (room.hasRights(client.getHabbo())) {
      RoomPlaylist playlist = RoomAudioManager.getInstance().getPlaylistForRoom(room.getId());
      if (playlist.getCurrentIndex() == message.currentIndex) {
        playlist.nextSong();
        playlist.setPlaying(true);
        PlaySongComposer playSongComposer = new PlaySongComposer(playlist.getCurrentIndex());
        room.sendComposer((new JavascriptCallbackComposer((OutgoingWebMessage)playSongComposer)).compose());
        room.sendComposer(playlist.getNowPlayingBubbleAlert().compose());
      } 
    } 
  }
  
  class JSONSongEndedEvent {
    public int currentIndex;
  }
}
