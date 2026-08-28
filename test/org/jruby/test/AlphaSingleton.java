
package org.jruby.test;

public class AlphaSingleton {

  public static final AlphaSingleton getInstance() {
    return INSTANCE;
  }
  private static final AlphaSingleton INSTANCE = new AlphaSingleton();
  private AlphaSingleton() {
  }
  
  public String alpha() {
    return "Alpha";
  }

}
