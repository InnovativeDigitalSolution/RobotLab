package org.myrobotlab.framework.interfaces;

public interface StateSaver {

  public boolean load();
  
  public boolean loadFromJson(String json);

  public boolean save();
}
