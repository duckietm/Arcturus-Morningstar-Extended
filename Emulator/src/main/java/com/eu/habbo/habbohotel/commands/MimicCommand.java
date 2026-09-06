package com.eu.habbo.habbohotel.commands;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.gameclients.GameClient;
import com.eu.habbo.habbohotel.permissions.Permission;
import com.eu.habbo.habbohotel.permissions.Rank;
import com.eu.habbo.habbohotel.rooms.RoomChatMessageBubbles;
import com.eu.habbo.habbohotel.users.Habbo;
import com.eu.habbo.habbohotel.users.HabboGender;
import com.eu.habbo.habbohotel.users.HabboInfo;
import com.eu.habbo.habbohotel.users.HabboManager;
import com.eu.habbo.habbohotel.users.clothingvalidation.ClothingValidationManager;
import com.eu.habbo.messages.outgoing.rooms.users.RoomUserDataComposer;
import com.eu.habbo.messages.outgoing.users.UserDataComposer;
import com.eu.habbo.util.figure.FigureUtil;

public class MimicCommand extends Command {
    public MimicCommand() {
        super("cmd_mimic", Emulator.getTexts().getValue("commands.keys.cmd_mimic").split(";"));
    }

    @Override
    public boolean handle(GameClient gameClient, String[] params) throws Exception {
        if (params.length != 2) {
            gameClient.getHabbo().whisper(Emulator.getTexts().getValue("commands.error.cmd_mimic.not_found").replace("%user%", ""), RoomChatMessageBubbles.ALERT);
            return true;
        }

        // The target used to be looked up in the current room only, so :mimic failed for
        // anyone standing in another room and for every offline user. Widen it the way the
        // other user-targeting commands do: room first (cheapest), then any online user,
        // then the database.
        Habbo habbo = gameClient.getHabbo().getHabboInfo().getCurrentRoom().getHabbo(params[1]);

        if (habbo == null) {
            habbo = Emulator.getGameEnvironment().getHabboManager().getHabbo(params[1]);
        }

        HabboInfo targetInfo = (habbo != null) ? habbo.getHabboInfo() : HabboManager.getOfflineHabboInfo(params[1]);

        if (targetInfo == null) {
            gameClient.getHabbo().whisper(Emulator.getTexts().getValue("commands.error.cmd_mimic.not_found").replace("%user%", params[1]), RoomChatMessageBubbles.ALERT);
            return true;
        }

        if (targetInfo.getId() == gameClient.getHabbo().getHabboInfo().getId()) {
            gameClient.getHabbo().whisper(Emulator.getTexts().getValue("commands.error.cmd_mimic.not_self"), RoomChatMessageBubbles.ALERT);
            return true;
        }

        String genderName = targetInfo.getGender().equals(HabboGender.M)
                ? Emulator.getTexts().getValue("gender.him")
                : Emulator.getTexts().getValue("gender.her");

        if (BssCommandPreferences.isEnabled(targetInfo.getId(), BssCommandPreferences.Flag.BLOCK_MIMIC)) {
            gameClient.getHabbo().whisper(Emulator.getTexts().getValue("commands.error.cmd_mimic.blocked")
                    .replace("%user%", params[1])
                    .replace("%gender_name%", genderName), RoomChatMessageBubbles.ALERT);
            return true;
        }

        // Resolved through the rank rather than the Habbo object, so the protection still
        // applies when the target is offline and there is no Habbo to ask.
        Rank targetRank = targetInfo.getRank();
        boolean targetProtected = targetRank != null
                && Emulator.getGameEnvironment().getPermissionsManager().hasPermission(targetRank, Permission.ACC_NOT_MIMICED, false);

        if (targetProtected && !gameClient.getHabbo().hasPermission(Permission.ACC_NOT_MIMICED)) {
            gameClient.getHabbo().whisper(Emulator.getTexts().getValue("commands.error.cmd_mimic.blocked")
                    .replace("%user%", params[1])
                    .replace("%gender_name%", genderName), RoomChatMessageBubbles.ALERT);
            return true;
        }

        if (!gameClient.getHabbo().hasPermission("acc_mimic_unredeemed")
                && FigureUtil.hasBlacklistedClothing(targetInfo.getLook(), gameClient.getHabbo().getForbiddenClothing())) {
            gameClient.getHabbo().whisper(Emulator.getTexts().getValue("commands.error.cmd_mimic.forbidden_clothing"), RoomChatMessageBubbles.ALERT);
            return true;
        }

        gameClient.getHabbo().getHabboInfo().setLook(ClothingValidationManager.VALIDATE_ON_MIMIC
                ? ClothingValidationManager.validateLook(gameClient.getHabbo(), targetInfo.getLook(), targetInfo.getGender().name())
                : targetInfo.getLook());
        gameClient.getHabbo().getHabboInfo().setGender(targetInfo.getGender());
        gameClient.sendResponse(new UserDataComposer(gameClient.getHabbo()));
        gameClient.getHabbo().getHabboInfo().getCurrentRoom().sendComposer(new RoomUserDataComposer(gameClient.getHabbo()).compose());
        gameClient.getHabbo().whisper(Emulator.getTexts().getValue("commands.succes.cmd_mimic.copied")
                .replace("%user%", params[1])
                .replace("%gender_name%", genderName), RoomChatMessageBubbles.ALERT);
        return true;
    }
}
