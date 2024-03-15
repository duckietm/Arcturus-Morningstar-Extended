package com.furnibuilder.afk;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.commands.Command;
import com.eu.habbo.habbohotel.commands.CommandHandler;
import com.eu.habbo.habbohotel.users.Habbo;
import com.eu.habbo.plugin.EventHandler;
import com.eu.habbo.plugin.EventListener;
import com.eu.habbo.plugin.HabboPlugin;
import com.eu.habbo.plugin.events.emulator.EmulatorLoadedEvent;
import com.eu.habbo.plugin.events.users.UserLoginEvent;
import com.furnibuilder.afk.commands.AfkCommand;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;

public class Afk extends HabboPlugin implements EventListener {
  public static Afk INSTANCE = null;
  
  public static String AFK_KEY = "afk.real.key";
  
  public void onEnable() {
    INSTANCE = this;
    Emulator.getPluginManager().registerEvents(this, this);
    if (Emulator.isReady) {
      checkDatabase();
      AfkCommand.startBackgroundThread();
    } 
    Emulator.getLogging().logStart("[Afk] Started BeRightBack Command Plugin!");
  }
  
  public void onDisable() {
    Emulator.getLogging().logShutdownLine("[Afk] Stopped BeRightBack Command Plugin!");
  }
  
  @EventHandler
  public static void onEmulatorLoaded(EmulatorLoadedEvent event) {
    INSTANCE.checkDatabase();
    AfkCommand.startBackgroundThread();
  }
  
  public boolean hasPermission(Habbo habbo, String s) {
    return false;
  }
  
  private void checkDatabase() {
    boolean reloadPermissions = false;
    try(Connection connection = Emulator.getDatabase().getDataSource().getConnection(); Statement statement = connection.createStatement()) {
      statement.execute("ALTER TABLE  `emulator_texts` CHANGE  `value`  `value` VARCHAR( 4096 ) CHARACTER SET latin1 COLLATE latin1_swedish_ci NOT NULL");
    } catch (SQLException sQLException) {}
    Emulator.getTexts().register("commands.description.cmd_afk", ":afk");
    Emulator.getTexts().register("afk.cmd_afk.keys", "afk;brb");
    Emulator.getTexts().register("afk.cmd_afk.brb", "* %username% is now AFK! *");
    Emulator.getTexts().register("afk.cmd_afk.back", "* %username% is now back! *");
    Emulator.getTexts().register("afk.cmd_afk.time", "* %username% has now been away for %time% minutes with reason: %reason% *");
    Emulator.getConfig().register("afk.effect_id", "565");
    Emulator.getConfig().register("brb.effect_id", "566");
    reloadPermissions = registerPermission("cmd_afk", "'0', '1', '2'", "1", reloadPermissions);
    if (reloadPermissions)
      Emulator.getGameEnvironment().getPermissionsManager().reload(); 
    CommandHandler.addCommand((Command)new AfkCommand("cmd_afk", Emulator.getTexts().getValue("afk.cmd_afk.keys").split(";")));
  }
  
  private boolean registerPermission(String name, String options, String defaultValue, boolean defaultReturn) {
    try(Connection connection = Emulator.getDatabase().getDataSource().getConnection(); 
        PreparedStatement statement = connection.prepareStatement("ALTER TABLE  `permissions` ADD  `" + name + "` ENUM(  " + options + " ) NOT NULL DEFAULT  '" + defaultValue + "'")) {
      statement.execute();
      return true;
    } catch (SQLException sQLException) {
      return defaultReturn;
    } 
  }
  
  @EventHandler
  public static void onUserLoginEvent(UserLoginEvent event) {
    if (event.habbo == null || event.habbo.getClient() == null)
      return; 
    (event.habbo.getHabboStats()).cache.put(AFK_KEY, "");
  }
  
  public static void main(String[] args) {
    System.out.println("Don't run this seperately");
  }
}
