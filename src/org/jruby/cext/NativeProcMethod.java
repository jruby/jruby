package org.jruby.cext;

import org.jruby.RubyArray;
import org.jruby.RubyModule;
import org.jruby.runtime.Block;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

public class NativeProcMethod extends AbstractNativeMethod {

    public NativeProcMethod(RubyModule clazz, long function) {
        super(clazz, -1, function);
    }
    
    @Override
    public IRubyObject call(ThreadContext context, IRubyObject recv, RubyModule clazz,
            String name, IRubyObject[] args) {
        pre(context, recv, clazz, name);
        try {
            return getNativeInstance().callProcMethod(function, Handle.nativeHandle(RubyArray.newArray(context.getRuntime(), args)));
        } finally {
            post(context);
        }
    }

    @Override
    public IRubyObject call(ThreadContext context, IRubyObject recv, RubyModule clazz,
            String name, IRubyObject[] args, Block block) {

        pre(context, recv, clazz, name, block);
        try {
            return getNativeInstance().callProcMethod(function, Handle.nativeHandle(RubyArray.newArray(context.getRuntime(), args)));
        } finally {
            post(context);
        }
    }

}
