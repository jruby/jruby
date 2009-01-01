
package org.jruby.ext.ffi.jffi;

import com.kenai.jffi.Function;
import org.jruby.RubyModule;
import org.jruby.runtime.Block;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

final class FastIntMethodOneArg extends FastIntMethod {
    private final IntParameterConverter c1;
    public FastIntMethodOneArg(RubyModule implementationClass, Function function,
            IntResultConverter resultConverter, IntParameterConverter[] parameterConverters) {
        super(implementationClass, function, resultConverter, parameterConverters);
        this.c1 = parameterConverters[0];
    }
    private final IRubyObject invoke(ThreadContext context, IRubyObject arg1) {
        int retval = invoker.invokeIrI(function, c1.intValue(context, arg1));
        return resultConverter.fromNative(context, retval);
    }
    @Override
    public final IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject[] args, Block block) {
        arity.checkArity(context.getRuntime(), args);
        return invoke(context, args[0]);
    }

    @Override
    public final IRubyObject call(ThreadContext context, IRubyObject self, RubyModule klazz,
            String name, IRubyObject arg1) {
        return invoke(context, arg1);
    }
}
