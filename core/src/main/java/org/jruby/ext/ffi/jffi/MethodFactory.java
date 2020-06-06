
package org.jruby.ext.ffi.jffi;

import com.kenai.jffi.CallingConvention;
import org.jruby.Ruby;
import org.jruby.RubyModule;
import org.jruby.ext.ffi.Type;
import org.jruby.internal.runtime.methods.DynamicMethod;
import org.jruby.runtime.builtin.IRubyObject;

/**
 * Common method factory definitions
 */
public abstract class MethodFactory {
    
    public static final DynamicMethod createDynamicMethod(Ruby runtime, RubyModule module,
            com.kenai.jffi.Function function, Type returnType, Type[] parameterTypes,
            CallingConvention convention, IRubyObject enums, boolean ignoreErrno) {
        
        final MethodFactory[] factories = new MethodFactory[] { 
            DefaultMethodFactory.getFactory()
            
        };
        
        for (MethodFactory f : factories) {
            if (f.isSupported(returnType, parameterTypes, convention)) {
                return f.createMethod(module, function, returnType, parameterTypes, convention, enums, ignoreErrno);
            }
        }
        
        throw runtime.newRuntimeError("cannot create dynamic method");
    }
    
    abstract boolean isSupported(Type returnType, Type[] parameterTypes, 
            CallingConvention convention);
    abstract DynamicMethod createMethod(RubyModule module, com.kenai.jffi.Function function, 
            Type returnType, Type[] parameterTypes, CallingConvention convention, IRubyObject enums, boolean ignoreErrno);
}
