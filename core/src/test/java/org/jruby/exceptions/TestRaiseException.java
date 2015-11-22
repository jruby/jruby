/*
 * Copyright (c) 2015 JRuby.
 */
package org.jruby.exceptions;

import org.jruby.RubyArray;
import org.jruby.RubyClass;
import org.jruby.RubyException;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.test.TestRubyBase;

public class TestRaiseException extends TestRubyBase {

    public void testBacktrace() {
        IRubyObject ex = runtime.evalScriptlet("ex = nil; " +
            "begin; raise 'with-bracktrace'; rescue => e; ex = e end; ex"
        );
        assertEquals("org.jruby.RubyException", ex.getClass().getName());
        IRubyObject backtrace = ((RubyException) ex).getBacktrace();
        assertNotNil( backtrace );
        assertFalse( ((RubyArray) backtrace).isEmpty() );
    }

    public void testJavaGeneratedBacktrace() {
        final int count = runtime.getBacktraceCount();

        final RubyClass RuntimeError = runtime.getRuntimeError();
        RaiseException re = new RaiseException(runtime, RuntimeError, "", false);
        IRubyObject backtrace = re.getException().backtrace();
        assertNotNil( backtrace );
        assertTrue( ((RubyArray) backtrace).isEmpty() );

        assertEquals( count + 1, runtime.getBacktraceCount() );
    }

    private void assertNotNil(IRubyObject val) {
        assertFalse("expected: " + val.inspect() + " to not be nil", val.isNil());
    }

}
