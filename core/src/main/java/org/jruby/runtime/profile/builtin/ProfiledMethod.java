package org.jruby.runtime.profile.builtin;

import org.jruby.internal.runtime.methods.DynamicMethod;
import org.jruby.util.ByteList;

/**
 * A dynamic method + it's invocation name holder for profiling purposes.
 */
public class ProfiledMethod {

    final ByteList name;
    final DynamicMethod method;
    
    public ProfiledMethod(ByteList name, DynamicMethod method) {
        this.name = name;
        this.method = method;
    }

    public ByteList getName() {
        return name;
    }

    public DynamicMethod getMethod() {
        return method;
    }
    
}
