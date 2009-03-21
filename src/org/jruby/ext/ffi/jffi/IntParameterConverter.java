
package org.jruby.ext.ffi.jffi;

import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

interface IntParameterConverter {
    boolean isConvertible(ThreadContext context, IRubyObject value);
    int intValue(ThreadContext context, IRubyObject value);
}
