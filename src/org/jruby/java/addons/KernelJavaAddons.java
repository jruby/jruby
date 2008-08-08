package org.jruby.java.addons;

import org.jruby.javasupport.*;
import org.jruby.RubyKernel;
import org.jruby.anno.JRubyMethod;
import org.jruby.runtime.Block;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.Visibility;
import org.jruby.runtime.builtin.IRubyObject;

public class KernelJavaAddons {
    @JRubyMethod(name = "raise", optional = 3, frame = true, module = true, visibility = Visibility.PRIVATE)
    public static IRubyObject rbRaise(
            ThreadContext context, IRubyObject recv, IRubyObject[] args, Block block) throws Throwable {
        
        if (args.length == 1 && args[0].dataGetStruct() instanceof JavaObject) {
            // looks like someone's trying to raise a Java exception. Let them.
            Object maybeThrowable = ((JavaObject)args[0].dataGetStruct()).getValue();
            
            if (maybeThrowable instanceof Throwable) {
                throw (Throwable)maybeThrowable;
            } else {
                throw context.getRuntime().newTypeError("can't raise a non-Throwable Java object");
            }
        } else {
            return RubyKernel.raise(context, recv, args, block);
        }
    }
}
