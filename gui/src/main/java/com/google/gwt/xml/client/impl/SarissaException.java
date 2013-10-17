package com.google.gwt.xml.client.impl;

import com.google.gwt.core.client.JavaScriptException;

public class SarissaException extends Exception {
  private String name;
  private String description;
  
  public String getName() {
    return name;
  }
  public String getDescription() {
    return description;
  }

  public SarissaException(String msg) {
    super(msg);
  }
  
  public SarissaException(JavaScriptException e) {
    super(e.getName() + " " + e.getDescription());
    name = e.getName();
    description = e.getDescription();
  }
  
  public SarissaException() {
    super();
  }
}
