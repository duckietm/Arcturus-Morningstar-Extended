package com.skeletor.plugin.javascript.communication.outgoing.common;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import com.skeletor.plugin.javascript.communication.outgoing.OutgoingWebMessage;

public class OnlineCountComposer extends OutgoingWebMessage {
  public OnlineCountComposer(int count) {
    super("online_count");
    this.data.add("count", (JsonElement)new JsonPrimitive(Integer.valueOf(count)));
  }
}
