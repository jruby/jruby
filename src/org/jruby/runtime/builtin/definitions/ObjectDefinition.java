package org.jruby.runtime.builtin.definitions;

import org.jruby.Ruby;
import org.jruby.RubyObject;
import org.jruby.runtime.IStaticCallable;

/**
 * 
 * @author jpetersen
 * @version $Revision$
 */
public abstract class ObjectDefinition implements IStaticCallable {
    private final Ruby runtime;

    private boolean defined = false;
    private RubyObject singleton = null;

    /**
     * Constructor for ObjectDefinition.
     */
    protected ObjectDefinition(Ruby runtime) {
        this.runtime = runtime;
    }

    public RubyObject getObject() {
        synchronized (this) {
            if (!defined) {
                singleton = createObject(runtime);
                defineSingletonMethods(new SingletonMethodContext(singleton, this));
                defined = true;
            }
            return singleton;
        }
    }

    protected abstract RubyObject createObject(Ruby runtime);

    protected abstract void defineSingletonMethods(SingletonMethodContext context);

}