package java_integration.fixtures;

public class PackageInstanceMethod {
  String thePackageScopeMethod() {
    return "42";
  }
  public void voidMethod() {
  }
  public void invokeVoidMethod() {
    voidMethod();
  }
}