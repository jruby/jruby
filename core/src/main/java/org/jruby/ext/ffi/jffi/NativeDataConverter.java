package org.jruby.ext.ffi.jffi;

import org.jruby.ext.ffi.NativeType;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

abstract public class NativeDataConverter {
    private final boolean referenceRequired;
    private final boolean postInvokeRequired;

    public NativeDataConverter() {
        this.referenceRequired = false;
        this.postInvokeRequired = false;
    }
    
    public NativeDataConverter(boolean referenceRequired, boolean postInvokeRequired) {
        this.referenceRequired = referenceRequired;
        this.postInvokeRequired = postInvokeRequired;
    }

    
    public final boolean isReferenceRequired() {
        return referenceRequired;
    }

    public final boolean isPostInvokeRequired() {
        return postInvokeRequired;
    }
    
    abstract public IRubyObject fromNative(ThreadContext context, IRubyObject obj);
    abstract public IRubyObject toNative(ThreadContext context, IRubyObject obj);
    abstract public NativeType nativeType();
}
