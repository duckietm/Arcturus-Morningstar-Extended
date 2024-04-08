package com.skeletor.plugin.javascript.audio;

import java.util.concurrent.ConcurrentHashMap;

public class RoomAudioManager {
  private static RoomAudioManager _instance;
  
  private ConcurrentHashMap<Integer, RoomPlaylist> roomAudio = new ConcurrentHashMap<>();
  
  public RoomPlaylist getPlaylistForRoom(int roomId) {
    if (this.roomAudio.containsKey(Integer.valueOf(roomId)))
      return this.roomAudio.get(Integer.valueOf(roomId)); 
    RoomPlaylist newPlaylist = new RoomPlaylist();
    this.roomAudio.put(Integer.valueOf(roomId), newPlaylist);
    return newPlaylist;
  }
  
  public void dispose(int roomId) {
    this.roomAudio.remove(Integer.valueOf(roomId));
  }
  
  public static void Init() {
    _instance = new RoomAudioManager();
  }
  
  public void Dispose() {
    this.roomAudio.clear();
    _instance = null;
  }
  
  public static RoomAudioManager getInstance() {
    if (_instance == null)
      _instance = new RoomAudioManager(); 
    return _instance;
  }
}
