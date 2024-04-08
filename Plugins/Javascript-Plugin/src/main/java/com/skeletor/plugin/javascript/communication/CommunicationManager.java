package com.skeletor.plugin.javascript.communication;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.gameclients.GameClient;
import com.skeletor.plugin.javascript.communication.incoming.IncomingWebMessage;
import com.skeletor.plugin.javascript.communication.incoming.audio.AddSongEvent;
import com.skeletor.plugin.javascript.communication.incoming.audio.NextSongEvent;
import com.skeletor.plugin.javascript.communication.incoming.audio.PlayStopEvent;
import com.skeletor.plugin.javascript.communication.incoming.audio.PreviousSongEvent;
import com.skeletor.plugin.javascript.communication.incoming.audio.RemoveSongEvent;
import com.skeletor.plugin.javascript.communication.incoming.audio.SongEndedEvent;
import com.skeletor.plugin.javascript.communication.incoming.common.MoveAvatarEvent;
import com.skeletor.plugin.javascript.communication.incoming.common.RequestCommandsEvent;
import com.skeletor.plugin.javascript.communication.incoming.common.RequestCreditsEvent;
import com.skeletor.plugin.javascript.communication.incoming.common.RequestSpinSlotMachineEvent;
import com.skeletor.plugin.javascript.communication.incoming.loaded.LoadedEvent;
import com.skeletor.plugin.javascript.utils.JsonFactory;
import gnu.trove.map.hash.THashMap;

public class CommunicationManager {
  private static CommunicationManager instance;
  
  private final THashMap<String, Class<? extends IncomingWebMessage>> _incomingMessages;
  
  static {
    try {
      instance = new CommunicationManager();
    } catch (Exception e) {
      e.printStackTrace();
    } 
  }
  
  public CommunicationManager() {
    this._incomingMessages = new THashMap();
    initializeMessages();
  }
  
  public void initializeMessages() {
    registerMessage("move_avatar", (Class)MoveAvatarEvent.class);
    registerMessage("request_credits", (Class)RequestCreditsEvent.class);
    registerMessage("spin_slot_machine", (Class)RequestSpinSlotMachineEvent.class);
    registerMessage("add_song", (Class)AddSongEvent.class);
    registerMessage("next_song", (Class)NextSongEvent.class);
    registerMessage("prev_song", (Class)PreviousSongEvent.class);
    registerMessage("play_stop", (Class)PlayStopEvent.class);
    registerMessage("remove_song", (Class)RemoveSongEvent.class);
    registerMessage("song_ended", (Class)SongEndedEvent.class);
    registerMessage("request_commands", (Class)RequestCommandsEvent.class);
    registerMessage("js_loaded", (Class)LoadedEvent.class);
  }
  
  public void registerMessage(String key, Class<? extends IncomingWebMessage> message) {
    this._incomingMessages.put(key, message);
  }
  
  public THashMap<String, Class<? extends IncomingWebMessage>> getIncomingMessages() {
    return this._incomingMessages;
  }
  
  public static CommunicationManager getInstance() {
    if (instance == null)
      try {
        instance = new CommunicationManager();
      } catch (Exception e) {
        Emulator.getLogging().logErrorLine(e.getMessage());
      }  
    return instance;
  }
  
  public void OnMessage(String jsonPayload, GameClient sender) {
    try {
      IncomingWebMessage.JSONIncomingEvent heading = (IncomingWebMessage.JSONIncomingEvent)JsonFactory.getInstance().fromJson(jsonPayload, IncomingWebMessage.JSONIncomingEvent.class);
      Class<? extends IncomingWebMessage> message = (Class<? extends IncomingWebMessage>)getInstance().getIncomingMessages().get(heading.header);
      IncomingWebMessage webEvent = message.getDeclaredConstructor(new Class[0]).newInstance(new Object[0]);
      webEvent.handle(sender, JsonFactory.getInstance().fromJson(heading.data.toString(), webEvent.type));
    } catch (Exception e) {
      Emulator.getLogging().logUndefinedPacketLine("unknown message: " + jsonPayload);
    } 
  }
  
  public void Dispose() {
    this._incomingMessages.clear();
    instance = null;
  }
}
