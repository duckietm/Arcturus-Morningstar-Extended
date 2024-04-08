package com.skeletor.plugin.javascript.communication.outgoing.audio;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import com.skeletor.plugin.javascript.communication.outgoing.OutgoingWebMessage;

public class PlayStopComposer extends OutgoingWebMessage {
  public PlayStopComposer(boolean isPlaying) {
    super("play_stop");
    this.data.add("playing", (JsonElement)new JsonPrimitive(Boolean.valueOf(isPlaying)));
  }
}
