package org.jruby.internal.runtime.methods;

import org.jruby.RubyModule;
import org.jruby.runtime.ICallable;
import org.jruby.runtime.Visibility;

/**
 *
 * @author  jpetersen
 * @version $Revision$
 */
public abstract class AbstractMethod implements ICallable {
    private RubyModule implementationClass;
    private Visibility visibility;
    
    protected AbstractMethod(Visibility visibility) {
        this.visibility = visibility;
    }
    
    /**
     * @see IMethod#getImplementationClass()
     */
    public RubyModule getImplementationClass() {
        return implementationClass;
    }

    /**
     * @see IMethod#setImplementationClass(RubyModule)
     */
    public void setImplementationClass(RubyModule implClass) {
        implementationClass = implClass;
    }

    /**
     * Gets the noex.
     * @return Returns a int
     */
    public Visibility getVisibility() {
        return visibility;
    }
    /**
     * @see org.jruby.runtime.ICallable#setVisibility(Visibility)
     */
    public void setVisibility(Visibility visibility) {
        this.visibility = visibility;
    }

    public boolean isUndefined() {
        return false;
    }

}