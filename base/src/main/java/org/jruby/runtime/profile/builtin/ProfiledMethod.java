package org.jruby.runtime.profile.builtin;

import org.jruby.internal.runtime.methods.DynamicMethod;
import org.jruby.util.ByteList;

/**
 * A dynamic method + it's invocation name holder for profiling purposes.
 */
public class ProfiledMethod {
    final String id;
    final DynamicMethod method;
    
    public ProfiledMethod(String id, DynamicMethod method) {
        this.id = id;
        this.method = method;
    }

    public String getName() {
        return id;
    }

    public DynamicMethod getMethod() {
        return method;
    }
    
}
