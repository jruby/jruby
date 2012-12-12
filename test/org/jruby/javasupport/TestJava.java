package org.jruby.javasupport;

import junit.framework.TestCase;

import org.jruby.Ruby;

class A {
    public static class C extends B {}
}

class B extends A {}


public class TestJava extends TestCase {

    public void testProxyCreation() {
        Ruby runtime = Ruby.newInstance();
        try {
            Java.getProxyClass(runtime, B.class);
            assert(true);
        }
        catch (AssertionError ae) {
            fail(ae.toString());
        }
    }
}
