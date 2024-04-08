package com.skeletor.plugin.javascript.categories;

import com.eu.habbo.Emulator;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CommandManager {
  private static final Logger LOGGER = LoggerFactory.getLogger(CommandManager.class);
  
  private final List<Category> commandCategories;
  
  public CommandManager() {
    this.commandCategories = new ArrayList<>();
    reload();
  }
  
  public void reload() {
    dispose();
    try(Connection connection = Emulator.getDatabase().getDataSource().getConnection(); 
        PreparedStatement statement = connection.prepareStatement("SELECT * FROM `command_categories` ORDER BY `order`"); 
        ResultSet set = statement.executeQuery()) {
      while (set.next()) {
        Category category = new Category(set);
        this.commandCategories.add(category);
      } 
    } catch (SQLException e) {
      LOGGER.error("Caught SQL exception", e);
    } 
    try(Connection connection = Emulator.getDatabase().getDataSource().getConnection(); 
        PreparedStatement statement = connection.prepareStatement("SELECT * FROM `command_category_permissions` ORDER BY `order`"); 
        ResultSet set = statement.executeQuery()) {
      while (set.next()) {
        if (hasCommandCategory(Integer.valueOf(set.getInt("category_id")))) {
          Category category = getCommandCategory(Integer.valueOf(set.getInt("category_id")));
          ((Category)this.commandCategories.get(this.commandCategories.indexOf(category))).addPermission(set.getString("permission"));
        } 
      } 
    } catch (SQLException e) {
      LOGGER.error("Caught SQL exception", e);
    } 
  }
  
  public void dispose() {
    this.commandCategories.clear();
  }
  
  public boolean hasCommandCategory(Integer id) {
    return 
      
      (((List)this.commandCategories.stream().filter(p -> (p.getId() == id.intValue())).collect(Collectors.toList())).size() > 0);
  }
  
  public Category getCommandCategory(Integer id) {
    if (hasCommandCategory(id))
      return ((List<Category>)this.commandCategories
        .stream()
        .filter(p -> (p.getId() == id.intValue()))
        .collect(Collectors.toList())).get(0); 
    return null;
  }
  
  public List<Category> getCommandCategories() {
    return this.commandCategories;
  }
}
