package org.jruby.test;

import junit.framework.TestCase;



public class FailingTest extends TestCase {
    private final String message;

    public FailingTest(String name, String message) {
        super(name);
        this.message = message;
    }

    public void runTest() throws Throwable {
        fail(message);
    }
}