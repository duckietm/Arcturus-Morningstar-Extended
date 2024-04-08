package com.skeletor.plugin.javascript.communication.incoming;

import com.eu.habbo.habbohotel.gameclients.GameClient;
import com.google.gson.JsonObject;

public abstract class IncomingWebMessage<T> {
  public final Class<T> type;
  
  public IncomingWebMessage(Class<T> type) {
    this.type = type;
  }
  
  public abstract void handle(GameClient paramGameClient, T paramT);
  
  public static class JSONIncomingEvent {
    public String header;
    
    public JsonObject data;
  }
}
