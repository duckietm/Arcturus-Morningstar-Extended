package com.skeletor.plugin.javascript.communication.outgoing.audio;

import com.skeletor.plugin.javascript.communication.outgoing.OutgoingWebMessage;

public class DisposePlaylistComposer extends OutgoingWebMessage {
  public DisposePlaylistComposer() {
    super("dispose_playlist");
  }
}
