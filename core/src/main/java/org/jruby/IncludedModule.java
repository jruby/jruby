package org.jruby;

import java.util.Map;

import org.jruby.runtime.builtin.IRubyObject;

public class IncludedModule extends RubyClass {
    public IncludedModule(Ruby runtime, RubyClass superClass, RubyModule origin) {
        super(runtime, superClass, false);
        this.origin = origin;
        this.metaClass = origin.metaClass;
        if (superClass != null) {
            setClassIndex(superClass.getClassIndex()); // use same ClassIndex as metaclass, since we're technically still of that type
        }
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

    @Override
    public String getName() {
        return origin.getName();
    }

    @Override
    public RubyModule getOrigin() {
        return origin;
    }

    @Deprecated
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

    @Override
    protected synchronized Map<String, IRubyObject> getClassVariables() {
        return origin.getClassVariables();
    }

    @Override
    protected Map<String, IRubyObject> getClassVariablesForRead() {
        return origin.getClassVariablesForRead();
    }

    /** The module to which this include origins. */
    protected final RubyModule origin;
}