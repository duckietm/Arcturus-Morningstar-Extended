package com.skeletor.plugin.javascript.communication.outgoing.common;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import com.skeletor.plugin.javascript.communication.outgoing.OutgoingWebMessage;

public class TwitchVideoComposer extends OutgoingWebMessage {
  public TwitchVideoComposer(String videoId) {
    super("twitch");
    this.data.add("videoId", (JsonElement)new JsonPrimitive(videoId));
  }
}
