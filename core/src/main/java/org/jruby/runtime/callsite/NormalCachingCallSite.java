package org.jruby.runtime.callsite;

import org.jruby.runtime.CallType;

/**
 * @deprecated replaced with {@link MonomorphicCallSite}
 */
public class NormalCachingCallSite extends CachingCallSite {

    public NormalCachingCallSite(String methodName) {
        super(methodName, CallType.NORMAL);
    }

}