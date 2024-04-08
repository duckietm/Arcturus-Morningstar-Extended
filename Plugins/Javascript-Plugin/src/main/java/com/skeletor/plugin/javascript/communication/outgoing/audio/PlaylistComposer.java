package com.skeletor.plugin.javascript.communication.outgoing.audio;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.skeletor.plugin.javascript.audio.RoomPlaylist;
import com.skeletor.plugin.javascript.communication.outgoing.OutgoingWebMessage;

public class PlaylistComposer extends OutgoingWebMessage {
  public PlaylistComposer(RoomPlaylist playlist) {
    super("playlist");
    JsonArray playlistJson = new JsonArray();
    for (RoomPlaylist.YoutubeVideo video : playlist.getPlaylist()) {
      JsonObject song = new JsonObject();
      song.add("name", (JsonElement)new JsonPrimitive(video.name));
      song.add("videoId", (JsonElement)new JsonPrimitive(video.videoId));
      song.add("channel", (JsonElement)new JsonPrimitive(video.channel));
      playlistJson.add((JsonElement)song);
    } 
    this.data.add("playlist", (JsonElement)playlistJson);
  }
}
