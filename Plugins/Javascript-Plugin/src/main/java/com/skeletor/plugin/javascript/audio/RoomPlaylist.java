package com.skeletor.plugin.javascript.audio;

import com.eu.habbo.messages.outgoing.MessageComposer;
import com.eu.habbo.messages.outgoing.generic.alerts.BubbleAlertComposer;
import gnu.trove.map.hash.THashMap;
import java.util.ArrayList;

import static com.skeletor.plugin.javascript.utils.RegexUtility.sanitize;

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
    if(playlist.size() - 1 >= index)
      res = this.playlist.remove(index);
    if(playlist.isEmpty()) this.setPlaying(false);
    if(index == this.getCurrentIndex()) {
      if(index > this.playlist.size() - 1 && !this.playlist.isEmpty()) {
        this.current = this.playlist.size() - 1;
      }
    }
    else if(index < this.getCurrentIndex() && this.getCurrentIndex() > 0) {
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
    final THashMap<String, String> keys = new THashMap<>();
    keys.put("display", "BUBBLE");
    keys.put("image", ("${image.library.url}notifications/music.png"));
    keys.put("message", "Now playing " + sanitize(this.getCurrentSong().name));
    return new BubbleAlertComposer("", keys);
  }
}
