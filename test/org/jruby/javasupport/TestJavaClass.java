package org.jruby.javasupport;

import junit.framework.TestCase;

import org.jruby.IRuby;
import org.jruby.Ruby;


public class TestJavaClass extends TestCase {

    public void test() {
        IRuby runtime = Ruby.getDefaultInstance();
        // This is now needed, since module Java
        // isn't in by default
        runtime.getModule("Kernel").callMethod(runtime.getCurrentContext(),"require",runtime.newSymbol("java"));
        JavaClass javaClass = JavaClass.get(runtime, String.class);
        assertSame(javaClass, JavaClass.get(runtime, String.class));
    }
}
