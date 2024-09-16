package org.jruby.javasupport;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.text.SimpleDateFormat;

import org.jruby.*;
import org.jruby.exceptions.RaiseException;
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
            Java.getProxyClass(runtime, B.class, false);
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
    public void testDuckTypeInterfaceImplGeneratesNoWarning() {
        final String script =
            "class Consumer1\n" +
            "  def self.accept(arg); puts self end\n" +
            "end\n" +
            "class Consumer2 < Consumer1; end\n" +
            "class Consumer3 < Consumer1; end\n" ;

        final ByteArrayOutputStream output = new ByteArrayOutputStream();

        RubyInstanceConfig config = new RubyInstanceConfig();
        config.setOutput(new PrintStream(output));
        config.setError(new PrintStream(output));

        final Ruby runtime = Ruby.newInstance(config);
        runtime.evalScriptlet(script);

        runtime.evalScriptlet(
            "coll = java.util.Collections.singleton(42)\n" +
            "coll.forEach(Consumer1); coll.forEach(Consumer2); coll.forEach(Consumer3)"
        );

        assertEquals("Consumer1\nConsumer2\nConsumer3\n", output.toString()); // no "warning: already initialized constant ..."
    }

    @Test
    public void testGetProxyClass() {
        final ByteArrayOutputStream output = new ByteArrayOutputStream();

        RubyInstanceConfig config = new RubyInstanceConfig();
        config.setOutput(new PrintStream(output));
        config.setError(new PrintStream(output)); // ignore warnings - we'll be replacing internal Java proxy constant

        final Ruby runtime = Ruby.newInstance(config);

        final Object klass = Java.getProxyClass(runtime, java.lang.System.class, true); // Java::JavaLang::System

        final RubyModule dummySystemProxy = RubyModule.newModule(runtime); // replace Java::JavaLang::System with smt different
        Java.setProxyClass(runtime, runtime.getClassFromPath("Java::JavaLang"), "System", dummySystemProxy, true);

        assertSame(klass, Java.getProxyClass(runtime, java.lang.System.class));
        assertSame(klass, Java.getProxyClass(runtime, java.lang.System.class, true));
        assertSame(klass, Java.getProxyClass(runtime, java.lang.System.class, false));

        // asserting here that the Java.addToJavaPackageModule path only executes once and not repeatedly
        assertSame(dummySystemProxy, runtime.getClassFromPath("Java::JavaLang::System"));
    }
}
