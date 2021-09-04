package java_integration.fixtures;
public class InterfaceWrapper {
  public static Runnable giveMeBack(Runnable runnable) {
    return runnable;
  }
}
