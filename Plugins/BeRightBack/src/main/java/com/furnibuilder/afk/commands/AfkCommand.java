package com.furnibuilder.afk.commands;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.commands.Command;
import com.eu.habbo.habbohotel.gameclients.GameClient;
import com.eu.habbo.habbohotel.rooms.RoomChatMessage;
import com.eu.habbo.habbohotel.rooms.RoomChatMessageBubbles;
import com.eu.habbo.habbohotel.users.Habbo;
import com.eu.habbo.messages.outgoing.rooms.users.RoomUserTalkComposer;
import com.eu.habbo.plugin.EventHandler;
import com.eu.habbo.plugin.EventListener;
import com.eu.habbo.plugin.HabboPlugin;
import com.eu.habbo.plugin.events.users.UserExitRoomEvent;
import com.eu.habbo.plugin.events.users.UserIdleEvent;
import com.furnibuilder.afk.Afk;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AfkCommand extends Command implements EventListener {
  public static HashMap<Integer, Integer> userAFKMap = new HashMap<>();
  
  public static HashMap<Integer, Integer> requiresUpdate = new HashMap<>();
  
  public static boolean started = false;
  
  public AfkCommand(String permission, String[] keys) {
    super(permission, keys);
    Emulator.getPluginManager().registerEvents((HabboPlugin)Afk.INSTANCE, this);
  }
  
  public boolean handle(GameClient gameClient, String[] strings) throws Exception {
    StringBuilder reason = new StringBuilder();
    if (strings.length > 1)
      for (int i = 1; i < strings.length; i++) {
        reason.append(strings[i]);
        reason.append(" ");
      }  
    String result = Emulator.getGameEnvironment().getWordFilter().filter(reason.toString(), gameClient.getHabbo());
    (gameClient.getHabbo().getHabboStats()).cache.put(Afk.AFK_KEY, result);
    if (!gameClient.getHabbo().getRoomUnit().isIdle())
      afk(gameClient.getHabbo(), strings[0]); 
    return true;
  }
  
  private static void afk(Habbo habbo) {
    afk(habbo, "afk");
  }
  
  private static void afk(Habbo habbo, String afk) {
    habbo.talk(Emulator.getTexts().getValue("afk.cmd_afk.brb").replace("%username%", habbo.getHabboInfo().getUsername()));
    habbo.getHabboInfo().getCurrentRoom().idle(habbo);
    if (afk.equalsIgnoreCase("afk")) {
      habbo.getHabboInfo().getCurrentRoom().giveEffect(habbo, Emulator.getConfig().getInt("afk.effect_id"), -1);
    } else if (afk.equalsIgnoreCase("brb")) {
      habbo.getHabboInfo().getCurrentRoom().giveEffect(habbo, Emulator.getConfig().getInt("brb.effect_id"), -1);
    } 
    userAFKMap.put(Integer.valueOf(habbo.getHabboInfo().getId()), Integer.valueOf(Emulator.getIntUnixTimestamp()));
    requiresUpdate.put(Integer.valueOf(habbo.getHabboInfo().getId()), Integer.valueOf(Emulator.getIntUnixTimestamp() + 300));
  }
  
  private static void back(Habbo habbo) {
    if (userAFKMap.containsKey(Integer.valueOf(habbo.getHabboInfo().getId()))) {
      habbo.talk(Emulator.getTexts().getValue("afk.cmd_afk.back").replace("%username%", habbo.getHabboInfo().getUsername()));
      habbo.getHabboInfo().getCurrentRoom().unIdle(habbo);
      habbo.getHabboInfo().getCurrentRoom().giveEffect(habbo, 0, -1);
      userAFKMap.remove(Integer.valueOf(habbo.getHabboInfo().getId()));
      requiresUpdate.remove(Integer.valueOf(habbo.getHabboInfo().getId()));
    } 
  }
  
  @EventHandler
  public static void onUserExitRoomEvent(UserExitRoomEvent event) {
    userAFKMap.remove(Integer.valueOf(event.habbo.getHabboInfo().getId()));
    requiresUpdate.remove(Integer.valueOf(event.habbo.getHabboInfo().getId()));
  }
  
  @EventHandler
  public static void onUserIdleEvent(UserIdleEvent event) {
    if (event.reason == UserIdleEvent.IdleReason.TIMEOUT) {
      afk(event.habbo);
    } else if (!event.idle) {
      back(event.habbo);
    } 
  }
  
  public static void startBackgroundThread() {
    if (started)
      return; 
    started = true;
    Emulator.getThreading().run(new Runnable() {
          public void run() {
            List<Integer> toRemove = new ArrayList<>();
            Emulator.getThreading().run(this, 2000L);
            int timeStamp = Emulator.getIntUnixTimestamp();
            for (Map.Entry<Integer, Integer> entry : AfkCommand.requiresUpdate.entrySet()) {
              if (((Integer)entry.getValue()).intValue() < timeStamp) {
                Habbo habbo = Emulator.getGameEnvironment().getHabboManager().getHabbo(((Integer)entry.getKey()).intValue());
                if (habbo != null)
                  if (habbo.getRoomUnit().isIdle()) {
                    habbo.getHabboInfo().getCurrentRoom().sendComposer((new RoomUserTalkComposer(new RoomChatMessage(
                            
                            Emulator.getTexts().getValue("afk.cmd_afk.time")
                            .replace("%username%", habbo.getHabboInfo().getUsername())
                            .replace("%time%", (int)Math.floor(((timeStamp - ((Integer)AfkCommand.userAFKMap.get(Integer.valueOf(habbo.getHabboInfo().getId()))).intValue()) / 60)) + "")
                            .replace("%reason%", (String)(habbo.getHabboStats()).cache.get(Afk.AFK_KEY)), habbo
                            .getRoomUnit(), RoomChatMessageBubbles.ALERT))).compose());
                    AfkCommand.requiresUpdate.put(entry.getKey(), Integer.valueOf(timeStamp + 300));
                    continue;
                  }  
                toRemove.add(entry.getKey());
              } 
            } 
            for (Integer i : toRemove) {
              AfkCommand.userAFKMap.remove(i);
              AfkCommand.requiresUpdate.remove(i);
            } 
          }
        });
  }
}
