package com.eu.habbo.habbohotel.commands;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.gameclients.GameClient;
import com.eu.habbo.habbohotel.rooms.RoomChatMessageBubbles;

final class BssPlacementCommand extends Command {
    enum Mode {
        BATCH,
        FORCE_HEIGHT,
        FORCE_ROTATION
    }

    private final Mode mode;

    BssPlacementCommand(String permission, Mode mode) {
        super(permission, Emulator.getTexts().getValue("commands.keys." + permission).split(";"));
        this.mode = mode;
    }

    @Override
    public boolean handle(GameClient gameClient, String[] params) {
        try {
            int userId = gameClient.getHabbo().getHabboInfo().getId();
            if (params.length > 1 && (params[1].equalsIgnoreCase("off") || params[1].equals("-1"))) {
                if (mode == Mode.FORCE_HEIGHT) BssPlacementPreferences.setForcedHeight(userId, null);
                if (mode == Mode.FORCE_ROTATION) BssPlacementPreferences.setForcedRotation(userId, null);
                send(gameClient, "commands.success." + permission + ".disabled");
                return true;
            }

            switch (mode) {
                case BATCH -> {
                    if (params.length != 3) return usage(gameClient);
                    int count = Integer.parseInt(params[1]);
                    double height = Double.parseDouble(params[2].replace(',', '.'));
                    if (count < 1 || count > 500 || height < 0 || height > 40) return usage(gameClient);
                    BssPlacementPreferences.setBatch(userId, count, height);
                }
                case FORCE_HEIGHT -> {
                    if (params.length != 2) return usage(gameClient);
                    double height = Double.parseDouble(params[1].replace(',', '.'));
                    if (height < 0 || height > 40) return usage(gameClient);
                    BssPlacementPreferences.setForcedHeight(userId, height);
                }
                case FORCE_ROTATION -> {
                    if (params.length != 2) return usage(gameClient);
                    int rotation = Integer.parseInt(params[1]);
                    if (rotation < 0 || rotation > 7) return usage(gameClient);
                    BssPlacementPreferences.setForcedRotation(userId, rotation);
                }
            }
            send(gameClient, "commands.success." + permission);
        } catch (NumberFormatException ignored) {
            return usage(gameClient);
        }
        return true;
    }

    private boolean usage(GameClient gameClient) {
        send(gameClient, "commands.error." + permission + ".usage");
        return true;
    }

    private void send(GameClient gameClient, String key) {
        gameClient.getHabbo().whisper(Emulator.getTexts().getValue(key), RoomChatMessageBubbles.ALERT);
    }
}
