
package org.jruby.ext.ffi.jffi;

import com.kenai.jffi.InvocationBuffer;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

/**
 * Converts a ruby parameter into a native argument.
 */
interface ParameterMarshaller {
    public void marshal(Invocation invocation, InvocationBuffer buffer, IRubyObject value);
    public void marshal(ThreadContext context, InvocationBuffer buffer, IRubyObject value);
    public boolean needsInvocationSession();
}
