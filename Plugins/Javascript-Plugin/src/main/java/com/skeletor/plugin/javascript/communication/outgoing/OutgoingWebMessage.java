package com.skeletor.plugin.javascript.communication.outgoing;

import com.google.gson.JsonObject;

public abstract class OutgoingWebMessage {
  public String header;
  
  public JsonObject data;
  
  public OutgoingWebMessage(String name) {
    this.header = name;
    this.data = new JsonObject();
  }
}
