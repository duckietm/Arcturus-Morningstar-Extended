package com.eu.habbo.messages.outgoing.inventory;

import com.eu.habbo.habbohotel.items.FurnitureType;
import com.eu.habbo.habbohotel.items.interactions.InteractionGift;
import com.eu.habbo.habbohotel.users.HabboItem;
import com.eu.habbo.messages.ServerMessage;
import com.eu.habbo.messages.outgoing.MessageComposer;
import com.eu.habbo.messages.outgoing.Outgoing;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;

public class InventoryItemsComposer extends MessageComposer {
    private static final Logger LOGGER = LoggerFactory.getLogger(InventoryItemsComposer.class);

    private final int fragmentNumber;
    private final int totalFragments;
    private final Int2ObjectMap<HabboItem> items;

    public InventoryItemsComposer(int fragmentNumber, int totalFragments, Int2ObjectMap<HabboItem> items) {
        this.fragmentNumber = fragmentNumber;
        this.totalFragments = totalFragments;
        this.items = items;
    }

    @Override
    protected ServerMessage composeInternal() {
        try {
            this.response.init(Outgoing.InventoryItemsComposer);
            this.response.appendInt(this.totalFragments);
            this.response.appendInt(this.fragmentNumber - 1);
            this.response.appendInt(this.items.size());

            for (HabboItem habboItem : this.items.values()) {
                serializeInventoryItem(this.response, habboItem);
            }
            return this.response;
        } catch (Exception e) {
            LOGGER.error("Caught exception", e);
        }

        return null;
    }

    /**
     * One inventory item, in the layout the client's FurnitureListItemParser reads. Shared with
     * InventoryUpdateItemComposer (the single-item "add or update" the client applies without refetching): the two
     * used to be written separately and drifted, so a furni pushed one way could differ from the same furni listed.
     */
    static void serializeInventoryItem(ServerMessage response, HabboItem habboItem) {
        response.appendInt(habboItem.getGiftAdjustedId());
        response.appendString(habboItem.getBaseItem().getType().code);
        response.appendInt(habboItem.getId());
        response.appendInt(habboItem.getBaseItem().getSpriteId());

        String name = habboItem.getBaseItem().getName();

        if (name.equals("floor") || name.equals("landscape") || name.equals("song_disk") || name.equals("wallpaper") || name.equals("poster")) {
            switch (name) {
                case "landscape":
                    response.appendInt(4);
                    break;
                case "floor":
                    response.appendInt(3);
                    break;
                case "wallpaper":
                    response.appendInt(2);
                    break;
                case "poster":
                    response.appendInt(6);
                    break;
                case "song_disk":
                    response.appendInt(8);
                    break;
            }
            response.appendInt(0);
            response.appendString(habboItem.getExtradata());
        } else {
            if (name.equals("gnome_box"))
                response.appendInt(13);
            else
                response.appendInt(habboItem instanceof InteractionGift ? ((((InteractionGift) habboItem).getColorId() * 1000) + ((InteractionGift) habboItem).getRibbonId()) : 1);

            habboItem.serializeExtradata(response);
        }
        response.appendBoolean(habboItem.getBaseItem().allowRecyle());
        response.appendBoolean(habboItem.getBaseItem().allowTrade());
        response.appendBoolean(!habboItem.isLimited() && habboItem.getBaseItem().allowInventoryStack());
        response.appendBoolean(habboItem.getBaseItem().allowMarketplace());
        response.appendInt(-1);
        response.appendBoolean(true);
        response.appendInt(-1);

        if (habboItem.getBaseItem().getType() == FurnitureType.FLOOR) {
            response.appendString("");
            if (name.equals("song_disk")) {
                List<String> extraDataAsList = Arrays.asList(habboItem.getExtradata().split("\n"));
                response.appendInt(Integer.valueOf(extraDataAsList.get(extraDataAsList.size() - 1)));
                return;
            }
            response.appendInt(habboItem instanceof InteractionGift ? ((((InteractionGift) habboItem).getColorId() * 1000) + ((InteractionGift) habboItem).getRibbonId()) : 1);
        }
    }

    public int getFragmentNumber() {
        return fragmentNumber;
    }

    public int getTotalFragments() {
        return totalFragments;
    }

    public Int2ObjectMap<HabboItem> getItems() {
        return items;
    }
}
