package com.eu.habbo.habbohotel.permissions;

import gnu.trove.map.hash.THashMap;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;

public class Rank {

    private final int id;


    private final int level;
    private final THashMap<String, Permission> permissions;
    private final THashMap<String, String> variables;
    private String name;
    private String badge;
    private int roomEffect;


    private boolean logCommands;


    private String prefix;


    private String prefixColor;


    private boolean hasPrefix;
    private int diamondsTimerAmount;
    private int creditsTimerAmount;
    private int pixelsTimerAmount;
    private int gotwTimerAmount;

    public Rank(ResultSet set) throws SQLException {
        this.permissions = new THashMap<>();
        this.variables = new THashMap<>();
        this.id = set.getInt("id");
        this.level = set.getInt("level");
        this.diamondsTimerAmount = 1;
        this.creditsTimerAmount = 1;
        this.pixelsTimerAmount = 1;
        this.gotwTimerAmount = 1;

        this.load(set);
    }

    public void load(ResultSet set) throws SQLException {
        ResultSetMetaData meta = set.getMetaData();
        this.name = set.getString("rank_name");
        this.badge = set.getString("badge");
        this.roomEffect = set.getInt("room_effect");
        this.logCommands = set.getString("log_commands").equals("1");
        this.prefix = set.getString("prefix");
        this.prefixColor = set.getString("prefix_color");
        this.diamondsTimerAmount = set.getInt("auto_points_amount");
        this.creditsTimerAmount = set.getInt("auto_credits_amount");
        this.pixelsTimerAmount = set.getInt("auto_pixels_amount");
        this.gotwTimerAmount = set.getInt("auto_gotw_amount");
        this.hasPrefix = !this.prefix.isEmpty();
        for (int i = 1; i < meta.getColumnCount() + 1; i++) {
            String columnName = meta.getColumnName(i);
            if (columnName.startsWith("cmd_") || columnName.startsWith("acc_")) {
                this.permissions.put(meta.getColumnName(i), new Permission(columnName, PermissionSetting.fromString(set.getString(i))));
            } else {
                this.variables.put(meta.getColumnName(i), set.getString(i));
            }
        }
    }

    public boolean hasPermission(String key, boolean isRoomOwner) {
        if (this.permissions.containsKey(key)) {
            Permission permission = this.permissions.get(key);

            return permission.setting == PermissionSetting.ALLOWED || permission.setting == PermissionSetting.ROOM_OWNER && isRoomOwner;

        }

        return false;
    }


    public int getId() {
        return this.id;
    }


    public int getLevel() {
        return this.level;
    }


    public String getName() {
        return this.name;
    }

    public String getBadge() {
        return this.badge;
    }

    public THashMap<String, Permission> getPermissions() {
        return this.permissions;
    }

    public THashMap<String, String> getVariables() {
        return this.variables;
    }

    public int getRoomEffect() {
        return this.roomEffect;
    }

    public boolean isLogCommands() {
        return this.logCommands;
    }

    public String getPrefix() {
        return this.prefix;
    }

    public String getPrefixColor() {
        return this.prefixColor;
    }

    public boolean hasPrefix() {
        return this.hasPrefix;
    }

    public int getDiamondsTimerAmount() { return this.diamondsTimerAmount; }

    public int getCreditsTimerAmount() { return this.creditsTimerAmount; }

    public int getPixelsTimerAmount() { return this.pixelsTimerAmount; }

    public int getGotwTimerAmount() { return this.gotwTimerAmount; }
}

