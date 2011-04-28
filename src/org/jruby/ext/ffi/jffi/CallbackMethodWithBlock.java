
package org.jruby.ext.ffi.jffi;

import com.kenai.jffi.Function;
import org.jruby.RubyModule;
import org.jruby.ext.ffi.CallbackInfo;
import org.jruby.runtime.Arity;
import org.jruby.runtime.Block;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

/**
 * A DynamicMethod that has a callback argument as the last parameter.  It will
 * treat any block as the last argument.
 */
final class CallbackMethodWithBlock extends DefaultMethod {
    private final int cbindex;
    
    public CallbackMethodWithBlock(RubyModule implementationClass, Function function, 
            FunctionInvoker functionInvoker, ParameterMarshaller[] marshallers, Signature signature, int cbindex) {
        super(implementationClass, function, functionInvoker, marshallers, signature);
        this.cbindex = cbindex;
    }

    @Override
    public IRubyObject call(ThreadContext context, IRubyObject self,
            RubyModule clazz, String name, IRubyObject[] args, Block block) {
        
        if (!block.isGiven()) {
            arity.checkArity(context.getRuntime(), args);
            return getNativeInvoker().invoke(context, args);
        
        } else {
            Arity.checkArgumentCount(context.getRuntime(), args, 
                    arity.getValue() - 1, arity.getValue());
            
            IRubyObject[] params = new IRubyObject[arity.getValue()];
            for (int i = 0; i < cbindex; i++) {
                params[i] = args[i];
            }
            
            params[cbindex] = CallbackManager.getInstance().getCallback(context.getRuntime(), 
                    (CallbackInfo) signature.getParameterType(cbindex), block);
            
            for (int i = cbindex + 1; i < params.length; i++) {
                params[i] = args[i - 1];
            }
            
            return getNativeInvoker().invoke(context, params);
        }
    }
}
