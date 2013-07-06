package org.jruby.test;

public class Worker {
  public void run_parent(Parent p) {
    p.run("WORKER");
  }
}
