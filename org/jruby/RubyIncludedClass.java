package org.jruby;

import org.jruby.exceptions.*;
import org.jruby.util.RubyMap;

/** This class represents an included module.
 * 
 * @author jpetersen
 */
public final class RubyIncludedClass extends RubyClass {
    private RubyModule delegate;

    /**
     * Constructor for RubyIncludedClass.
     * @param ruby
     * @param parent
     */
    public RubyIncludedClass(Ruby ruby, RubyClass superClass, RubyModule delegate) {
        super(ruby, superClass);

        this.delegate = delegate;
    }

    /** include_class_new
     *
     */
    public RubyIncludedClass newIncludeClass(RubyClass superClass) {
        return new RubyIncludedClass(getRuby(), superClass, getDelegate());
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
            throw new RubyFrozenException(getRuby(), "module");
        }
    }

    /** rb_cvar_singleton
     * 
     *@deprecated since Ruby 1.6.7
     */
    public RubyModule getClassVarSingleton() {
        return getDelegate();
    }

    /*
     * @see RubyObject#getRubyClass()
     */
    public RubyClass getInternalClass() {
//        throw new UnsupportedOperationException("An included class is only a wrapper for a module");
		return delegate.getInternalClass();
    }

    /*
     * @see RubyObject#setRubyClass(RubyClass)
     */
    public void setRubyClass(RubyClass newRubyClass) {
        throw new UnsupportedOperationException("An included class is only a wrapper for a module");
    }

    /*
     * @see RubyModule#getMethods()
     */
    public RubyMap getMethods() {
        return delegate.getMethods();
    }

    /*
     * @see RubyModule#setMethods(RubyMap)
     */
    public void setMethods(RubyMap newMethods) {
        throw new UnsupportedOperationException("An included class is only a wrapper for a module");
    }

    /*
     * @see RubyObject#getInstanceVariables()
     */
    public RubyMap getInstanceVariables() {
        return delegate.getInstanceVariables();
    }

    /*
     * @see RubyObject#setInstanceVariables(RubyMap)
     */
    public void setInstanceVariables(RubyMap newMethods) {
        throw new UnsupportedOperationException("An included class is only a wrapper for a module");
    }

    public String getClassname() {
		//throw new UnsupportedOperationException("An included class is only a wrapper for a module");
		return delegate.getClassname();
    }

    /**
     * Gets the delegateModule.
     * @return Returns a RubyModule
     */
    public RubyModule getDelegate() {
        return delegate;
    }
}
