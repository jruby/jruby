
package org.jruby.ext.ffi.jffi;

import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

/**
 *
 */
abstract public class NativeInvoker {
    public abstract IRubyObject invoke(ThreadContext context);
    
    public abstract IRubyObject invoke(ThreadContext context, 
            IRubyObject arg1);
    
    public abstract IRubyObject invoke(ThreadContext context, 
            IRubyObject arg1, IRubyObject arg2);
    
    public abstract IRubyObject invoke(ThreadContext context, 
            IRubyObject arg1, IRubyObject arg2, IRubyObject arg3);
    
    public abstract IRubyObject invoke(ThreadContext context, 
            IRubyObject arg1, IRubyObject arg2, IRubyObject arg3,
            IRubyObject arg4);
    
    public abstract IRubyObject invoke(ThreadContext context, 
            IRubyObject arg1, IRubyObject arg2, IRubyObject arg3,
            IRubyObject arg4, IRubyObject arg5);
    
    public abstract IRubyObject invoke(ThreadContext context, 
            IRubyObject arg1, IRubyObject arg2, IRubyObject arg3,
            IRubyObject arg4, IRubyObject arg5, IRubyObject arg6);
    
    public abstract IRubyObject invoke(ThreadContext context, IRubyObject[] args);
}
