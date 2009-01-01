
package org.jruby.ext.ffi.jffi;

import com.kenai.jffi.Function;
import org.jruby.RubyModule;
import org.jruby.runtime.Block;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

final class FastIntMethodThreeArg extends FastIntMethod {
    private final IntParameterConverter c1, c2, c3;
    public FastIntMethodThreeArg(RubyModule implementationClass, Function function,
            IntResultConverter resultConverter, IntParameterConverter[] parameterConverters) {
        super(implementationClass, function, resultConverter, parameterConverters);
        this.c1 = parameterConverters[0];
        this.c2 = parameterConverters[1];
        this.c3 = parameterConverters[2];
    }
    private final IRubyObject invoke(ThreadContext context, IRubyObject arg1, 
            IRubyObject arg2, IRubyObject arg3) {
        int retval = invoker.invokeIIIrI(function,
                    c1.intValue(context, arg1),
                    c2.intValue(context, arg2),
                    c3.intValue(context, arg3));
        return resultConverter.fromNative(context, retval);
    }
    @Override
    public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject[] args, Block block) {
        arity.checkArity(context.getRuntime(), args);
        return invoke(context, args[0], args[1], args[2]);
    }

    @Override
    public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule klazz, 
            String name, IRubyObject arg1, IRubyObject arg2, IRubyObject arg3) {
        return invoke(context, arg1, arg2, arg3);
    }
}
