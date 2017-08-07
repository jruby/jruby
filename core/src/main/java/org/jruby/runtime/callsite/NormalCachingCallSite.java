package org.jruby.runtime.callsite;

import org.jruby.RubySymbol;
import org.jruby.internal.runtime.methods.DynamicMethod;
import org.jruby.runtime.CallType;
import org.jruby.runtime.builtin.IRubyObject;

public class NormalCachingCallSite extends CachingCallSite {
    public NormalCachingCallSite(RubySymbol methodName) {
        super(methodName, CallType.NORMAL);
    }

    protected boolean methodMissing(DynamicMethod method, IRubyObject caller) {
        return method.isUndefined() || (!methodName.asJavaString().equals("method_missing") && !method.isCallableFrom(caller, callType));
    }
}
