
package org.jruby.ext.ffi.jffi;

import com.kenai.jffi.Function;
import com.kenai.jffi.HeapInvocationBuffer;
import org.jruby.RubyModule;
import org.jruby.runtime.Arity;
import org.jruby.runtime.Block;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

class DefaultMethodZeroArg extends JFFIDynamicMethod {
    private final HeapInvocationBuffer dummyBuffer;

    public DefaultMethodZeroArg(RubyModule implementationClass, Function function, FunctionInvoker functionInvoker) {
        super(implementationClass, Arity.NO_ARGUMENTS, function, functionInvoker);
        dummyBuffer = new HeapInvocationBuffer(function);
    }

    @Override
    public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject[] args, Block block) {
        arity.checkArity(context.getRuntime(), args);
        return functionInvoker.invoke(context.getRuntime(), function, dummyBuffer);
    }

    @Override
    public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule klazz, String name) {
        return functionInvoker.invoke(context.getRuntime(), function, dummyBuffer);
    }

}
