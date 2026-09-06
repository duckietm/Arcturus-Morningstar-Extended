package com.eu.habbo.messages.outgoing.furnieditor;

import com.eu.habbo.habbohotel.items.editor.FurniEditorRepository;
import com.eu.habbo.messages.ServerMessage;
import com.eu.habbo.messages.outgoing.MessageComposer;
import com.eu.habbo.messages.outgoing.Outgoing;

/** Furni editor: the items_crackable configuration of one base item (or "none" when the row does not exist). */
public class FurniEditorCrackableComposer extends MessageComposer {
    private final int itemId;
    private final FurniEditorRepository.Crackable crackable;

    public FurniEditorCrackableComposer(int itemId, FurniEditorRepository.Crackable crackable) {
        this.itemId = itemId;
        this.crackable = crackable;
    }

    @Override
    protected ServerMessage composeInternal() {
        this.response.init(Outgoing.FurniEditorCrackableComposer);
        this.response.appendInt(this.itemId);
        this.response.appendBoolean(this.crackable != null);

        if (this.crackable == null) {
            this.response.appendInt(0);
            this.response.appendString("");
            this.response.appendString("");
            this.response.appendInt(0);
            this.response.appendInt(0);
            this.response.appendString("");
            this.response.appendInt(0);
            return this.response;
        }

        this.response.appendInt(this.crackable.count());
        this.response.appendString(nonNull(this.crackable.achievementTick()));
        this.response.appendString(nonNull(this.crackable.achievementCracked()));
        this.response.appendInt(this.crackable.requiredEffect());
        this.response.appendInt(this.crackable.subscriptionDuration());
        this.response.appendString(nonNull(this.crackable.subscriptionType()));

        this.response.appendInt(this.crackable.prizes().size());
        for (FurniEditorRepository.CrackablePrize prize : this.crackable.prizes()) {
            this.response.appendInt(prize.itemId());
            this.response.appendString(nonNull(prize.itemName()));
            this.response.appendString(nonNull(prize.publicName()));
            this.response.appendInt(prize.spriteId());
            this.response.appendString(nonNull(prize.type()));
            this.response.appendInt(prize.chance());
        }

        return this.response;
    }

    private static String nonNull(String value) {
        return value == null ? "" : value;
    }
}
