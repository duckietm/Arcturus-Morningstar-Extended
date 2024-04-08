package com.skeletor.plugin.javascript.communication.incoming.common;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.gameclients.GameClient;
import com.eu.habbo.habbohotel.rooms.RoomChatMessageBubbles;
import com.eu.habbo.habbohotel.users.HabboItem;
import com.eu.habbo.messages.outgoing.MessageComposer;
import com.eu.habbo.messages.outgoing.users.UserCreditsComposer;
import com.skeletor.plugin.javascript.communication.incoming.IncomingWebMessage;
import com.skeletor.plugin.javascript.communication.outgoing.OutgoingWebMessage;
import com.skeletor.plugin.javascript.communication.outgoing.common.UpdateCreditsComposer;
import com.skeletor.plugin.javascript.communication.outgoing.slotmachine.SpinResultComposer;
import com.skeletor.plugin.javascript.override_packets.outgoing.JavascriptCallbackComposer;

public class RequestSpinSlotMachineEvent extends IncomingWebMessage<RequestSpinSlotMachineEvent.JSONRequestSpinSlotMachineEvent> {
  private static final int maxNumber = 5;
  
  private static final int LEMON = 0;
  
  private static final int MELON = 1;
  
  private static final int GRAPES = 2;
  
  private static final int CHERRY = 3;
  
  private static final int BAR = 4;
  
  public RequestSpinSlotMachineEvent() {
    super(JSONRequestSpinSlotMachineEvent.class);
  }
  
  public void handle(GameClient client, JSONRequestSpinSlotMachineEvent message) {
    HabboItem item = client.getHabbo().getRoomUnit().getRoom().getHabboItem(message.itemId);
    if (item == null)
      return; 
    if (message.bet <= 0 || message.bet > client.getHabbo().getHabboInfo().getCredits())
      return; 
    client.getHabbo().getHabboInfo().addCredits(-message.bet);
    client.sendResponse((MessageComposer)new UserCreditsComposer(client.getHabbo()));
    client.getHabbo().talk(Emulator.getTexts().getValue("slot.machines.spin", "* Bets %amount% on Slots Machine *").replace("%amount%", message.bet + ""), RoomChatMessageBubbles.ALERT);
    client.sendResponse((MessageComposer)new JavascriptCallbackComposer((OutgoingWebMessage)new UpdateCreditsComposer(client.getHabbo().getHabboInfo().getCredits())));
    int result1 = Emulator.getRandom().nextInt(5);
    int result2 = Emulator.getRandom().nextInt(5);
    int result3 = Emulator.getRandom().nextInt(5);
    int amountWon = 0;
    boolean won = false;
    if (result1 == result2 && result2 == result3) {
      won = true;
      switch (result1) {
        case 0:
          amountWon = 5 * message.bet;
          break;
        case 1:
          amountWon = 6 * message.bet;
          break;
        case 2:
          amountWon = 10 * message.bet;
          break;
        case 3:
          amountWon = 15 * message.bet;
          break;
        case 4:
          amountWon = 20 * message.bet;
          break;
      } 
      client.getHabbo().getHabboInfo().addCredits(amountWon);
    } else if (result1 == 4 && result2 == 4) {
      won = true;
      amountWon = 4 * message.bet;
      client.getHabbo().getHabboInfo().addCredits(amountWon);
    } else if (result1 == 3 && result2 == 3) {
      won = true;
      amountWon = 3 * message.bet;
      client.getHabbo().getHabboInfo().addCredits(amountWon);
    } else if (result1 == 3) {
      won = true;
      amountWon = 2 * message.bet;
      client.getHabbo().getHabboInfo().addCredits(amountWon);
    } 
    SpinResultComposer resultComposer = new SpinResultComposer(result1, result2, result3, won, amountWon);
    client.sendResponse((MessageComposer)new JavascriptCallbackComposer((OutgoingWebMessage)resultComposer));
    int finalAmount = amountWon;
    Emulator.getThreading().run(() -> client.getHabbo().talk(Emulator.getTexts().getValue("slot.machines.won", "* Won %amount% in Slots Machine *").replace("%amount%", finalAmount + ""), RoomChatMessageBubbles.ALERT), 5000L);
  }
  
  static class JSONRequestSpinSlotMachineEvent {
    int itemId;
    
    int bet;
  }
}
