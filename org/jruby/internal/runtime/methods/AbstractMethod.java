package org.jruby.internal.runtime.methods;

import org.jruby.*;
import org.jruby.runtime.*;
import org.jruby.runtime.methods.*;

/**
 *
 * @author  jpetersen
 * @version $Revision$
 */
public abstract class AbstractMethod implements IMethod {
    private RubyModule implementationClass;
    private int noex;

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
    public int getNoex() {
        return noex;
    }

    /**
     * Sets the noex.
     * @param noex The noex to set
     */
    public void setNoex(int noex) {
        this.noex = noex;
    }
}