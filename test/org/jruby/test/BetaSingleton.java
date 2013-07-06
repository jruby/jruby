
package org.jruby.test;

public class BetaSingleton {

  public static final BetaSingleton getInstance() {
    return INSTANCE;
  }
  private static final BetaSingleton INSTANCE = new BetaSingleton();
  private BetaSingleton() {
  }
  
  public String beta() {
    return "Beta";
  }

}
