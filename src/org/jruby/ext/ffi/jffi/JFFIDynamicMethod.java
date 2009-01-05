
package org.jruby.ext.ffi.jffi;


import com.kenai.jffi.Function;
import org.jruby.RubyModule;
import org.jruby.internal.runtime.methods.CallConfiguration;
import org.jruby.internal.runtime.methods.DynamicMethod;
import org.jruby.runtime.Arity;
import org.jruby.runtime.Visibility;

abstract class JFFIDynamicMethod extends DynamicMethod {
    protected final Arity arity;
    protected final Function function;
    protected final FunctionInvoker functionInvoker;

    public JFFIDynamicMethod(RubyModule implementationClass, Arity arity, Function function, FunctionInvoker functionInvoker) {
        super(implementationClass, Visibility.PUBLIC, CallConfiguration.FrameNoneScopeNone);
        this.arity = arity;
        this.function = function;
        this.functionInvoker = functionInvoker;
    }
    @Override
    public final DynamicMethod dup() {
        return this;
    }
    @Override
    public final Arity getArity() {
        return arity;
    }
    @Override
    public final boolean isNative() {
        return true;
    }
}
