package com.eu.habbo.plugin.events.users;

import com.eu.habbo.habbohotel.users.Habbo;
import com.eu.habbo.habbohotel.users.inventory.WardrobeComponent;

public class UserSavedWardrobeEvent extends UserEvent {
    public final WardrobeComponent.WardrobeItem wardrobeItem;


    public UserSavedWardrobeEvent(Habbo habbo, WardrobeComponent.WardrobeItem wardrobeItem) {
        super(habbo);
        this.wardrobeItem = wardrobeItem;
    }
}
