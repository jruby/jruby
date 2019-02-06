package org.jruby.exceptions;

import org.jruby.runtime.builtin.IRubyObject;

/**
 * An unrescuable exception used to propagate a catch's throw.
 */
public class CatchThrowException extends RuntimeException implements Unrescuable {
    public CatchThrowException() {tag = null;}
    public CatchThrowException(IRubyObject tag) {
        this.tag = tag;
    }
    public IRubyObject[] args = IRubyObject.NULL_ARRAY;
    public final IRubyObject tag;

    @Override
    public synchronized Throwable fillInStackTrace() {
        return this;
    }
}
