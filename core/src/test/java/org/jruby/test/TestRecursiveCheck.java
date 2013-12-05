package org.jruby.test;

import junit.framework.TestCase;
import org.jruby.CompatVersion;

import org.jruby.Ruby;
import org.jruby.RubyInstanceConfig;

public class TestRecursiveCheck extends TestCase {
    private Ruby runtime;

    public TestRecursiveCheck(String name) {
        super(name);
    }

    public void setUp() {
        runtime = Ruby.newInstance();
    }

    public void testWorksFromMultipleThreads() throws Exception {
        Thread thread = new Thread(new Runnable() {
            public void run() {
                assertNotNull(runtime.evalScriptlet("[].hash").convertToInteger());
            }
        });
        thread.start();
        thread.join();
    }
}
