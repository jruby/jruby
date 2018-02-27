/*
 * Copyright (c) 2015 JRuby.
 */
package org.jruby.exceptions;

import org.jruby.RubyArray;
import org.jruby.RubyClass;
import org.jruby.RubyException;
import org.jruby.runtime.backtrace.RubyStackTraceElement;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.test.TestRubyBase;

public class TestRaiseException extends TestRubyBase {

    public void testBacktrace() {
        IRubyObject ex = runtime.evalScriptlet("ex = nil; " +
                        "begin; raise 'with-backtrace'; rescue => e; ex = e end; ex"
        );
        assertEquals("org.jruby.RubyException", ex.getClass().getName());
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
            assert ex.toString().startsWith("org.jruby.exceptions.RaiseException: (StandardError)");

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

    public void testJavaGeneratedBacktrace() {
        final int count = runtime.getBacktraceCount();

        final RubyClass RuntimeError = runtime.getRuntimeError();
        RaiseException re = new RaiseException(runtime, RuntimeError, "");
        IRubyObject backtrace = re.getException().backtrace();
        assertNotNil( backtrace );
        assertTrue( ((RubyArray) backtrace).isEmpty() );

        assertNotSame( RubyStackTraceElement.EMPTY_ARRAY, re.getException().getBacktraceElements() );

        assertEquals( count + 1, runtime.getBacktraceCount() );
    }

    public void testJavaGeneratedNilBacktrace() {
        final int count = runtime.getBacktraceCount();
        IRubyObject backtrace = runtime.getNil();

        final RubyClass RuntimeError = runtime.getRuntimeError();
        RaiseException re = new RaiseException(runtime, RuntimeError, "", backtrace);
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

}
