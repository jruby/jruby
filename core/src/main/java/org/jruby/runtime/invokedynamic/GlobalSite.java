package org.jruby.runtime.invokedynamic;

import java.lang.invoke.MethodHandle;
import org.jruby.Ruby;
import org.jruby.RubyClass;
import org.jruby.common.IRubyWarnings;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

import java.lang.invoke.MethodType;
import java.lang.invoke.MutableCallSite;

public class GlobalSite extends MutableCallSite {
    private final String name;
    private volatile int failures;

    public GlobalSite(MethodType type, String name) {
        super(type);
        this.name = name;
    }
    
    public void setTarget(MethodHandle target) {
        super.setTarget(target);
        incrementFailures();
    }
    
    public int failures() {
        return failures;
    }
    
    public void incrementFailures() {
        failures += 1;
    }

    public String name() {
        return name;
    }
}
