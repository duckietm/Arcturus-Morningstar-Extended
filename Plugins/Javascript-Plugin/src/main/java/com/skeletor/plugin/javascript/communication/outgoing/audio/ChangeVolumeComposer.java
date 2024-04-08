package com.skeletor.plugin.javascript.communication.outgoing.audio;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import com.skeletor.plugin.javascript.communication.outgoing.OutgoingWebMessage;

public class ChangeVolumeComposer extends OutgoingWebMessage {
  public ChangeVolumeComposer(int volume) {
    super("change_volume");
    this.data.add("volume", (JsonElement)new JsonPrimitive(Integer.valueOf(volume)));
  }
}
