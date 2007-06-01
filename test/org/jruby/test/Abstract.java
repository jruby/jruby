package org.jruby.test;

public abstract class Abstract {
  public String call_protected() {
    return protected_method();
  }

  abstract protected String protected_method();
}