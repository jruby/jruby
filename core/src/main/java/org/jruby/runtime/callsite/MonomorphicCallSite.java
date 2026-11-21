package org.jruby.runtime.callsite;

import org.jruby.runtime.CallType;

/**
 * Single type call site
 *
 * @see CachingCallSite
 */
public class MonomorphicCallSite extends CachingCallSite {
    public MonomorphicCallSite(String methodName) {
        this(methodName, CallType.NORMAL);
    }

    public MonomorphicCallSite(String methodName, CallType callType) {
        super(methodName, callType);
    }
}
