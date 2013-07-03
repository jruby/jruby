package org.jruby.runtime.callsite;

import org.jruby.internal.runtime.methods.DynamicMethod;
import org.jruby.runtime.CallType;
import org.jruby.runtime.builtin.IRubyObject;

public class FunctionalCachingCallSite extends CachingCallSite {
    public FunctionalCachingCallSite(String methodName) {
        super(methodName, CallType.FUNCTIONAL);
        totalCallSites++;
    }

    protected boolean methodMissing(DynamicMethod method, IRubyObject caller) {
        return method.isUndefined();
    }
}
