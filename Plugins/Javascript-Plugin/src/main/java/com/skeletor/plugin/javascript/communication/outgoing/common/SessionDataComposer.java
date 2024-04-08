package com.skeletor.plugin.javascript.communication.outgoing.common;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import com.skeletor.plugin.javascript.communication.outgoing.OutgoingWebMessage;

public class SessionDataComposer extends OutgoingWebMessage {
  public SessionDataComposer(int id, String username, String look, int credits) {
    super("session_data");
    this.data.add("id", (JsonElement)new JsonPrimitive(Integer.valueOf(id)));
    this.data.add("username", (JsonElement)new JsonPrimitive(username));
    this.data.add("look", (JsonElement)new JsonPrimitive(look));
    this.data.add("credits", (JsonElement)new JsonPrimitive(Integer.valueOf(credits)));
  }
}
