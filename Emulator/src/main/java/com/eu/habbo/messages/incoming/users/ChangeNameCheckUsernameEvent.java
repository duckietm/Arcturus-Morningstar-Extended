package com.eu.habbo.messages.incoming.users;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.users.HabboManager;
import com.eu.habbo.messages.incoming.MessageHandler;
import com.eu.habbo.messages.outgoing.rooms.users.RoomUserNameChangedComposer;
import com.eu.habbo.messages.outgoing.users.ChangeNameCheckResultComposer;

import java.util.ArrayList;
import java.util.List;

public class ChangeNameCheckUsernameEvent extends MessageHandler {
    public static String VALID_CHARACTERS = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890_-=!?@:,.";

    @Override
    public void handle() throws Exception {
        if (!this.client.getHabbo().getHabboStats().allowNameChange)
            return;

        String name = this.packet.readString();

        int errorCode = ChangeNameCheckResultComposer.AVAILABLE;

        List<String> suggestions = new ArrayList<>(4);
        if (name.length() < 3) {
            errorCode = ChangeNameCheckResultComposer.TOO_SHORT;
        } else if (name.length() > 15) {
            errorCode = ChangeNameCheckResultComposer.TOO_LONG;
        } else if (name.equalsIgnoreCase(this.client.getHabbo().getHabboInfo().getUsername()) || HabboManager.getOfflineHabboInfo(name) != null || ConfirmChangeNameEvent.changingUsernames.contains(name.toLowerCase())) {
            errorCode = ChangeNameCheckResultComposer.TAKEN_WITH_SUGGESTIONS;
            suggestions.add(name + Emulator.getRandom().nextInt(9999));
            suggestions.add(name + Emulator.getRandom().nextInt(9999));
            suggestions.add(name + Emulator.getRandom().nextInt(9999));
            suggestions.add(name + Emulator.getRandom().nextInt(9999));
        } else if (!Emulator.getGameEnvironment().getWordFilter().filter(name, this.client.getHabbo()).equalsIgnoreCase(name)) {
            errorCode = ChangeNameCheckResultComposer.NOT_VALID;
        } else {
            String checkName = name;
            for (char c : VALID_CHARACTERS.toCharArray()) {
                checkName = checkName.replace(c + "", "");
            }

            if (!checkName.isEmpty()) {
                errorCode = ChangeNameCheckResultComposer.NOT_VALID;
            } else {
                this.client.getHabbo().getHabboStats().changeNameChecked = name;
            }
        }

        this.client.sendResponse(new ChangeNameCheckResultComposer(errorCode, name, suggestions));
    }
}
