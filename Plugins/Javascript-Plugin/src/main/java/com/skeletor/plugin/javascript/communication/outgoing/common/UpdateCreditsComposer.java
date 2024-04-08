package com.skeletor.plugin.javascript.communication.outgoing.common;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import com.skeletor.plugin.javascript.communication.outgoing.OutgoingWebMessage;

public class UpdateCreditsComposer extends OutgoingWebMessage {
  public UpdateCreditsComposer(int credits) {
    super("update_credits");
    this.data.add("credits", (JsonElement)new JsonPrimitive(Integer.valueOf(credits)));
  }
}
