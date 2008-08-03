package org.jruby.ext.ffi.jna;

import org.jruby.runtime.builtin.IRubyObject;

/**
 * Converts a ruby parameter into a native argument.
 */
interface Marshaller {

    Object marshal(Invocation invocation, IRubyObject parameter);
}
