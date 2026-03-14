package com.eu.habbo.messages.incoming.rooms.users;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.pets.PetTasks;
import com.eu.habbo.habbohotel.rooms.Room;
import com.eu.habbo.habbohotel.rooms.RoomTile;
import com.eu.habbo.habbohotel.rooms.RoomUnit;
import com.eu.habbo.habbohotel.rooms.RoomUnitStatus;
import com.eu.habbo.habbohotel.users.Habbo;
import com.eu.habbo.habbohotel.users.HabboInfo;
import com.eu.habbo.habbohotel.users.HabboItem;
import com.eu.habbo.messages.incoming.MessageHandler;
import com.eu.habbo.messages.outgoing.rooms.users.RoomUnitOnRollerComposer;
import com.eu.habbo.plugin.events.users.UserIdleEvent;
import gnu.trove.set.hash.THashSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RoomUserWalkEvent extends MessageHandler {

  private static final Logger LOGGER = LoggerFactory.getLogger(RoomUserWalkEvent.class);
  public static final String CONTROL_KEY = "control";

  @Override
  public int getRatelimit() {
    return Emulator.getConfig().getInt("pathfinder.click.delay", 0);
  }

  @Override
  public void handle() throws Exception {
    if (this.client.getHabbo().getHabboInfo().getCurrentRoom() == null) {
      return;
    }

    int x = this.packet.readInt(); // Position X
    int y = this.packet.readInt(); // Position Y

    Habbo habbo = getControlledHabbo();
    if (habbo == null) {
      return;
    }

    RoomUnit roomUnit = habbo.getRoomUnit();
    HabboInfo habboInfo = habbo.getHabboInfo();
    Room room = habboInfo.getCurrentRoom();

    try {
      if (roomUnit != null && roomUnit.isInRoom() && roomUnit.canWalk()) {
        if (roomUnit.cmdTeleport) {
          handleTeleport(room, (short) x, (short) y, roomUnit, habboInfo);
          return;
        }

        // Don't calculate a new path if we are on a horse
        if (habboInfo.getRiding() != null && habboInfo.getRiding().getTask() != null
            && habboInfo.getRiding().getTask().equals(PetTasks.JUMP)) {
          return;
        }

        // Don't calulcate a new path if are already at the end position
        if (x == roomUnit.getX() && y == roomUnit.getY()) {
          return;
        }

        if (room == null || room.getLayout() == null) {
          return;
        }

        if (roomUnit.isIdle()) {
          fireIdleEvent(habbo, roomUnit);
        }

        RoomTile tile = room.getLayout().getTile((short) x, (short) y);

        // this should never happen, if it does it would be a design flaw
        if (tile == null) {
          return;
        }

        if (habbo.getRoomUnit().hasStatus(RoomUnitStatus.LAY) && room.getLayout()
            .getTilesInFront(habbo.getRoomUnit().getCurrentLocation(),
                habbo.getRoomUnit().getBodyRotation().getValue(), 2).contains(tile)) {
          return;
        }

        if (room.canLayAt(tile.x, tile.y) && handleLay(room, tile, (short) x, (short) y,
            roomUnit)) {
          return;
        }

        THashSet<HabboItem> items = room.getItemsAt(tile);

        if (!items.isEmpty()) {
          for (HabboItem item : items) {
            RoomTile overriddenTile = item.getOverrideGoalTile(roomUnit, room, tile);

            if (overriddenTile == null) {
              return; // null cancels the entire event
            }

            if (!overriddenTile.equals(tile) && overriddenTile.isWalkable()) {
              tile = overriddenTile;
              break;
            }
          }
        }

        // This is where we set the end location and begin finding a path
        if (tile.isWalkable() || room.canSitOrLayAt(tile.x, tile.y)) {
          if (roomUnit.getMoveBlockingTask() != null) {
            roomUnit.getMoveBlockingTask().get();
          }

          roomUnit.setGoalLocation(tile);
        }
      }
    } catch (Exception e) {
      LOGGER.error("Caught exception", e);
    }
  }

  private static boolean handleLay(Room room, RoomTile tile, short x, short y, RoomUnit roomUnit) {
    HabboItem bed = room.getTopItemAt(tile.x, tile.y);

    if (bed != null && bed.getBaseItem().allowLay()) {
      RoomTile pillow = getPillow(room, x, y, bed);

      if (pillow != null && room.canLayAt(pillow.x, pillow.y)) {
        roomUnit.setGoalLocation(pillow);
        return true;
      }
    }
    return false;
  }

  private static RoomTile getPillow(Room room, short x, short y, HabboItem bed) {
    RoomTile pillow = room.getLayout().getTile(bed.getX(), bed.getY());
    switch (bed.getRotation()) {
      case 0:
      case 4:
        pillow = room.getLayout().getTile(x, bed.getY());
        break;
      case 2:
      case 8:
        pillow = room.getLayout().getTile(bed.getX(), y);
        break;
    }
    return pillow;
  }

  private static void fireIdleEvent(Habbo habbo, RoomUnit roomUnit) {
    UserIdleEvent event = new UserIdleEvent(habbo, UserIdleEvent.IdleReason.WALKED, false);
    Emulator.getPluginManager().fireEvent(event);

    if (!event.isCancelled() && !event.idle) {
      if (roomUnit.getRoom() != null) {
        roomUnit.getRoom().unIdle(habbo);
      }
      roomUnit.resetIdleTimer();
    }
  }

  private static void handleTeleport(Room room, short x, short y, RoomUnit roomUnit,
      HabboInfo habboInfo) {
    RoomTile t = room.getLayout().getTile(x, y);
    room.sendComposer(new RoomUnitOnRollerComposer(roomUnit, t, room).compose());

    if (habboInfo.getRiding() != null) {
      room.sendComposer(
          new RoomUnitOnRollerComposer(habboInfo.getRiding().getRoomUnit(), t, room).compose());
    }
  }

  private Habbo getControlledHabbo() {
    Habbo habbo = this.client.getHabbo();

    RoomUnit roomUnit = this.client.getHabbo().getRoomUnit();

    if (roomUnit.isTeleporting) {
      return null;
    }

    if (roomUnit.isKicked) {
      return null;
    }

    if (roomUnit.getCacheable().get(CONTROL_KEY) != null) {
      habbo = (Habbo) roomUnit.getCacheable().get(CONTROL_KEY);

      if (habbo.getHabboInfo().getCurrentRoom() != this.client.getHabbo().getHabboInfo()
          .getCurrentRoom()) {
        habbo.getRoomUnit().getCacheable().remove("controller");
        this.client.getHabbo().getRoomUnit().getCacheable().remove(CONTROL_KEY);
        habbo = this.client.getHabbo();
      }
    }
    return habbo;
  }
}
