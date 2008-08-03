package org.jruby.ext.ffi.jna;

import com.sun.jna.Function;
import org.jruby.Ruby;
import org.jruby.runtime.builtin.IRubyObject;

/**
 * An interface that is used to invoke a native function with a specific
 * return type, and convert that return type to a ruby object.
 */
interface FunctionInvoker {

    IRubyObject invoke(Ruby runtime, Function function, Object[] args);
}
