package com.eu.habbo.plugin.events.users;

import com.eu.habbo.habbohotel.users.Habbo;

public class UserPurchasePictureEvent extends UserEvent {

    public String url;


    public int roomId;


    public int timestamp;


    public UserPurchasePictureEvent(Habbo habbo, String url, int roomId, int timestamp) {
        super(habbo);

        this.url = url;
        this.roomId = roomId;
        this.timestamp = timestamp;
    }
}