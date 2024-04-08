package com.skeletor.plugin.javascript;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.commands.Command;
import com.eu.habbo.habbohotel.commands.CommandHandler;
import com.eu.habbo.habbohotel.items.ItemInteraction;
import com.eu.habbo.habbohotel.users.Habbo;
import com.eu.habbo.messages.outgoing.MessageComposer;
import com.eu.habbo.plugin.EventHandler;
import com.eu.habbo.plugin.EventListener;
import com.eu.habbo.plugin.HabboPlugin;
import com.eu.habbo.plugin.events.emulator.EmulatorLoadItemsManagerEvent;
import com.eu.habbo.plugin.events.emulator.EmulatorLoadedEvent;
import com.eu.habbo.plugin.events.rooms.RoomUncachedEvent;
import com.eu.habbo.plugin.events.users.UserCreditsEvent;
import com.eu.habbo.plugin.events.users.UserEnterRoomEvent;
import com.eu.habbo.plugin.events.users.UserExitRoomEvent;
import com.eu.habbo.plugin.events.users.UserLoginEvent;
import com.eu.habbo.plugin.events.users.UserSavedSettingsEvent;
import com.skeletor.plugin.javascript.audio.RoomAudioManager;
import com.skeletor.plugin.javascript.audio.RoomPlaylist;
import com.skeletor.plugin.javascript.categories.CommandManager;
import com.skeletor.plugin.javascript.commands.CmdCommand;
import com.skeletor.plugin.javascript.commands.TwitchCommand;
import com.skeletor.plugin.javascript.commands.UpdateCategoriesCommand;
import com.skeletor.plugin.javascript.commands.YoutubeCommand;
import com.skeletor.plugin.javascript.communication.CommunicationManager;
import com.skeletor.plugin.javascript.communication.outgoing.OutgoingWebMessage;
import com.skeletor.plugin.javascript.communication.outgoing.audio.ChangeVolumeComposer;
import com.skeletor.plugin.javascript.communication.outgoing.audio.DisposePlaylistComposer;
import com.skeletor.plugin.javascript.communication.outgoing.audio.PlaySongComposer;
import com.skeletor.plugin.javascript.communication.outgoing.audio.PlayStopComposer;
import com.skeletor.plugin.javascript.communication.outgoing.audio.PlaylistComposer;
import com.skeletor.plugin.javascript.communication.outgoing.common.OnlineCountComposer;
import com.skeletor.plugin.javascript.communication.outgoing.common.SessionDataComposer;
import com.skeletor.plugin.javascript.communication.outgoing.common.TwitchVideoComposer;
import com.skeletor.plugin.javascript.communication.outgoing.common.UpdateCreditsComposer;
import com.skeletor.plugin.javascript.interactions.InteractionSlotMachine;
import com.skeletor.plugin.javascript.interactions.InteractionYoutubeJukebox;
import com.skeletor.plugin.javascript.override_packets.incoming.JavascriptCallbackEvent;
import com.skeletor.plugin.javascript.override_packets.outgoing.JavascriptCallbackComposer;
import com.skeletor.plugin.javascript.runnables.OnlineCountRunnable;
import com.skeletor.plugin.javascript.scheduler.CheckLoadedScheduler;
import gnu.trove.map.hash.THashMap;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class main extends HabboPlugin implements EventListener {
  public static final int JSCALLBACKEVENTHEADER = 314;
  
  private static CommandManager commandManager;
  
  private static THashMap<Integer, String> twitchRooms;
  
  public static String USER_LOADED_EVENT = "user.loaded.event.key";
  
  public static String STARTED_LOADING_EVENT = "user.loading.event.key";
  
  public void onEnable() throws Exception {
    Emulator.getPluginManager().registerEvents(this, this);
    if (Emulator.isReady && !Emulator.isShuttingDown)
      onEmulatorLoadedEvent(null); 
  }
  
  @EventHandler
  public void onEmulatorLoadedEvent(EmulatorLoadedEvent e) throws Exception {
    Emulator.getGameServer().getPacketManager().registerHandler(Integer.valueOf(314), JavascriptCallbackEvent.class);
    Emulator.getTexts().register("jscommands.keys.cmd_commands", "commands");
    Emulator.getTexts().register("commands.description.cmd_commandsc", ":commandsc");
    Emulator.getTexts().register("categories.cmd_commandsc.keys", "commandsc");
    Emulator.getTexts().register("commands.generic.cmd_commandsc.text", "Your Commands");
    Emulator.getTexts().register("commands.generic.cmd_commandsc.others", "Others");
    Emulator.getTexts().register("commands.description.cmd_update_categories", ":update_categories");
    Emulator.getTexts().register("categories.cmd_update_categories.keys", "update_categories");
    Emulator.getTexts().register("categories.cmd_update_categories.success", "Successfully updated command categories");
    Emulator.getConfig().register("categories.cmd_commandsc.show_keys", "1");
    Emulator.getConfig().register("categories.cmd_commandsc.show_others", "1");
    boolean reloadPermissions = false;
    reloadPermissions = registerPermission("cmd_commandsc", "'0', '1'", "1", reloadPermissions);
    reloadPermissions = registerPermission("cmd_update_categories", "'0', '1'", "0", reloadPermissions);
    if (reloadPermissions)
      Emulator.getGameEnvironment().getPermissionsManager().reload(); 
    CommandHandler.addCommand((Command)new YoutubeCommand());
    CommandHandler.addCommand((Command)new CmdCommand());
    CommandHandler.addCommand((Command)new TwitchCommand());
    CommandHandler.addCommand((Command)new UpdateCategoriesCommand("cmd_update_categories", Emulator.getTexts().getValue("categories.cmd_update_categories.keys").split(";")));
    RoomAudioManager.Init();
    OnlineCountRunnable.getInstance().start();
    commandManager = new CommandManager();
    twitchRooms = new THashMap();
    new CheckLoadedScheduler();
  }
  
  private boolean registerPermission(String name, String options, String defaultValue, boolean defaultReturn) {
    try(Connection connection = Emulator.getDatabase().getDataSource().getConnection(); 
        
        PreparedStatement statement = connection.prepareStatement("ALTER TABLE  `permissions` ADD  `" + name + "` ENUM(  " + options + " ) NOT NULL DEFAULT  '" + defaultValue + "'")) {
      statement.execute();
      return true;
    } catch (SQLException sQLException) {
      return defaultReturn;
    } 
  }
  
  @EventHandler
  public void onLoadItemsManager(EmulatorLoadItemsManagerEvent e) {
    Emulator.getGameEnvironment().getItemManager().addItemInteraction(new ItemInteraction("slots_machine", InteractionSlotMachine.class));
    Emulator.getGameEnvironment().getItemManager().addItemInteraction(new ItemInteraction("yt_jukebox", InteractionYoutubeJukebox.class));
  }
  
  @EventHandler
  public void onUserEnterRoomEvent(UserEnterRoomEvent e) {
    RoomPlaylist playlist = RoomAudioManager.getInstance().getPlaylistForRoom(e.room.getId());
    if (playlist.getPlaylist().size() > 0) {
      e.habbo.getClient().sendResponse((MessageComposer)new JavascriptCallbackComposer((OutgoingWebMessage)new PlaylistComposer(playlist)));
      if (playlist.isPlaying()) {
        e.habbo.getClient().sendResponse((MessageComposer)new JavascriptCallbackComposer((OutgoingWebMessage)new PlaySongComposer(playlist.getCurrentIndex())));
        e.habbo.getClient().sendResponse(playlist.getNowPlayingBubbleAlert());
      } 
    } 
    if (e.room.getHabbos() == null || e.room.getHabbos().size() == 0)
      twitchRooms.remove(Integer.valueOf(e.room.getId())); 
    if (twitchRooms.containsKey(Integer.valueOf(e.room.getId()))) {
      TwitchVideoComposer twitchVideoComposer = new TwitchVideoComposer((String)twitchRooms.get(Integer.valueOf(e.room.getId())));
      e.habbo.getClient().sendResponse((new JavascriptCallbackComposer((OutgoingWebMessage)twitchVideoComposer)).compose());
    } 
  }
  
  @EventHandler
  public void onUserExitRoomEvent(UserExitRoomEvent e) {
    e.habbo.getClient().sendResponse((MessageComposer)new JavascriptCallbackComposer((OutgoingWebMessage)new PlayStopComposer(false)));
    e.habbo.getClient().sendResponse((MessageComposer)new JavascriptCallbackComposer((OutgoingWebMessage)new DisposePlaylistComposer()));
  }
  
  @EventHandler
  public void onRoomUncachedEvent(RoomUncachedEvent e) {
    RoomAudioManager.getInstance().dispose(e.room.getId());
  }
  
  @EventHandler
  public void onUserLoginEvent(UserLoginEvent e) {
    if (e.habbo == null || e.habbo.getClient() == null)
      return; 
    Habbo habbo = e.habbo;
    (e.habbo.getHabboStats()).cache.put(USER_LOADED_EVENT, Boolean.valueOf(false));
    (e.habbo.getHabboStats()).cache.put(STARTED_LOADING_EVENT, Integer.valueOf(Emulator.getIntUnixTimestamp()));
    SessionDataComposer sessionDataComposer = new SessionDataComposer(e.habbo.getHabboInfo().getId(), e.habbo.getHabboInfo().getUsername(), e.habbo.getHabboInfo().getLook(), e.habbo.getHabboInfo().getCredits());
    e.habbo.getClient().sendResponse((MessageComposer)new JavascriptCallbackComposer((OutgoingWebMessage)sessionDataComposer));
    e.habbo.getClient().sendResponse((MessageComposer)new JavascriptCallbackComposer((OutgoingWebMessage)new ChangeVolumeComposer((e.habbo.getHabboStats()).volumeTrax)));
    e.habbo.getClient().sendResponse((MessageComposer)new JavascriptCallbackComposer((OutgoingWebMessage)new OnlineCountComposer(Emulator.getGameEnvironment().getHabboManager().getOnlineCount())));
    Emulator.getThreading().run(() -> userCheckLoaded(habbo), 500L);
  }
  
  public void userCheckLoaded(Habbo habbo) {
    if (habbo == null || habbo.getClient() == null || !habbo.isOnline())
      return; 
    if (((Boolean)(habbo.getHabboStats()).cache.get(USER_LOADED_EVENT)).booleanValue())
      return; 
    if (Emulator.getIntUnixTimestamp() - ((Integer)(habbo.getHabboStats()).cache.get(STARTED_LOADING_EVENT)).intValue() > 15)
      return; 
    SessionDataComposer sessionDataComposer = new SessionDataComposer(habbo.getHabboInfo().getId(), habbo.getHabboInfo().getUsername(), habbo.getHabboInfo().getLook(), habbo.getHabboInfo().getCredits());
    habbo.getClient().sendResponse((MessageComposer)new JavascriptCallbackComposer((OutgoingWebMessage)sessionDataComposer));
    habbo.getClient().sendResponse((MessageComposer)new JavascriptCallbackComposer((OutgoingWebMessage)new ChangeVolumeComposer((habbo.getHabboStats()).volumeTrax)));
    habbo.getClient().sendResponse((MessageComposer)new JavascriptCallbackComposer((OutgoingWebMessage)new OnlineCountComposer(Emulator.getGameEnvironment().getHabboManager().getOnlineCount())));
    Emulator.getThreading().run(() -> userCheckLoaded(habbo), 500L);
  }
  
  @EventHandler
  public void onUserCreditsEvent(UserCreditsEvent e) {
    UpdateCreditsComposer creditsComposer = new UpdateCreditsComposer(e.habbo.getHabboInfo().getCredits());
    e.habbo.getClient().sendResponse((MessageComposer)new JavascriptCallbackComposer((OutgoingWebMessage)creditsComposer));
  }
  
  @EventHandler
  public void onUserSavedSettingsEvent(UserSavedSettingsEvent e) {
    e.habbo.getClient().sendResponse((MessageComposer)new JavascriptCallbackComposer((OutgoingWebMessage)new ChangeVolumeComposer((e.habbo.getHabboStats()).volumeTrax)));
  }
  
  public void onDisable() throws Exception {
    CommunicationManager.getInstance().Dispose();
    RoomAudioManager.getInstance().Dispose();
    OnlineCountRunnable.getInstance().stop();
  }
  
  public static CommandManager getCommandManager() {
    return commandManager;
  }
  
  public static void addTwitchRoom(int roomId, String twitch) {
    twitchRooms.put(Integer.valueOf(roomId), twitch);
  }
  
  public boolean hasPermission(Habbo habbo, String s) {
    return false;
  }
}
