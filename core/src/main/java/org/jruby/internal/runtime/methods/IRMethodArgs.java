package org.jruby.internal.runtime.methods;

import java.util.List;

public interface IRMethodArgs {
    public List<String[]> getParameterList();

    public enum ArgType {
        key, keyrest, block, opt, rest, req
    }
}
