package com.eu.habbo.habbohotel.wired.core;

import com.eu.habbo.habbohotel.rooms.Room;
import com.eu.habbo.habbohotel.rooms.RoomTile;
import com.eu.habbo.habbohotel.rooms.RoomUnit;
import com.eu.habbo.habbohotel.users.HabboItem;
import com.eu.habbo.habbohotel.wired.WiredTriggerType;

import java.util.Optional;

/**
 * Immutable event representing what happened in the room that triggered wired execution.
 * <p>
 * This replaces the scattered {@code Object[] stuff} parameter pattern with a strongly-typed,
 * immutable event object. Triggers produce/receive this event.
 * </p>
 * 
 * <h3>Usage Example:</h3>
 * <pre>{@code
 * WiredEvent event = WiredEvent.builder(WiredEvent.Type.USER_WALKS_ON, room)
 *     .actor(roomUnit)
 *     .sourceItem(steppedItem)
 *     .tile(roomUnit.getCurrentLocation())
 *     .build();
 * }</pre>
 * 
 * @see WiredContext
 * @see WiredEngine
 */
public final class WiredEvent {

    /**
     * Types of wired events that can occur in a room.
     * Maps to the legacy {@link WiredTriggerType} but with cleaner naming.
     */
    public enum Type {
        /** User says something in chat */
        USER_SAYS(WiredTriggerType.SAY_SOMETHING),
        
        /** User walks onto furniture */
        USER_WALKS_ON(WiredTriggerType.WALKS_ON_FURNI),
        
        /** User walks off furniture */
        USER_WALKS_OFF(WiredTriggerType.WALKS_OFF_FURNI),
        
        /** Furniture state is toggled/changed */
        FURNI_STATE_CHANGED(WiredTriggerType.STATE_CHANGED),
        
        /** Timer fires at a given time */
        TIMER_TICK(WiredTriggerType.AT_GIVEN_TIME),
        
        /** Timer fires periodically/repeatedly */
        TIMER_REPEAT(WiredTriggerType.PERIODICALLY),
        
        /** Long timer repeat */
        TIMER_REPEAT_LONG(WiredTriggerType.PERIODICALLY_LONG),
        
        /** User enters the room */
        USER_ENTERS_ROOM(WiredTriggerType.ENTER_ROOM),
        
        /** Game starts */
        GAME_STARTS(WiredTriggerType.GAME_STARTS),
        
        /** Game ends */
        GAME_ENDS(WiredTriggerType.GAME_ENDS),
        
        /** Bot collision */
        BOT_COLLISION(WiredTriggerType.COLLISION),
        
        /** Bot reached furniture (STF = stack tile furni) */
        BOT_REACHED_FURNI(WiredTriggerType.BOT_REACHED_STF),
        
        /** Bot reached habbo (AVTR = avatar) */
        BOT_REACHED_HABBO(WiredTriggerType.BOT_REACHED_AVTR),
        
        /** Score threshold achieved */
        SCORE_ACHIEVED(WiredTriggerType.SCORE_ACHIEVED),
        
        /** User starts idling */
        USER_IDLES(WiredTriggerType.IDLES),
        
        /** User stops idling */
        USER_UNIDLES(WiredTriggerType.UNIDLES),
        
        /** User starts dancing */
        USER_STARTS_DANCING(WiredTriggerType.STARTS_DANCING),
        
        /** User stops dancing */
        USER_STOPS_DANCING(WiredTriggerType.STOPS_DANCING),
        
        /** Team wins a game */
        TEAM_WINS(WiredTriggerType.CUSTOM),
        
        /** Team loses a game */
        TEAM_LOSES(WiredTriggerType.CUSTOM),
        
        /** Custom trigger type for plugins */
        CUSTOM(WiredTriggerType.CUSTOM);

        private final WiredTriggerType legacyType;

        Type(WiredTriggerType legacyType) {
            this.legacyType = legacyType;
        }

        /**
         * Get the legacy trigger type for backwards compatibility.
         * @return the corresponding {@link WiredTriggerType}
         */
        public WiredTriggerType toLegacyType() {
            return legacyType;
        }

        /**
         * Convert from legacy trigger type to new event type.
         * @param legacyType the legacy trigger type
         * @return the corresponding event type
         */
        public static Type fromLegacyType(WiredTriggerType legacyType) {
            for (Type type : values()) {
                if (type.legacyType == legacyType) {
                    return type;
                }
            }
            return CUSTOM;
        }
    }

    private final Type type;
    private final Room room;
    private final RoomUnit actor;       // nullable
    private final HabboItem sourceItem; // nullable
    private final RoomTile tile;        // nullable
    private final String text;          // nullable
    private final Object[] legacyStuff; // for adapter compatibility
    private final long createdAtMs;

    private WiredEvent(Builder builder) {
        this.type = builder.type;
        this.room = builder.room;
        this.actor = builder.actor;
        this.sourceItem = builder.sourceItem;
        this.tile = builder.tile;
        this.text = builder.text;
        this.legacyStuff = builder.legacyStuff;
        this.createdAtMs = builder.createdAtMs;
    }

    // Getters

    /**
     * Get the type of this event.
     * @return the event type
     */
    public Type getType() {
        return type;
    }

    /**
     * Get the room where this event occurred.
     * @return the room (never null)
     */
    public Room getRoom() {
        return room;
    }

    /**
     * Get the actor (user) that caused this event.
     * @return optional containing the actor, or empty if no user involved
     */
    public Optional<RoomUnit> getActor() {
        return Optional.ofNullable(actor);
    }

    /**
     * Get the source item that was involved in this event.
     * For example, the furniture that was clicked or stepped on.
     * @return optional containing the source item, or empty if no item involved
     */
    public Optional<HabboItem> getSourceItem() {
        return Optional.ofNullable(sourceItem);
    }

    /**
     * Get the tile where this event occurred.
     * @return optional containing the tile, or empty if no specific tile
     */
    public Optional<RoomTile> getTile() {
        return Optional.ofNullable(tile);
    }

    /**
     * Get the text associated with this event.
     * For example, the message in a SAY_SOMETHING trigger.
     * @return optional containing the text, or empty if no text
     */
    public Optional<String> getText() {
        return Optional.ofNullable(text);
    }

    /**
     * Get the legacy stuff array for adapter compatibility.
     * This allows legacy triggers/conditions/effects to work with the new system.
     * @return the legacy stuff array, or empty array if not set
     */
    public Object[] getLegacyStuff() {
        return legacyStuff != null ? legacyStuff : new Object[0];
    }

    /**
     * Get the timestamp when this event was created.
     * @return milliseconds since epoch
     */
    public long getCreatedAtMs() {
        return createdAtMs;
    }

    /**
     * Create a new builder for constructing events.
     * @param type the event type (required)
     * @param room the room where event occurred (required)
     * @return a new builder instance
     */
    public static Builder builder(Type type, Room room) {
        return new Builder(type, room);
    }

    /**
     * Create a new builder from a legacy trigger type.
     * @param legacyType the legacy trigger type
     * @param room the room where event occurred
     * @return a new builder instance
     */
    public static Builder fromLegacy(WiredTriggerType legacyType, Room room) {
        return new Builder(Type.fromLegacyType(legacyType), room);
    }

    @Override
    public String toString() {
        return "WiredEvent{" +
                "type=" + type +
                ", room=" + (room != null ? room.getId() : "null") +
                ", actor=" + (actor != null ? actor.getId() : "null") +
                ", sourceItem=" + (sourceItem != null ? sourceItem.getId() : "null") +
                ", createdAtMs=" + createdAtMs +
                '}';
    }

    /**
     * Builder for constructing immutable WiredEvent instances.
     */
    public static final class Builder {
        private final Type type;
        private final Room room;
        private RoomUnit actor;
        private HabboItem sourceItem;
        private RoomTile tile;
        private String text;
        private Object[] legacyStuff;
        private long createdAtMs = System.currentTimeMillis();

        private Builder(Type type, Room room) {
            if (type == null) throw new IllegalArgumentException("Event type cannot be null");
            if (room == null) throw new IllegalArgumentException("Room cannot be null");
            this.type = type;
            this.room = room;
        }

        /**
         * Set the actor (user) that caused this event.
         * @param actor the room unit
         * @return this builder
         */
        public Builder actor(RoomUnit actor) {
            this.actor = actor;
            return this;
        }

        /**
         * Set the source item involved in this event.
         * @param sourceItem the habbo item
         * @return this builder
         */
        public Builder sourceItem(HabboItem sourceItem) {
            this.sourceItem = sourceItem;
            return this;
        }

        /**
         * Set the tile where this event occurred.
         * @param tile the room tile
         * @return this builder
         */
        public Builder tile(RoomTile tile) {
            this.tile = tile;
            return this;
        }

        /**
         * Set the text associated with this event.
         * @param text the text content
         * @return this builder
         */
        public Builder text(String text) {
            this.text = text;
            return this;
        }

        /**
         * Set the legacy stuff array for adapter compatibility.
         * @param stuff the legacy object array
         * @return this builder
         */
        public Builder legacyStuff(Object[] stuff) {
            this.legacyStuff = stuff;
            return this;
        }

        /**
         * Set a custom creation timestamp.
         * @param createdAtMs milliseconds since epoch
         * @return this builder
         */
        public Builder createdAtMs(long createdAtMs) {
            this.createdAtMs = createdAtMs;
            return this;
        }

        /**
         * Build the immutable WiredEvent.
         * @return the constructed event
         */
        public WiredEvent build() {
            return new WiredEvent(this);
        }
    }
}
