
package org.jruby.ext.ffi.jffi;

import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

interface LongParameterConverter {
    long longValue(ThreadContext context, IRubyObject value);
}
