package org.jruby.runtime.callsite;

import org.jruby.runtime.CallType;

/**
 * Single type call site
 *
 * @see CachingCallSite
 */
public class MonomorphicCallSite extends CachingCallSite {
    public MonomorphicCallSite(String methodName) {
        super(methodName, CallType.NORMAL);
    }
}
