
package org.jruby.ext.ffi.jffi;

import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

public interface LongResultConverter {
    IRubyObject fromNative(ThreadContext context, long value);
}
