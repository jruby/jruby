package org.jruby.internal.runtime.methods;

import org.jruby.RubyModule;
import org.jruby.runtime.Arity;
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
    
    public RubyModule getImplementationClass() {
        return implementationClass;
    }

    public void setImplementationClass(RubyModule implClass) {
        implementationClass = implClass;
    }

    public Visibility getVisibility() {
        return visibility;
    }

    public void setVisibility(Visibility visibility) {
        this.visibility = visibility;
    }

    public boolean isUndefined() {
        return false;
    }

    public Arity getArity() {
        return Arity.optional();
    }

    public void initializeCacheEntry(CacheEntry cacheEntry) {
        cacheEntry.setVisibility(getVisibility());
        cacheEntry.setOrigin(getImplementationClass());
        cacheEntry.setMethod(this);
        cacheEntry.setRecvClass(getImplementationClass());
    }
}