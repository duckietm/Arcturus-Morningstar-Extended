package com.skeletor.plugin.javascript.audio;

import com.eu.habbo.messages.outgoing.MessageComposer;
import com.eu.habbo.messages.outgoing.generic.alerts.BubbleAlertComposer;
import gnu.trove.map.hash.THashMap;
import java.util.ArrayList;

public class RoomPlaylist {
  private ArrayList<YoutubeVideo> playlist = new ArrayList<>();
  
  private int current = 0;
  
  private boolean playing = false;
  
  public YoutubeVideo nextSong() {
    if (this.current < this.playlist.size() - 1) {
      this.current++;
    } else {
      this.current = 0;
    } 
    return this.playlist.get(this.current);
  }
  
  public YoutubeVideo prevSong() {
    if (this.current > 0)
      this.current--; 
    return this.playlist.get(this.current);
  }
  
  public boolean isPlaying() {
    return this.playing;
  }
  
  public void setPlaying(boolean playing) {
    this.playing = playing;
  }
  
  public void addSong(YoutubeVideo song) {
    this.playlist.add(song);
  }
  
  public YoutubeVideo removeSong(int index) {
    YoutubeVideo res = null;
    if (this.playlist.size() - 1 >= index)
      res = this.playlist.remove(index); 
    if (this.playlist.size() == 0)
      setPlaying(false); 
    if (index == getCurrentIndex()) {
      if (index > this.playlist.size() - 1 && this.playlist.size() > 0)
        this.current = this.playlist.size() - 1; 
    } else if (index < getCurrentIndex() && getCurrentIndex() > 0) {
      this.current--;
    } 
    return res;
  }
  
  public YoutubeVideo getCurrentSong() {
    return this.playlist.get(this.current);
  }
  
  public int getCurrentIndex() {
    return this.current;
  }
  
  public ArrayList<YoutubeVideo> getPlaylist() {
    return this.playlist;
  }
  
  public static class YoutubeVideo {
    public String name;
    
    public String videoId;
    
    public String channel;
    
    public YoutubeVideo(String name, String videoId, String channel) {
      this.name = name;
      this.videoId = videoId;
      this.channel = channel;
    }
  }
  
  public MessageComposer getNowPlayingBubbleAlert() {
    THashMap<String, String> keys = new THashMap();
    keys.put("display", "BUBBLE");
    keys.put("image", "${image.library.url}notifications/music.png");
    keys.put("message", "Now playing " + (getCurrentSong()).name);
    return (MessageComposer)new BubbleAlertComposer("", keys);
  }
}
