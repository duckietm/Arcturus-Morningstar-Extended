package com.eu.habbo.habbohotel.rooms;

public enum FurnitureMovementError {
    NONE(""),
    NO_RIGHTS("${room.error.cant_set_not_owner}"),
    INVALID_MOVE("${room.error.cant_set_item}"),
    CANT_STACK("${room.error.cant_set_item}"),
    CANCEL_PLUGIN_PLACE("${room.error.plugin_place}"),
    CANCEL_PLUGIN_MOVE("${room.error.plugin_move}"),
    CANCEL_PLUGIN_ROTATE("${room.error.plugin_rotate}"),
    TILE_HAS_HABBOS("${room.error.cant_set_item}"),
    TILE_HAS_PETS("${room.error.cant_set_item}"),
    TILE_HAS_BOTS("${room.error.cant_set_item}"),
    MAX_DIMMERS("${room.error.max_dimmers}"),
    MAX_SOUNDFURNI("${room.errors.max_soundfurni}"),
    MAX_ITEMS("${room.error.max_furniture}"),
    MAX_STICKIES("${room.error.max_stickies}");


    public final String errorCode;

    FurnitureMovementError(String errorCode) {
        this.errorCode = errorCode;
    }
}