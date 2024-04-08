package com.skeletor.plugin.javascript.communication.outgoing.audio;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import com.skeletor.plugin.javascript.communication.outgoing.OutgoingWebMessage;

public class PlaySongComposer extends OutgoingWebMessage {
  public PlaySongComposer(int index) {
    super("play_song");
    this.data.add("index", (JsonElement)new JsonPrimitive(Integer.valueOf(index)));
  }
}
