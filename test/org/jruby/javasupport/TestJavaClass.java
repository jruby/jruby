package org.jruby.javasupport;

import junit.framework.TestCase;

import org.jruby.Ruby;


public class TestJavaClass extends TestCase {

    public void test() {
        Ruby runtime = Ruby.getDefaultInstance();
        JavaClass javaClass = JavaClass.get(runtime, String.class);
        assertSame(javaClass, JavaClass.get(runtime, String.class));
    }
}
