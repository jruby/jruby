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
    public final String name;
    private final String file;
    private final int line;
    private volatile int failures;

    public GlobalSite(MethodType type, String name, String file, int line) {
        super(type);
        this.name = name;
        this.file = file;
        this.line = line;
    }

    public String file() {
        return file;
    }

    public int line() {
        return line;
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
}
