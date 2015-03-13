package org.jruby.internal.runtime.methods;

import java.util.List;

public interface IRMethodArgs {
    public String[] getParameterList();

    public enum ArgType {
        key, keyreq, keyrest, block, opt, rest, req
    }
}
