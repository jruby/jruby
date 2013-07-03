package org.jruby.javasupport;

import junit.framework.TestCase;

import org.jruby.Ruby;


public class TestJavaClass extends TestCase {

    public void test() {
        Ruby runtime = Ruby.newInstance();
        // This is now needed, since module Java
        // isn't in by default
        runtime.getModule("Kernel").callMethod(runtime.getCurrentContext(),"require",runtime.newString("java"));
        JavaClass javaClass = JavaClass.get(runtime, String.class);
        assertSame(javaClass, JavaClass.get(runtime, String.class));
    }
}
