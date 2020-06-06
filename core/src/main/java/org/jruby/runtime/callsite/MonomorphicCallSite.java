package org.jruby.runtime.callsite;

import org.jruby.runtime.CallType;

/**
 * @see CachingCallSite
 */
public class MonomorphicCallSite extends CachingCallSite {
    public MonomorphicCallSite(String methodName) {
        super(methodName, CallType.NORMAL);
    }
}
