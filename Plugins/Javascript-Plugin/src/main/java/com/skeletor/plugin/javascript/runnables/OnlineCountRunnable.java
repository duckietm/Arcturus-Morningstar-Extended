package com.skeletor.plugin.javascript.runnables;

import com.eu.habbo.Emulator;
import com.eu.habbo.messages.outgoing.MessageComposer;
import com.skeletor.plugin.javascript.communication.outgoing.OutgoingWebMessage;
import com.skeletor.plugin.javascript.communication.outgoing.common.OnlineCountComposer;
import com.skeletor.plugin.javascript.override_packets.outgoing.JavascriptCallbackComposer;

public class OnlineCountRunnable implements Runnable {
  private static final OnlineCountRunnable instance = new OnlineCountRunnable();
  
  private volatile boolean running = false;
  
  public void run() {
    if (this.running) {
      int count = Emulator.getGameEnvironment().getHabboManager().getOnlineCount();
      Emulator.getGameServer().getGameClientManager().sendBroadcastResponse((MessageComposer)new JavascriptCallbackComposer((OutgoingWebMessage)new OnlineCountComposer(count)));
      Emulator.getThreading().run(this, 30000L);
    } 
  }
  
  public static OnlineCountRunnable getInstance() {
    return instance;
  }
  
  public void start() {
    if (!this.running) {
      this.running = true;
      Emulator.getThreading().run(this);
    } 
  }
  
  public void stop() {
    this.running = false;
  }
}
