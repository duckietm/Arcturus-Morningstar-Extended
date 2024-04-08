package com.skeletor.plugin.javascript.scheduler;

import com.eu.habbo.Emulator;
import com.eu.habbo.core.Scheduler;
import com.eu.habbo.habbohotel.users.Habbo;
import com.eu.habbo.messages.outgoing.MessageComposer;
import com.skeletor.plugin.javascript.communication.outgoing.OutgoingWebMessage;
import com.skeletor.plugin.javascript.communication.outgoing.common.SessionDataComposer;
import com.skeletor.plugin.javascript.main;
import com.skeletor.plugin.javascript.override_packets.outgoing.JavascriptCallbackComposer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CheckLoadedScheduler extends Scheduler {
  private static final Logger LOGGER = LoggerFactory.getLogger(CheckLoadedScheduler.class);
  
  public CheckLoadedScheduler() {
    super(60);
    run();
  }
  
  public void run() {
    super.run();
    if (Emulator.getConfig().getBoolean("pop.up.enabled", true))
      for (Habbo habbo : Emulator.getGameEnvironment().getHabboManager().getOnlineHabbos().values()) {
        if (!(habbo.getHabboStats()).cache.containsKey(main.USER_LOADED_EVENT))
          continue; 
        if (((Boolean)(habbo.getHabboStats()).cache.get(main.USER_LOADED_EVENT)).booleanValue())
          continue; 
        SessionDataComposer sessionDataComposer = new SessionDataComposer(habbo.getHabboInfo().getId(), habbo.getHabboInfo().getUsername(), habbo.getHabboInfo().getLook(), habbo.getHabboInfo().getCredits());
        habbo.getClient().sendResponse((MessageComposer)new JavascriptCallbackComposer((OutgoingWebMessage)sessionDataComposer));
      }  
  }
}
