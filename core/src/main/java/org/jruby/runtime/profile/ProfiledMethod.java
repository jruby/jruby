package org.jruby.runtime.profile;

import org.jruby.internal.runtime.methods.DynamicMethod;

/**
 * A dynamic method + it's invocation name holder for profiling purposes.
 */
public class ProfiledMethod {

    final String name;
    final DynamicMethod method;
    
    public ProfiledMethod(String name, DynamicMethod method) {
        this.name = name;
        this.method = method;
    }

    public String getName() {
        return name;
    }

    public DynamicMethod getMethod() {
        return method;
    }
    
}
