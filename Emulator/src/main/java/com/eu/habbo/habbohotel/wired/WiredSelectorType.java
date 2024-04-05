package com.eu.habbo.habbohotel.wired;

public enum WiredSelectorType {
    AREA(0),
    FURNI_SOBRE_FURNI(1),
    FURNI_SELECCIONADOS(2),
    FURNI_ALTURA(3),
    FURNI_BYTYPE(4),
    USER_BY_TYPE(6),
    USER_BY_TEAM(7),
    USER_BY_HANDITEM(8),
    USER_BY_ACTION(9),
    USER_BY_USERNAME(10),
    FILTER_X_FURNI(18),
    FILTER_X_USER(19);

    public final int code;

    WiredSelectorType(int code) {
        this.code = code;
    }
}
