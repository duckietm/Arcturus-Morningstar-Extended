package com.eu.habbo.habbohotel.items;

import com.eu.habbo.Emulator;

public enum PostItColor {

    BLUE("9CCEFF"),


    GREEN("9CFF9C"),


    PINK("FF9CFF"),


    YELLOW("FFFF33"),


    RED("FF9C9D"),


    ORANGE("FFCD9C"),


    PURPLE("C3B1E1"),


    LIGHTBLUE("DBDEFB"),


    WHITE("FFFFFF"),


    BLACK("282828");

    public final String hexColor;

    PostItColor(String hexColor) {
        this.hexColor = hexColor;
    }


    public static boolean isCustomColor(String color) {
        for (PostItColor postItColor : PostItColor.values()) {
            if (postItColor.hexColor.equalsIgnoreCase(color))
                return false;
        }

        return true;
    }

    public static PostItColor randomColorNotYellow() {
        return PostItColor.values()[Emulator.getRandom().nextInt(3)];
    }
}
