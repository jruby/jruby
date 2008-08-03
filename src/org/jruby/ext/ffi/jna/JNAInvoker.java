package org.jruby.ext.ffi.jna;

import com.sun.jna.Function;
import org.jruby.Ruby;
import org.jruby.ext.ffi.Invoker;
import org.jruby.runtime.builtin.IRubyObject;

/**
 * A native invoker that uses JNA.
 */
final class JNAInvoker extends Invoker {

    private final Function function;
    private final FunctionInvoker functionInvoker;
    private final Marshaller[] marshallers;

    public JNAInvoker(Function function, FunctionInvoker functionInvoker, Marshaller[] marshallers) {
        super(marshallers.length);
        this.function = function;
        this.functionInvoker = functionInvoker;
        this.marshallers = marshallers;
    }

    public IRubyObject invoke(Ruby runtime, IRubyObject[] rubyArgs) {
        Object[] args = new Object[rubyArgs.length];
        Invocation invocation = new Invocation();
        for (int i = 0; i < args.length; ++i) {
            args[i] = marshallers[i].marshal(invocation, rubyArgs[i]);
        }
        IRubyObject retVal = functionInvoker.invoke(runtime, function, args);
        invocation.finish();
        return retVal;
    }
}
