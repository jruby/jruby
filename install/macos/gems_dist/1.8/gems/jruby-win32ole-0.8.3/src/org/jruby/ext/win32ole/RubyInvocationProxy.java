package org.jruby.ext.win32ole;

import org.racob.com.InvocationProxy;
import org.racob.com.Variant;
import org.jruby.Ruby;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

/**
 */
public class RubyInvocationProxy extends InvocationProxy {
    private final Ruby runtime;
    private final IRubyObject target;
    
    public RubyInvocationProxy(IRubyObject target) {
        this.target = target;
        this.runtime = target.getRuntime();
    }

    @Override
    public Variant invoke(String methodName, Variant[] variantArgs) {
        ThreadContext context = runtime.getCurrentContext();

        int length = variantArgs.length;
        IRubyObject[] args = new IRubyObject[length];
        for (int i = 0; i < length; i++) {
            args[i] = RubyWIN32OLE.fromVariant(runtime, variantArgs[i]);
        }

        target.callMethod(context, methodName, args);
        return null;
    }

}
