/*
 * Copyright (c) 2015 JRuby.
 */
package org.jruby.exceptions;

import org.jruby.Ruby;
import org.jruby.RubyArray;
import org.jruby.RubyClass;
import org.jruby.RubyException;
import org.jruby.RubyObject;
import org.jruby.RubyRuntimeAdapter;
import org.jruby.RubyString;
import org.jruby.anno.JRubyMethod;
import org.jruby.javasupport.JavaEmbedUtils;
import org.jruby.runtime.Block;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.backtrace.RubyStackTraceElement;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.test.Base;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

public class TestRaiseException extends Base {

    public void testBacktrace() {
        IRubyObject ex = runtime.evalScriptlet("ex = nil; " +
                        "begin; raise 'with-backtrace'; rescue => e; ex = e end; ex"
        );
        assertEquals("org.jruby.RubyRuntimeError", ex.getClass().getName());
        assertNotNil(((RubyException) ex).getBacktrace());
        RubyArray backtrace = (RubyArray) (((RubyException) ex).getBacktrace());
        assertFalse(backtrace.isEmpty());
        assert backtrace.get(0).toString().startsWith("<script>:1");

        assertEquals("with-backtrace", ((RubyException) ex).getMessage().asJavaString());
    }

    public void testStackTrace() {
        try {
            runtime.evalScriptlet(
                    "def first; raise StandardError, '' end\n" +
                    "def second(); first() end\n" +
                    "second()"
            );
            fail();
        }
        catch (RaiseException ex) {
            assert ex.toString().startsWith("org.jruby.exceptions.StandardError: (StandardError)");

            StackTraceElement[] stack = ex.getStackTrace();
            assertEquals(3, stack.length);
            assertEquals("first", stack[0].getMethodName());
            assertEquals(1, stack[0].getLineNumber());
            assertEquals("second", stack[1].getMethodName());
            assertEquals(2, stack[1].getLineNumber());
        }
    }

    public void testToString() {
        try {
            runtime.evalScriptlet("def foo(arg); end; foo(1, 2)");
            fail();
        }
        catch (RaiseException ex) {
            // NOTE: probably makes sense for RubyException#toString to
            // "ArgumentError: wrong number of arguments (given 2, expected 1)"
            // instead of just the message (as to_s does)
            assertNotNull( ex.getException().toString() );
        }
    }

    public void testFromWithBacktrace() {
        final int count = runtime.getExceptionCount();

        final IRubyObject backtrace = runtime.newArray();
        RaiseException ex = RaiseException.from(runtime, runtime.getArgumentError(), "testFromWithBacktrace", backtrace);

        assertEquals( count + 1, runtime.getExceptionCount() );

        assertEquals( "(ArgumentError) testFromWithBacktrace", ex.getMessage() );
        assertSame( backtrace, ex.getException().getBacktrace() );
        assertEquals( 0, ex.getException().getBacktraceElements().length );
    }

    public void testFromLegacyOnlyPreRaisesOnce() {
        final int count = runtime.getExceptionCount();

        final IRubyObject ex = runtime.getRuntimeError().newInstance(runtime.getCurrentContext(), Block.NULL_BLOCK);
        RaiseException.from((RubyException) ex, runtime.newArrayLight());

        assertEquals( count + 1, runtime.getExceptionCount() );

        assertEquals( 0, ((RubyException) ex).getBacktraceElements().length );
    }

    public void testFromJavaGeneratedBacktrace() {
        final int count = runtime.getBacktraceCount();

        final RubyClass RuntimeError = runtime.getRuntimeError();
        RaiseException re = RaiseException.from(runtime, RuntimeError, "");
        assertEquals( count + 1, runtime.getBacktraceCount() );

        assertEquals( "(RuntimeError) ", re.getMessage() );
        assertEquals( "", re.getException().getMessageAsJavaString() );
        IRubyObject backtrace = re.getException().backtrace();
        assertNotNil( backtrace );
        assertTrue( ((RubyArray) backtrace).isEmpty() ); // empty backtrace -> as we're not in Ruby land

        assertTrue( re.getException().getBacktraceElements().length == 0 );

        assertEquals( count + 1, runtime.getBacktraceCount() );
    }

    public void testFromExtGeneratedBacktrace() {
        final int count = runtime.getBacktraceCount();
        try {
            Razer.define(runtime);
            runtime.evalScriptlet("[1].each { Razer.new.raise_from }\n");
            fail("not raised");
        } catch (RaiseException re) {
            assertEquals("(ZeroDivisionError) raise_from", re.getLocalizedMessage());

            IRubyObject backtrace = re.getException().backtrace();
            assertNotNil( backtrace );
            assertFalse( ((RubyArray) backtrace).isEmpty() );
            assertTrue(re.getException().getBacktraceElements().length > 2);
            assertEquals("raise_from", re.getException().getBacktraceElements()[0].getMethodName());

            assertEquals(count + 1, runtime.getBacktraceCount());
        }
    }

    public void testFromExtExplicitNilBacktrace() {
        final int count = runtime.getBacktraceCount();
        try {
            Razer.define(runtime);
            runtime.evalScriptlet("[1].each { Razer.new.raise_from_nil }\n");
            fail("not raised");
        } catch (StandardError re) {
            assertEquals("org.jruby.exceptions.ZeroDivisionError: (ZeroDivisionError) raise_from_nil", re.toString());

            IRubyObject backtrace = re.getException().backtrace();
            assertNil( backtrace );

            assertNil( re.getException().getBacktrace() );
            assertSame( RubyStackTraceElement.EMPTY_ARRAY, re.getException().getBacktraceElements() );

            assertEquals( count, runtime.getBacktraceCount() );
        }
    }

    public void testFromJavaExplicitNilBacktrace() {
        final int count = runtime.getBacktraceCount();
        IRubyObject backtrace = runtime.getNil();

        final RubyClass RuntimeError = runtime.getRuntimeError();
        RaiseException re = RaiseException.from(runtime, RuntimeError, "testFromJavaGeneratedNilBacktrace", backtrace);
        assertEquals( "(RuntimeError) testFromJavaGeneratedNilBacktrace", re.getLocalizedMessage() );
        assertEquals( "testFromJavaGeneratedNilBacktrace", re.getException().getMessageAsJavaString() );
        backtrace = re.getException().backtrace();
        assertNil( backtrace );

        assertNil( re.getException().getBacktrace() );
        assertSame( RubyStackTraceElement.EMPTY_ARRAY, re.getException().getBacktraceElements() );

        assertEquals( count, runtime.getBacktraceCount() );
    }

    public void testJavaExceptionTraceIncludesRubyTrace() {
        String script =
                "def one\n" +
                        "  two\n" +
                        "end\n" +
                        "def two\n" +
                        "  raise 'here'\n" +
                        "end\n" +
                        "one\n";
        try {
            eval(script, "test_raise_exception1");
            fail("should have raised an exception");
        }
        catch (RaiseException ex) {
            String trace = printStackTrace(ex);
            // System.out.println(trace);
            assertTrue(trace.indexOf("here") >= 0);
            assertTrue(trace.indexOf("one") >= 0);
            assertTrue(trace.indexOf("two") >= 0);
            assertTrue(trace.indexOf("test_raise_exception1.one(test_raise_exception1:2)") >= 0);
        }
    }

    public void testRubyExceptionTraceIncludesJavaPart() {
        String script =
                "require 'java'\n" +
                        "java_import('org.jruby.test.ThrowFromJava') { |p,c| 'ThrowFromJava' }\n" +
                        "def throw_it; [ ThrowFromJava.new ].each { |tfj| tfj.throwIt } end\n" +
                        "throw_it()\n";
        try {
            eval(script, "test_raise_exception2");
            fail("should have raised an exception");
        }
        catch (RuntimeException ex) {
            assertEquals(RuntimeException.class, ex.getClass());

            final StackTraceElement[] trace = ex.getStackTrace();
            final String fullTrace = printStackTrace(ex); // System.out.println( fullTrace );
            // NOTE: 'unknown' JRuby packages e.g. "org.jruby.test" should not be filtered
            //  ... if they are that hurts stack-traces from extensions such as jruby-rack and jruby-openssl
            assertTrue(trace[0].toString(), trace[0].toString().startsWith("org.jruby.test.ThrowFromJava.throwIt"));

            boolean found_each = false;
            for ( StackTraceElement element : trace ) {
                if ( "each".equals( element.getMethodName() ) && element.getClassName().contains("RubyArray") ) {
                    if ( found_each ) {
                        fail("duplicate " + element + " in : \n" + fullTrace);
                    }
                    found_each = true;
                }
            }
            assertTrue("missing org.jruby.RubyArray.each ... in : \n" + fullTrace, found_each);
        }
        // ~ FULL TRACE (no filtering) :
        //    java.lang.RuntimeException: here
        //    at org.jruby.test.TestRaiseException$ThrowFromJava.throwIt(org/jruby/test/TestRaiseException.java:63)
        //    at sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
        //    at sun.reflect.NativeMethodAccessorImpl.invoke(sun/reflect/NativeMethodAccessorImpl.java:62)
        //    at sun.reflect.DelegatingMethodAccessorImpl.invoke(sun/reflect/DelegatingMethodAccessorImpl.java:43)
        //    at java.lang.reflect.Method.invoke(java/lang/reflect/Method.java:498)
        //    at org.jruby.javasupport.JavaMethod.invokeDirectWithExceptionHandling(org/jruby/javasupport/JavaMethod.java:438)
        //    at org.jruby.javasupport.JavaMethod.invokeDirect(org/jruby/javasupport/JavaMethod.java:302)
        //    at org.jruby.java.invokers.InstanceMethodInvoker.call(org/jruby/java/invokers/InstanceMethodInvoker.java:35)
        //    at org.jruby.runtime.callsite.CachingCallSite.cacheAndCall(org/jruby/runtime/callsite/CachingCallSite.java:293)
        //    at org.jruby.runtime.callsite.CachingCallSite.call(org/jruby/runtime/callsite/CachingCallSite.java:131)
        //    at test_raise_exception2.block in throw_it(test_raise_exception2:3)
        //    at org.jruby.runtime.CompiledIRBlockBody.yieldDirect(org/jruby/runtime/CompiledIRBlockBody.java:156)
        //    at org.jruby.runtime.BlockBody.yield(org/jruby/runtime/BlockBody.java:110)
        //    at org.jruby.runtime.Block.yield(org/jruby/runtime/Block.java:167)
        //    at org.jruby.RubyArray.each(org/jruby/RubyArray.java:1567)
        //    at org.jruby.runtime.callsite.CachingCallSite.cacheAndCall(org/jruby/runtime/callsite/CachingCallSite.java:303)
        //    at org.jruby.runtime.callsite.CachingCallSite.callBlock(org/jruby/runtime/callsite/CachingCallSite.java:141)
        //    at org.jruby.runtime.callsite.CachingCallSite.call(org/jruby/runtime/callsite/CachingCallSite.java:145)
        //    at test_raise_exception2.throw_it(test_raise_exception2:3)
        //    at org.jruby.internal.runtime.methods.CompiledIRMethod.invokeExact(org/jruby/internal/runtime/methods/CompiledIRMethod.java:232)
        //    at org.jruby.internal.runtime.methods.CompiledIRMethod.call(org/jruby/internal/runtime/methods/CompiledIRMethod.java:101)
        //    at org.jruby.internal.runtime.methods.DynamicMethod.call(org/jruby/internal/runtime/methods/DynamicMethod.java:189)
        //    at org.jruby.runtime.callsite.CachingCallSite.cacheAndCall(org/jruby/runtime/callsite/CachingCallSite.java:293)
        //    at org.jruby.runtime.callsite.CachingCallSite.call(org/jruby/runtime/callsite/CachingCallSite.java:131)
        //    at test_raise_exception2.<top>(test_raise_exception2:4)
        //    at java.lang.invoke.MethodHandle.invokeWithArguments(java/lang/invoke/MethodHandle.java:627)
        //    at org.jruby.ir.Compiler$1.load(org/jruby/ir/Compiler.java:111)
        //    at org.jruby.Ruby.runScript(org/jruby/Ruby.java:825)
        //    at org.jruby.Ruby.runScript(org/jruby/Ruby.java:817)
        //    at org.jruby.Ruby.runNormally(org/jruby/Ruby.java:755)
        //    at org.jruby.test.TestRubyBase.eval(org/jruby/test/TestRubyBase.java:84)
        //    at org.jruby.test.TestRaiseException.testRubyExceptionTraceIncludesJavaPart(org/jruby/test/TestRaiseException.java:73)
        //    at sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
        //    at sun.reflect.NativeMethodAccessorImpl.invoke(sun/reflect/NativeMethodAccessorImpl.java:62)
        //    at sun.reflect.DelegatingMethodAccessorImpl.invoke(sun/reflect/DelegatingMethodAccessorImpl.java:43)
        //    at java.lang.reflect.Method.invoke(java/lang/reflect/Method.java:498)
        //    at junit.framework.TestCase.runTest(junit/framework/TestCase.java:176)

        // ~ FILTERING (all org.jruby) ~ 9.0.5.0 :
        //    java.lang.RuntimeException: here
        //    at java.lang.reflect.Method.invoke(java/lang/reflect/Method.java:498)
        //    at test_raise_exception2.block in throw_it(test_raise_exception2:3)
        //    at org.jruby.RubyArray.each(org/jruby/RubyArray.java:1567)
        //    at test_raise_exception2.throw_it(test_raise_exception2:3)
        //    at test_raise_exception2.<top>(test_raise_exception2:4)
        //    at java.lang.invoke.MethodHandle.invokeWithArguments(java/lang/invoke/MethodHandle.java:627)
        //    at java.lang.reflect.Method.invoke(java/lang/reflect/Method.java:498)
        //    at junit.framework.TestCase.runTest(junit/framework/TestCase.java:176)

        // ~ LESS FILTERING in 9.1 :
        //    java.lang.RuntimeException: here
        //    at org.jruby.test.TestRaiseException$ThrowFromJava.throwIt(org/jruby/test/TestRaiseException.java:63)
        //    at java.lang.reflect.Method.invoke(java/lang/reflect/Method.java:498)
        //    at org.jruby.java.invokers.InstanceMethodInvoker.call(org/jruby/java/invokers/InstanceMethodInvoker.java:35)
        //    at test_raise_exception2.block in throw_it(test_raise_exception2:3)
        //    at org.jruby.RubyArray.each(org/jruby/RubyArray.java:1567)
        //    at test_raise_exception2.throw_it(test_raise_exception2:3)
        //    at test_raise_exception2.<top>(test_raise_exception2:4)
        //    at java.lang.invoke.MethodHandle.invokeWithArguments(java/lang/invoke/MethodHandle.java:627)
        //    at org.jruby.Ruby.runScript(org/jruby/Ruby.java:825)
        //    at org.jruby.Ruby.runScript(org/jruby/Ruby.java:817)
        //    at org.jruby.Ruby.runNormally(org/jruby/Ruby.java:755)
        //    at org.jruby.test.TestRubyBase.eval(org/jruby/test/TestRubyBase.java:84)
        //    at org.jruby.test.TestRaiseException.testRubyExceptionTraceIncludesJavaPart(org/jruby/test/TestRaiseException.java:73)
        //    at java.lang.reflect.Method.invoke(java/lang/reflect/Method.java:498)
        //    at junit.framework.TestCase.runTest(junit/framework/TestCase.java:176)
    }

    public void testRubyExceptionBacktraceIncludesJavaOrigin() {
        String script =
                "require 'java'\n" +
                        "hash = Hash.new { org.jruby.test.ThrowFromJava.new.throwIt }\n" +
                        "begin; hash['missing']; rescue java.lang.Exception => e; $ex_trace = e.backtrace end \n" +
                        "$ex_trace" ;

        RubyArray trace = (RubyArray) runtime.evalScriptlet(script);

        String fullTrace = trace.join(runtime.getCurrentContext(), runtime.newString("\n")).toString();
        // System.out.println(fullTrace);

        // NOTE: 'unknown' JRuby packages e.g. "org.jruby.test" should not be filtered
        //  ... if they are that hurts stack-traces from extensions such as jruby-rack and jruby-openssl
        assertTrue(trace.get(0).toString(), trace.get(0).toString().startsWith("org.jruby.test.ThrowFromJava.throwIt"));

        boolean hash_default = false;
        for ( Object element : trace ) {
            if ( element.toString().contains("org.jruby.RubyHash.default")) {
                if ( hash_default ) fail("duplicate " + element + " in : \n" + fullTrace);
                hash_default = true;
            }
        }
        assertTrue("missing org.jruby.RubyHash.default ... in : \n" + fullTrace, hash_default);
    }

    public void testRubyExceptionUsingEmbedAdapter() {
        try {
            RubyRuntimeAdapter evaler = JavaEmbedUtils.newRuntimeAdapter();

            evaler.eval(runtime, "no_method_with_this_name");
            fail("expected to throw");
        } catch (RaiseException re) {
            assertEquals("(NameError) undefined local variable or method `no_method_with_this_name' for main:Object", re.getMessage());
        }
    }

    public void testRaiseExceptionWithoutCause() {
        try {
            runtime.evalScriptlet("raise RuntimeError, 'foo'");
            fail("expected to throw");
        } catch (RaiseException re) {
            assertTrue(re.getMessage().contains("foo"));

            assertNull(re.getCause());
            assertFalse(printStackTrace(re).contains("Caused by:"));
        }
    }

    public void testManuallySetExceptionCause() {
        try {
            runtime.evalScriptlet("raise RuntimeError, 'foo'");
            fail("expected to throw");
        } catch (RaiseException re) {
            assertTrue(re.getMessage().contains("foo"));
            re.initCause(new IllegalArgumentException("TEST-CAUSE"));

            assertNotNull(re.getCause());
            assertEquals("TEST-CAUSE", re.getCause().getMessage());
            assertTrue(printStackTrace(re).contains("Caused by:"));
        }
    }

    public void testRubyExceptionCause() {
        try {
            runtime.evalScriptlet("begin; raise NameError, 'foo'; rescue => e; raise 'bar'; end");
            fail("expected to throw");
        } catch (RaiseException re) {
            assertTrue(re.getMessage().contains("bar"));

            assertTrue(printStackTrace(re).contains("Caused by:"));
            assertNotNull(re.getCause());
            assertTrue(re.getCause() instanceof org.jruby.exceptions.NameError);
        }
    }

    public void testJavaExceptionCause() {
        try {
            runtime.evalScriptlet("begin; raise java.io.IOException.new('foo'); rescue => e; raise 'bar'; end");
            fail("expected to throw");
        } catch (RaiseException re) {
            assertTrue(re.getMessage().contains("bar"));

            assertNotNull(re.getCause());
            assertTrue(re.getCause() instanceof java.io.IOException);
            assertEquals("foo", re.getCause().getMessage());
        }
    }

    public void testOverrideSetExceptionCause() {
        try {
            runtime.evalScriptlet("raise 'foo'");
            fail("expected to throw");
        } catch (RaiseException re) {
            assertTrue(re.getMessage().contains("foo"));
            re.initCause(new IllegalArgumentException("TEST-CAUSE"));

            assertNotNull(re.getCause());
            assertEquals("TEST-CAUSE", re.getCause().getMessage());
            assertTrue(printStackTrace(re).contains("Caused by:"));
        }
    }

    public void testOverrideExceptionCause() {
        try {
            runtime.evalScriptlet("begin; raise NameError, 'foo'; rescue => e; raise 'bar'; end");
            fail("expected to throw");
        } catch (RaiseException re) {
            assertTrue(re.getMessage().contains("bar"));
            re.initCause(new IllegalArgumentException("TEST-CAUSE"));

            assertTrue(printStackTrace(re).contains("Caused by: java.lang.IllegalArgumentException"));
            assertTrue(re.getCause() instanceof IllegalArgumentException);
        }
    }

    private static String printStackTrace(final Throwable ex) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ex.printStackTrace(new PrintStream(baos));
        return baos.toString();
    }

    private void assertNil(IRubyObject val) {
        assertTrue("expected: " + val.inspect() + " to be nil", val.isNil());
    }

    private void assertNotNil(IRubyObject val) {
        assertFalse("expected: " + val.inspect() + " to not be nil", val.isNil());
    }

    public static class Razer extends RubyObject {

        Razer(Ruby runtime, RubyClass klass) {
            super(runtime, klass);
        }

        static RubyClass define(Ruby runtime) {
            RubyClass klass = runtime.defineClass("Razer", runtime.getObject(), Razer::new);
            klass.defineAnnotatedMethods(Razer.class);
            return klass;
        }

        @JRubyMethod
        public IRubyObject raise_from(ThreadContext context) {
            throw RaiseException.from(context.runtime, context.runtime.getZeroDivisionError(), "raise_from");
        }

        @JRubyMethod
        public IRubyObject raise_from_nil(ThreadContext context) {
            throw RaiseException.from(context.runtime, context.runtime.getZeroDivisionError(), "raise_from_nil", context.nil);
        }
    }

    public void testNewRaiseException() {
        Ruby ruby = Ruby.newInstance();

        try {
            throw ruby.newRaiseException(ruby.getException(), "blah");
        } catch (Exception e) {
            assertEquals("(Exception) blah", e.getMessage());
        }

        try {
            throw ruby.newRaiseException(ruby.getException(), "blah");
        } catch (Exception e) {
            assertEquals("(Exception) blah", e.getMessage());
        }

        RubyArray backtrace = ruby.newEmptyArray();
        try {
            throw RaiseException.from(ruby, ruby.getException(), "blah", backtrace);
        } catch (Exception e) {
            assertEquals("(Exception) blah", e.getMessage());
            assertEquals(backtrace, e.getException().backtrace());
        }

        RubyString message = ruby.newString("blah");
        try {
            throw RaiseException.from(ruby, ruby.getException(), message);
        } catch (Exception e) {
            assertEquals("(Exception) blah", e.getMessage());
            assertEquals(message, e.getException().message(ruby.getCurrentContext()));
        }

        try {
            throw RaiseException.from(ruby, "Exception", "blah");
        } catch (Exception e) {
            assertEquals("(Exception) blah", e.getMessage());
        }

        try {
            throw RaiseException.from(ruby, "Exception", "blah", backtrace);
        } catch (Exception e) {
            assertEquals("(Exception) blah", e.getMessage());
            assertEquals(backtrace, e.getException().backtrace());
        }

        try {
            throw RaiseException.from(ruby, "Exception", message);
        } catch (Exception e) {
            assertEquals("(Exception) blah", e.getMessage());
            assertEquals(message, e.getException().message(ruby.getCurrentContext()));
        }
    }
}
