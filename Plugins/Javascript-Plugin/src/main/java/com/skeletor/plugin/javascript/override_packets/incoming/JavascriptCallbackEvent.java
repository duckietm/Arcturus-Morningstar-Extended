package com.skeletor.plugin.javascript.override_packets.incoming;

import com.eu.habbo.messages.incoming.MessageHandler;
import com.skeletor.plugin.javascript.communication.CommunicationManager;

public class JavascriptCallbackEvent extends MessageHandler {
  public void handle() {
    String payload = this.packet.readString();
    CommunicationManager.getInstance().OnMessage(payload, this.client);
  }
}
