
package org.jruby.ext.ffi.jffi;

import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

interface IntParameterConverter {
    int intValue(ThreadContext context, IRubyObject value);
}
