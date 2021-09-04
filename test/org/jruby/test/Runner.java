package org.jruby.test;

public class Runner {

  private static Runner runner;
  public static Runner getRunner() { 
    if (runner == null) { runner = new Runner(); }
    return runner; 
  }

  private boolean running;

  public synchronized void runJob(Runnable r) { 
    running = true;
    r.run(); 
    running = false;
  }

  public boolean isRunning() {
    return running;
  }

} 
