package org.jruby.ext.ffi.jffi;

import com.kenai.jffi.HeapInvocationBuffer;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

/**
 * Invokes a native function using InvocationBuffer methods.
 */
final class BufferNativeInvoker extends NativeInvoker {
    private final com.kenai.jffi.Function function;
    private final FunctionInvoker functionInvoker;
    private final ParameterMarshaller[] parameterMarshallers;
    private final boolean needsInvocationSession;
    private final int postInvokeCount;
    private final int referenceCount;
    private final HeapInvocationBuffer dummyBuffer;
    
    
    BufferNativeInvoker(com.kenai.jffi.Function function,
            FunctionInvoker functionInvoker,
            ParameterMarshaller[] parameterMarshallers) {

        this.function = function;
        this.functionInvoker = functionInvoker;
        this.parameterMarshallers = (ParameterMarshaller[]) parameterMarshallers.clone();
        
        int piCount = 0;
        int refCount = 0;
        for (ParameterMarshaller m : parameterMarshallers) {
            if (m.requiresPostInvoke()) {
                ++piCount;
            }

            if (m.requiresReference()) {
                ++refCount;
            }
        }
        this.postInvokeCount = piCount;
        this.referenceCount = refCount;
        this.needsInvocationSession = piCount > 0 || refCount > 0;
        this.dummyBuffer = new HeapInvocationBuffer(function);
    }
    
    
    public IRubyObject invoke(ThreadContext context) {
        return functionInvoker.invoke(context, function, dummyBuffer);
    }
    
    public IRubyObject invoke(ThreadContext context, IRubyObject arg1) {
        HeapInvocationBuffer buffer = new HeapInvocationBuffer(function);
        
        if (needsInvocationSession) {
            Invocation invocation = new Invocation(context, postInvokeCount, referenceCount);
            try {
                parameterMarshallers[0].marshal(invocation, buffer, arg1);
                
                return functionInvoker.invoke(context, function, buffer);
            
            } finally {
                invocation.finish();
            }
        
        } else {
            parameterMarshallers[0].marshal(context, buffer, arg1);
            
            return functionInvoker.invoke(context, function, buffer);
        }
    }
    
    public IRubyObject invoke(ThreadContext context, IRubyObject arg1, IRubyObject arg2) {
        HeapInvocationBuffer buffer = new HeapInvocationBuffer(function);
        if (needsInvocationSession) {
            Invocation invocation = new Invocation(context, postInvokeCount, referenceCount);
            try {
                parameterMarshallers[0].marshal(invocation, buffer, arg1);
                parameterMarshallers[1].marshal(invocation, buffer, arg2);
            
                return functionInvoker.invoke(context, function, buffer);
            
            } finally {
                invocation.finish();
            }
        
        } else {
            parameterMarshallers[0].marshal(context, buffer, arg1);
            parameterMarshallers[1].marshal(context, buffer, arg2);
            
            return functionInvoker.invoke(context, function, buffer);
        }
    }
    
    public IRubyObject invoke(ThreadContext context, IRubyObject arg1, 
            IRubyObject arg2, IRubyObject arg3) {
        HeapInvocationBuffer buffer = new HeapInvocationBuffer(function);
        if (needsInvocationSession) {
            Invocation invocation = new Invocation(context, postInvokeCount, referenceCount);
            try {
                parameterMarshallers[0].marshal(invocation, buffer, arg1);
                parameterMarshallers[1].marshal(invocation, buffer, arg2);
                parameterMarshallers[2].marshal(invocation, buffer, arg3);
                
                return functionInvoker.invoke(context, function, buffer);
            } finally {
                invocation.finish();
            }
        
        } else {
            parameterMarshallers[0].marshal(context, buffer, arg1);
            parameterMarshallers[1].marshal(context, buffer, arg2);
            parameterMarshallers[2].marshal(context, buffer, arg3);
        
            return functionInvoker.invoke(context, function, buffer);
        }
    }

    @Override
    public IRubyObject invoke(ThreadContext context, IRubyObject arg1, IRubyObject arg2, IRubyObject arg3, IRubyObject arg4) {
        return invoke(context, new IRubyObject[] { arg1, arg2, arg3, arg4 });
    }

    @Override
    public IRubyObject invoke(ThreadContext context, IRubyObject arg1, IRubyObject arg2, IRubyObject arg3, IRubyObject arg4, IRubyObject arg5) {
        return invoke(context, new IRubyObject[] { arg1, arg2, arg3, arg4, arg5 });
    }

    @Override
    public IRubyObject invoke(ThreadContext context, IRubyObject arg1, IRubyObject arg2, IRubyObject arg3, IRubyObject arg4, IRubyObject arg5, IRubyObject arg6) {
        return invoke(context, new IRubyObject[] { arg1, arg2, arg3, arg4, arg5, arg6 });
    }
    

    public IRubyObject invoke(ThreadContext context, IRubyObject[] args) {
        
        HeapInvocationBuffer buffer = new HeapInvocationBuffer(function);

        if (needsInvocationSession) {
            Invocation invocation = new Invocation(context, postInvokeCount, referenceCount);
            try {
                for (int i = 0; i < args.length; ++i) {
                    parameterMarshallers[i].marshal(invocation, buffer, args[i]);
                }
                return functionInvoker.invoke(context, function, buffer);
            
            } finally {
                invocation.finish();
            }
        
        } else {
            for (int i = 0; i < args.length; ++i) {
                parameterMarshallers[i].marshal(context, buffer, args[i]);
            }

            return functionInvoker.invoke(context, function, buffer);
        }
    }
    
}
