package org.jruby;

import java.util.Map;

import org.jruby.runtime.builtin.IRubyObject;

public class IncludedModule extends RubyClass {
    public IncludedModule(Ruby runtime, RubyClass superClass, RubyModule origin) {
        super(runtime, superClass, false);
        this.origin = origin;
        this.metaClass = origin.metaClass;
    }

    @Override
    public boolean isModule() {
        return false;
    }

    @Override
    public boolean isClass() {
        return false;
    }

    @Override
    public boolean isImmediate() {
        return true;
    }

    @Override
    public void setMetaClass(RubyClass newRubyClass) {
        throw new UnsupportedOperationException("An included class is only a wrapper for a module");
    }

    public void setMethods(Map newMethods) {
        throw new UnsupportedOperationException("An included class is only a wrapper for a module");
    }

    @Override
    public String getName() {
        return origin.getName();
    }

    @Override
    public RubyModule getNonIncludedClass() {
        return origin;
    }

   /**
    * We don't want to reveal ourselves to Ruby code, so origin this
    * operation.
    */
    @Override
    public IRubyObject id() {
        return origin.id();
    }

    /** The module to which this include origins. */
    protected final RubyModule origin;
}