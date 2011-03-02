package java_integration.fixtures;
// for testing JRUBY-4680

public abstract class ClassWithAbstractMethods {
  // no overload
  public abstract String foo1(Object arg);
  
  public static String callFoo1(ClassWithAbstractMethods cwam, Object arg) {
    return cwam.foo1(arg);
  }
  
  // overloaded with other not abstract
  public abstract String foo2(Object arg);
  public String foo2(Object arg0, Object arg1) {
    return "ok";
  }
  
  public static String callFoo2(ClassWithAbstractMethods cwam, Object arg0, Object arg1) {
    return cwam.foo2(arg0, arg1);
  }
}