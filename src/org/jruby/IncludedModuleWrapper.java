package org.jruby;

import org.jruby.exceptions.FrozenError;

import java.util.Map;

/** This class represents an included module.
 * 
 * @author jpetersen
 */
public final class IncludedModuleWrapper extends RubyClass {
    private RubyModule delegate;

    public IncludedModuleWrapper(Ruby ruby, RubyClass superClass, RubyModule delegate) {
        super(ruby, superClass);

        this.delegate = delegate;
    }

    /** include_class_new
     *
     */
    public IncludedModuleWrapper newIncludeClass(RubyClass superClass) {
        return new IncludedModuleWrapper(getRuntime(), superClass, getDelegate());
    }

    public boolean isModule() {
        return false;
    }

    public boolean isClass() {
        return false;
    }

    public boolean isIncluded() {
        return true;
    }

    protected void testFrozen() {
        if (isFrozen()) {
            throw new FrozenError(getRuntime(), "module");
        }
    }

    public RubyClass getMetaClass() {
		return delegate.getMetaClass();
    }

    public void setMetaClass(RubyClass newRubyClass) {
        throw new UnsupportedOperationException("An included class is only a wrapper for a module");
    }

    public Map getMethods() {
        return delegate.getMethods();
    }

    public void setMethods(Map newMethods) {
        throw new UnsupportedOperationException("An included class is only a wrapper for a module");
    }

    public Map getInstanceVariables() {
        return delegate.getInstanceVariables();
    }

    public void setInstanceVariables(Map newMethods) {
        throw new UnsupportedOperationException("An included class is only a wrapper for a module");
    }

    public String getName() {
		return delegate.getName();
    }

    public RubyModule getDelegate() {
        return delegate;
    }
    
    public RubyClass getRealClass() {
        return getSuperClass().getRealClass();
    }
}
