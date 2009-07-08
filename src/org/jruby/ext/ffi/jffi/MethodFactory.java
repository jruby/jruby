
package org.jruby.ext.ffi.jffi;

import com.kenai.jffi.CallingConvention;
import org.jruby.Ruby;
import org.jruby.RubyModule;
import org.jruby.ext.ffi.Type;
import org.jruby.internal.runtime.methods.DynamicMethod;

/**
 * Common method factory definitions
 */
public abstract class MethodFactory {
    
    public static final DynamicMethod createDynamicMethod(Ruby runtime, RubyModule module,
            com.kenai.jffi.Function function, Type returnType, Type[] parameterTypes,
            CallingConvention convention) {
        DynamicMethod dm;
        if (convention == CallingConvention.DEFAULT
            && FastIntMethodFactory.getFactory().isFastIntMethod(returnType, parameterTypes)) {
            dm = FastIntMethodFactory.getFactory().createMethod(module,
                    function, returnType, parameterTypes);
        } else if (convention == CallingConvention.DEFAULT
            && FastLongMethodFactory.getFactory().isFastLongMethod(returnType, parameterTypes)) {
            dm = FastLongMethodFactory.getFactory().createMethod(module,
                    function, returnType, parameterTypes);
        } else {
            dm = DefaultMethodFactory.getFactory().createMethod(module,
                    function, returnType, parameterTypes, convention, runtime.getNil());
        }
    
        return dm;
    }
}
