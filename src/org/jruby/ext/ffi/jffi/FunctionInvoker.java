package org.jruby.ext.ffi.jffi;


import com.kenai.jffi.Function;
import com.kenai.jffi.HeapInvocationBuffer;
import org.jruby.Ruby;
import org.jruby.runtime.builtin.IRubyObject;

/**
 * An interface that is used to invoke a native function with a specific
 * return type, and convert that return type to a ruby object.
 */
interface FunctionInvoker {

    IRubyObject invoke(Ruby runtime, Function function, HeapInvocationBuffer args);
}
