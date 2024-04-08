package com.skeletor.plugin.javascript.communication.outgoing.slotmachine;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import com.skeletor.plugin.javascript.communication.outgoing.OutgoingWebMessage;

public class SpinResultComposer extends OutgoingWebMessage {
  public SpinResultComposer(int result1, int result2, int result3, boolean won, int payout) {
    super("slot_result");
    this.data.add("result1", (JsonElement)new JsonPrimitive(Integer.valueOf(result1)));
    this.data.add("result2", (JsonElement)new JsonPrimitive(Integer.valueOf(result2)));
    this.data.add("result3", (JsonElement)new JsonPrimitive(Integer.valueOf(result3)));
    this.data.add("won", (JsonElement)new JsonPrimitive(Boolean.valueOf(won)));
    this.data.add("payout", (JsonElement)new JsonPrimitive(Integer.valueOf(payout)));
  }
}
