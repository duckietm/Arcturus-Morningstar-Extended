package com.skeletor.plugin.javascript.categories;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class Category {
  private int id;
  
  private String name;
  
  private List<String> permissions;
  
  public Category(ResultSet set) throws SQLException {
    this.id = set.getInt("id");
    this.name = set.getString("name");
    this.permissions = new ArrayList<>();
  }
  
  public void addPermission(String permission) {
    if (!this.permissions.contains(permission))
      this.permissions.add(permission); 
  }
  
  public int getId() {
    return this.id;
  }
  
  public String getName() {
    return this.name;
  }
  
  public List<String> getPermissions() {
    return this.permissions;
  }
}
