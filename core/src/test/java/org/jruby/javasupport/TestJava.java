package org.jruby.javasupport;

import java.lang.reflect.Method;
import java.text.SimpleDateFormat;

import org.jruby.*;
import org.jruby.exceptions.RaiseException;
import org.jruby.javasupport.test.HashBase;
import org.jruby.runtime.ThreadContext;
import org.junit.Test;

import org.jruby.test.ThrowingConstructor;

class A {
    public static class C extends B {}
}
class B extends A { }

public class TestJava extends junit.framework.TestCase {

    @Test
    public void testProxyCreation() {
        final Ruby runtime = Ruby.newInstance();
        try {
            Java.getProxyClass(runtime.getCurrentContext(), B.class);
            assert(true);
        }
        catch (AssertionError ae) {
            fail(ae.toString());
        }
    }

    @Test
    public void testGetFunctionInterface() {
        Method method;
        method = Java.getFunctionalInterfaceMethod(java.lang.Runnable.class);
        assertNotNull(method);
        assertEquals("run", method.getName());

        method = Java.getFunctionalInterfaceMethod(java.io.Serializable.class);
        assertNull(method);

        //if ( Java.JAVA8 ) { // compare and equals both abstract
        method = Java.getFunctionalInterfaceMethod(java.util.Comparator.class);
        assertNotNull(method);
        assertEquals("compare", method.getName());
        //}

        method = Java.getFunctionalInterfaceMethod(java.lang.Comparable.class);
        assertNotNull(method);
        assertEquals("compareTo", method.getName());

        method = Java.getFunctionalInterfaceMethod(java.lang.Iterable.class);
        assertNotNull(method);
        assertEquals("iterator", method.getName());

        method = Java.getFunctionalInterfaceMethod(java.util.concurrent.ThreadFactory.class);
        assertNotNull(method);

        method = Java.getFunctionalInterfaceMethod(java.util.Enumeration.class);
        assertNull(method);

        method = Java.getFunctionalInterfaceMethod(FxRunnable1.class);
        assertNotNull(method);
        assertEquals("run", method.getName());

        method = Java.getFunctionalInterfaceMethod(FxRunnable2.class);
        assertNotNull(method);
        assertEquals("run", method.getName());

        method = Java.getFunctionalInterfaceMethod(NonFxRunnable.class);
        assertNull(method);
    }

    private static interface FxRunnable1 extends Runnable { abstract void run() ; }

    private static interface FxRunnable2 extends Runnable { /* inherited run() */ }

    private static interface NonFxRunnable extends Runnable { public void doRun() ; }

    @Test
    public void testJavaConstructorExceptionHandling() throws Exception {
        final Ruby runtime = Ruby.newInstance();
        ThreadContext context = runtime.getCurrentContext();
        JavaConstructor constructor = JavaConstructor.create(runtime,
                ThrowingConstructor.class.getDeclaredConstructor(Integer.class)
        );

        assertNotNull(constructor.newInstanceDirect(context, new Object[] { 1 }));

        try {
            constructor.newInstanceDirect(context, new Object[0]);
            assertTrue("raise exception expected", false);
        }
        catch (RaiseException ex) {
            assertEquals("(ArgumentError) wrong number of arguments (given 0, expected 1)", ex.getMessage());
            assertNull(ex.getCause());
            assertNotNull(ex.getException());
            assertEquals("wrong number of arguments (given 0, expected 1)", ex.getException().getMessage().toString());
        }

        try {
            constructor.newInstanceDirect(context, new Object[] { -1 });
            assertTrue("raise exception expected", false);
        }
        catch (IllegalStateException ex) {
            assertEquals("param == -1", ex.getMessage());
            StackTraceElement e0 = ex.getStackTrace()[0];
            assertEquals("org.jruby.test.ThrowingConstructor", e0.getClassName());
            assertEquals("<init>", e0.getMethodName());
        }

        try {
            constructor.newInstanceDirect(context, new Object[] { null }); // new IllegalStateException() null cause message
            assertTrue("raise exception expected", false);
        }
        catch (IllegalStateException ex) {
            assertEquals(null, ex.getMessage());
        }
    }

    @Test
    public void testOverrideNewOnConcreteJavaProxySubClassRegression() {
        String script =
            "class FormatImpl < java.text.SimpleDateFormat\n" +
            "  include Enumerable\n" +
            "  public_class_method :new\n" +
            "  class << self\n" +
            "    def new(thread_provider: true)\n" +
            "      super()\n" +
            "    end\n" +
            "  end\n" +
            "end\n";

        final Ruby runtime = Ruby.newInstance();
        runtime.evalScriptlet(script);

        assertNotNull(runtime.evalScriptlet("FormatImpl.new")); // used to cause an infinite loop
        assertTrue(runtime.evalScriptlet("FormatImpl.new").toJava(Object.class) instanceof SimpleDateFormat);
    }

    @Test
    public void testHashMethodOnJavaProxy() {
        final Ruby runtime = Ruby.newInstance();
        // we internally shuffle around the subclasses (due interface module includes in the Java class) in a map,
        // calling hashCode -> HashBase.hash() would: (ArgumentError) wrong number of arguments (given 0, expected 1)
        var hashBase = Java.getProxyClass(runtime.getCurrentContext(), HashBase.class);
        assertEquals(hashBase.id, hashBase.hashCode());
    }
}
