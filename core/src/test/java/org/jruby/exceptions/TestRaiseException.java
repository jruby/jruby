/*
 * Copyright (c) 2015 JRuby.
 */
package org.jruby.exceptions;

import org.jruby.Ruby;
import org.jruby.RubyArray;
import org.jruby.RubyClass;
import org.jruby.RubyException;
import org.jruby.RubyObject;
import org.jruby.anno.JRubyMethod;
import org.jruby.runtime.Block;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.backtrace.RubyStackTraceElement;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.test.TestRubyBase;

public class TestRaiseException extends TestRubyBase {

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
}
