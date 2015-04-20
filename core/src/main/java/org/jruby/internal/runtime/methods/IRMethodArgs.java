package org.jruby.internal.runtime.methods;

import org.jruby.runtime.Signature;

public interface IRMethodArgs {
    // FIXME: Should get pushed to DynamicMethod
    public Signature getSignature();
    public String[] getParameterList();

    public enum ArgType {
        key, keyreq, keyrest, block, opt, rest, req
    }
}
