package java_integration.fixtures;

import org.jruby.java.codegen.Reified;

public class Service implements Reified {
  public String getName() {
    return "ServiceName";
  }
}
