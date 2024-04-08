package com.skeletor.plugin.javascript.override_packets.outgoing;

import com.eu.habbo.messages.ServerMessage;
import com.eu.habbo.messages.outgoing.MessageComposer;
import com.skeletor.plugin.javascript.communication.outgoing.OutgoingWebMessage;
import com.skeletor.plugin.javascript.utils.JsonFactory;

public class JavascriptCallbackComposer extends MessageComposer {
  private OutgoingWebMessage webMessage;
  
  public JavascriptCallbackComposer(OutgoingWebMessage message) {
    this.webMessage = message;
  }
  
  public ServerMessage composeInternal() {
    this.response.init(2023);
    String jsonMessage = JsonFactory.getInstance().toJson(this.webMessage).replace("/", "&#47;");
    this.response.appendString("habblet/open/" + jsonMessage);
    return this.response;
  }
}
