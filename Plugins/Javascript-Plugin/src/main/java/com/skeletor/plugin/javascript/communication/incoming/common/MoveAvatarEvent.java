package com.skeletor.plugin.javascript.communication.incoming.common;

import com.eu.habbo.habbohotel.gameclients.GameClient;
import com.eu.habbo.habbohotel.rooms.Room;
import com.eu.habbo.habbohotel.rooms.RoomTile;
import com.skeletor.plugin.javascript.communication.incoming.IncomingWebMessage;

public class MoveAvatarEvent extends IncomingWebMessage<MoveAvatarEvent.JSONMoveAvatarEvent> {
  private static final short DEFAULT_WALK_AMOUNT = 1;
  
  public MoveAvatarEvent() {
    super(JSONMoveAvatarEvent.class);
  }
  
  public void handle(GameClient client, JSONMoveAvatarEvent message) {
    Room room = client.getHabbo().getRoomUnit().getRoom();
    if (room == null)
      return; 
    short x = (client.getHabbo().getRoomUnit().getGoal()).x;
    short y = (client.getHabbo().getRoomUnit().getGoal()).y;
    switch (message.direction) {
      case "stop":
        return;
      case "left":
        y = (short)(y + 1);
        break;
      case "right":
        y = (short)(y - 1);
        break;
      case "up":
        x = (short)(x - 1);
        break;
      case "down":
        x = (short)(x + 1);
        break;
      default:
        return;
    } 
    try {
      RoomTile goal = room.getLayout().getTile(x, y);
      if (goal == null)
        return; 
      if (goal.isWalkable() || client.getHabbo().getHabboInfo().getCurrentRoom().canSitOrLayAt(goal.x, goal.y)) {
        if (client.getHabbo().getRoomUnit().getMoveBlockingTask() != null)
          client.getHabbo().getRoomUnit().getMoveBlockingTask().get(); 
        client.getHabbo().getRoomUnit().setGoalLocation(goal);
      } 
    } catch (Exception exception) {}
  }
  
  static class JSONMoveAvatarEvent {
    String direction;
  }
}
