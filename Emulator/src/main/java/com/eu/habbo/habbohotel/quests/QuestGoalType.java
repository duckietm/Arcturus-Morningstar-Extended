package com.eu.habbo.habbohotel.quests;

/** What a quest, a daily task or a reward-track task counts. */
public enum QuestGoalType {
    TALK_IN_ROOM,
    VISIT_ROOMS,
    PLACE_FURNI,
    GIVE_RESPECT,
    COMPLETE_QUEST,
    CLAIM_DAILY_TASK;

    /** Accepts the enum name or the official reward-track action name (chat_with_someone, place_item...). */
    public static QuestGoalType fromCode(String code) {
        if (code == null) {
            return null;
        }
        String normalized = code.trim().toUpperCase();
        switch (normalized) {
            case "CHAT_WITH_SOMEONE":
                return TALK_IN_ROOM;
            case "ENTER_OTHER_USERS_ROOM":
                return VISIT_ROOMS;
            case "PLACE_ITEM":
                return PLACE_FURNI;
            default:
                break;
        }
        try {
            return QuestGoalType.valueOf(normalized);
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    /** The official client action name, which picks the reward-track task icon. */
    public String actionType() {
        return switch (this) {
            case TALK_IN_ROOM -> "chat_with_someone";
            case VISIT_ROOMS -> "enter_other_users_room";
            case PLACE_FURNI -> "place_item";
            case GIVE_RESPECT -> "give_respect";
            case COMPLETE_QUEST -> "complete_quest";
            case CLAIM_DAILY_TASK -> "claim_daily_task";
        };
    }
}
