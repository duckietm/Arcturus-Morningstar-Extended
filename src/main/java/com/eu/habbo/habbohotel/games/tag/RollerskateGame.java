package com.eu.habbo.habbohotel.games.tag;

import com.eu.habbo.habbohotel.games.GameTeam;
import com.eu.habbo.habbohotel.items.interactions.games.tag.InteractionTagPole;
import com.eu.habbo.habbohotel.rooms.Room;

public class RollerskateGame extends TagGame {
    public RollerskateGame(Room room) {
        super(GameTeam.class, TagGamePlayer.class, room);
    }

    @Override
    public Class<? extends InteractionTagPole> getTagPole() {
        return null;
    }

    @Override
    public int getMaleEffect() {
        return 55;
    }

    @Override
    public int getMaleTaggerEffect() {
        return 57;
    }

    @Override
    public int getFemaleEffect() {
        return 56;
    }

    @Override
    public int getFemaleTaggerEffect() {
        return 58;
    }
}