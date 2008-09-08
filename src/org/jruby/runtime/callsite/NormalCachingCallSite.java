package org.jruby.runtime.callsite;

import org.jruby.internal.runtime.methods.DynamicMethod;
import org.jruby.runtime.CallType;
import org.jruby.runtime.builtin.IRubyObject;

public class NormalCachingCallSite extends CachingCallSite {
    public NormalCachingCallSite(String methodName) {
        super(methodName, CallType.NORMAL);
        totalCallSites++;
    }

    protected boolean methodMissing(DynamicMethod method, IRubyObject caller) {
        return method.isUndefined() || (!methodName.equals("method_missing") && !method.isCallableFrom(caller, callType));
    }
}
