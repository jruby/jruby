
package org.jruby.ext.ffi.jffi;

import com.kenai.jffi.Function;
import org.jruby.RubyModule;
import org.jruby.runtime.Block;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

final class FastIntMethodZeroArg extends FastIntMethod {
    public FastIntMethodZeroArg(RubyModule implementationClass, Function function,
            IntResultConverter resultConverter, IntParameterConverter[] parameterConverters) {
        super(implementationClass, function, resultConverter, parameterConverters);
        
    }

    @Override
    public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject[] args, Block block) {
        arity.checkArity(context.getRuntime(), args);
        return resultConverter.fromNative(context, invoker.invokeVrI(function));
    }

    @Override
    public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule klazz, String name) {
        return resultConverter.fromNative(context, invoker.invokeVrI(function));
    }
}
