
package org.jruby.test;

public class BetaSingleton {

  public static final BetaSingleton getInstance() {
    return INSTANCE;
  }
  private static final BetaSingleton INSTANCE = new BetaSingleton();
  private BetaSingleton() { /* no-op */ }

  // class :

  public static String betac() {
      return "betaClass";
  }

  public static boolean isBetac() {
      return true;
  }

  public static String getBetac() {
      return "BetaClass";
  }

  // 2

  public static boolean isBetac2() {
    return true;
  }

  public static String getBetac2() {
    return "BetaClass2";
  }

  // 3

  public static String getBetac3() {
    return "BetaClass3";
  }

  public static boolean isBetac3() {
    return true;
  }

  // 4

  public static String getBetac4() {
    return "BetaClass4";
  }

  public static Object betac4() {
    return "betaClass4";
  }

  //

  public String getBeta() { return "Beta"; }

  public String beta() { return "beta"; }

  public boolean isBeta() { return true; }

  // 2

  public String getBeta2() { return "Beta2"; }

  public boolean isBeta2() { return true; }

  // 3

  public boolean isBeta3() { return true; }

  public String getBeta3() { return "Beta3"; }

  // 4

  public boolean beta4() {
    return true;
  }

  public String getBeta4() {
    return "Beta4";
  }

  // 5

  public String getBeta5() {
    return "Beta5";
  }

  public boolean beta5(final Object arg) {
    return true;
  }

  // 6

  public Object beta6() {
    return "beta6";
  }

  public boolean isBeta6() {
    return true;
  }

  // 7

  public boolean isBeta7() {
    return true;
  }

  public Boolean beta7() {
    return null;
  }

  //

  public static String betaCasedc() {
    return "betaCasedc";
  }

  public static boolean isBetaCasedc() {
    return true;
  }

  public static String getBetaCasedc() {
    return "BetaCasedc";
  }

  // 2

  public static boolean isBetaCasedc2() {
    return true;
  }

  public static String getBetaCasedc2() {
    return "BetaCasedc2";
  }

  // 3

  public static String getBetaCasedc3() {
    return "BetaCasedc3";
  }

  public static boolean isBetaCasedc3() {
    return true;
  }

  //

  public String getBetaCased() { return "BetaCased"; }

  public String betaCased() { return "betaCased"; }

  public boolean isBetaCased() { return true; }

  // 2

  public String getBetaCased2() { return "BetaCased2"; }

  public boolean isBetaCased2() { return true; }

  // 3

  public boolean isBetaCased3() { return true; }

  public String getBetaCased3() { return "BetaCased3"; }

}
