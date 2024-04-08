package com.skeletor.plugin.javascript.communication.outgoing.common;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import com.skeletor.plugin.javascript.communication.outgoing.OutgoingWebMessage;

public class YoutubeTVComposer extends OutgoingWebMessage {
  public YoutubeTVComposer(String videoId, Integer time) {
    super("youtube_tv");
    this.data.add("videoId", (JsonElement)new JsonPrimitive(videoId));
    this.data.add("time", (JsonElement)new JsonPrimitive(time));
    this.data.add("itemId", (JsonElement)new JsonPrimitive(Integer.valueOf(0)));
  }
}
