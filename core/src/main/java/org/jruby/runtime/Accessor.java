package org.jruby.runtime;

import org.jruby.runtime.builtin.IRubyObject;

/**
 * Newer version of IAccessor which is both context aware AND
 * it allows it to just reuse get/set on the variables it
 * tends to represent.
 */
public interface Accessor {
    IRubyObject get(ThreadContext context);
    IRubyObject set(ThreadContext context, IRubyObject newValue);
    default void force(ThreadContext context, IRubyObject newValue) {
        set(context, newValue);
    }
}
