package com.skeletor.plugin.javascript.interactions;

import com.eu.habbo.habbohotel.gameclients.GameClient;
import com.eu.habbo.habbohotel.items.Item;
import com.eu.habbo.habbohotel.items.interactions.InteractionDefault;
import com.eu.habbo.habbohotel.rooms.Room;
import com.eu.habbo.habbohotel.rooms.RoomUnit;
import com.eu.habbo.messages.outgoing.MessageComposer;
import com.skeletor.plugin.javascript.communication.outgoing.OutgoingWebMessage;
import com.skeletor.plugin.javascript.communication.outgoing.slotmachine.SlotMachineComposer;
import com.skeletor.plugin.javascript.override_packets.outgoing.JavascriptCallbackComposer;
import java.sql.ResultSet;
import java.sql.SQLException;

public class InteractionSlotMachine extends InteractionDefault {
  public InteractionSlotMachine(ResultSet set, Item baseItem) throws SQLException {
    super(set, baseItem);
  }
  
  public InteractionSlotMachine(int id, int userId, Item item, String extradata, int limitedStack, int limitedSells) {
    super(id, userId, item, extradata, limitedStack, limitedSells);
  }
  
  public boolean canWalkOn(RoomUnit roomUnit, Room room, Object[] objects) {
    return false;
  }
  
  public boolean isWalkable() {
    return false;
  }
  
  public void onWalk(RoomUnit roomUnit, Room room, Object[] objects) throws Exception {}
  
  public void onClick(GameClient client, Room room, Object[] objects) throws Exception {
    super.onClick(client, room, objects);
    SlotMachineComposer webComposer = new SlotMachineComposer(getId(), client.getHabbo().getHabboInfo().getCredits());
    client.sendResponse((MessageComposer)new JavascriptCallbackComposer((OutgoingWebMessage)webComposer));
  }
}
