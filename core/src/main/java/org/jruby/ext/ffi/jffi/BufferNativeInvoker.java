package org.jruby.ext.ffi.jffi;

import com.kenai.jffi.HeapInvocationBuffer;
import org.jruby.RubyModule;
import org.jruby.runtime.Arity;
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
    
    
    BufferNativeInvoker(RubyModule implementationClass, com.kenai.jffi.Function function, Signature signature,
            FunctionInvoker functionInvoker,
            ParameterMarshaller[] parameterMarshallers) {
        super(implementationClass, function, signature);
        this.function = function;
        this.functionInvoker = functionInvoker;
        this.parameterMarshallers = parameterMarshallers.clone();
        
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
    
    
    public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule klazz, String name) {
        return functionInvoker.invoke(context, function, dummyBuffer);
    }
    
    public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule klazz, String name, IRubyObject arg1) {
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
    
    public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule klazz, String name,
                            IRubyObject arg1, IRubyObject arg2) {
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
    
    public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule klazz, String name,
                            IRubyObject arg1, IRubyObject arg2, IRubyObject arg3) {
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


    public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule klazz, String name, IRubyObject[] args) {
        
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
