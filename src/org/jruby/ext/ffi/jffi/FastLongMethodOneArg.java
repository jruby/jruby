
package org.jruby.ext.ffi.jffi;

import com.kenai.jffi.Function;
import org.jruby.RubyModule;
import org.jruby.runtime.Block;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

final class FastLongMethodOneArg extends FastLongMethod {
    private final LongParameterConverter c1;
    public FastLongMethodOneArg(RubyModule implementationClass, Function function,
            LongResultConverter resultConverter, LongParameterConverter[] parameterConverters) {
        super(implementationClass, function, resultConverter, parameterConverters);
        this.c1 = parameterConverters[0];
    }
    private final IRubyObject invoke(ThreadContext context, IRubyObject arg1) {
        long retval = invoker.invokeLrL(function, c1.longValue(context, arg1));
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
