package org.jruby.javasupport;

import org.junit.Test;

import org.jruby.Ruby;

public class TestJavaClass extends junit.framework.TestCase {

    @Test
    public void testGet() {
        Ruby runtime = Ruby.newInstance();
        // This is now needed, since module Java
        // isn't in by default
        requireJava(runtime);

        JavaClass javaClass = JavaClass.get(runtime, String.class);
        assertSame(javaClass, JavaClass.get(runtime, String.class));
    }

    static void requireJava(final Ruby runtime) {
        runtime.getModule("Kernel").callMethod(runtime.getCurrentContext(), "require", runtime.newString("java"));
    }

}
