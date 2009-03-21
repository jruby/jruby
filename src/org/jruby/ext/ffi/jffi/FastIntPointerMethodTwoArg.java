
package org.jruby.ext.ffi.jffi;

import com.kenai.jffi.Function;
import com.kenai.jffi.HeapInvocationBuffer;
import org.jruby.RubyModule;
import org.jruby.runtime.Block;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

final class FastIntPointerMethodTwoArg extends FastIntMethod {
    private final IntParameterConverter c1, c2;
    private final ParameterMarshaller m1, m2;

    public FastIntPointerMethodTwoArg(RubyModule implementationClass, Function function,
            IntResultConverter intResultConverter, IntParameterConverter[] intParameterConverters,
            ParameterMarshaller[] marshallers) {

        super(implementationClass, function, intResultConverter, intParameterConverters);
        this.c1 = intParameterConverters[0];
        this.c2 = intParameterConverters[1];
        this.m1 = marshallers[0];
        this.m2 = marshallers[1];
    }

    private final IRubyObject invoke(ThreadContext context, IRubyObject arg1, IRubyObject arg2) {
        if (c1.isConvertible(context, arg1) && c2.isConvertible(context, arg2)) {
            int retval = invoker.invokeIIrI(function, c1.intValue(context, arg1),
                    c2.intValue(context, arg2));
            return resultConverter.fromNative(context, retval);
        } else {
            HeapInvocationBuffer buffer = new HeapInvocationBuffer(function);
            m1.marshal(context, buffer, arg1);
            m2.marshal(context, buffer, arg2);
            return resultConverter.fromNative(context, invoker.invokeInt(function, buffer));
        }
    }

    @Override
    public final IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject[] args, Block block) {
        arity.checkArity(context.getRuntime(), args);
        return invoke(context, args[0], args[1]);
    }

    @Override
    public final IRubyObject call(ThreadContext context, IRubyObject self, RubyModule klazz,
            String name, IRubyObject arg1, IRubyObject arg2) {
        return invoke(context, arg1, arg2);
    }
}
