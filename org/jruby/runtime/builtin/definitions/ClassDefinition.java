package org.jruby.runtime.builtin.definitions;

import org.jruby.Ruby;
import org.jruby.RubyClass;
import org.jruby.runtime.IStaticCallable;

/**
 * 
 * @author jpetersen
 * @version $Revision$
 */
public abstract class ClassDefinition  implements IStaticCallable {
    private final Ruby runtime;

    private boolean defined = false;
    private RubyClass type = null;

    /**
     * Constructor for ObjectDefinition.
     */
    protected ClassDefinition(Ruby runtime) {
        this.runtime = runtime;
    }

    public RubyClass getType() {
        synchronized (this) {
            if (!defined) {
                type = createType(runtime);
                defineSingletonMethods(new SingletonMethodContext(type, this));
                defineMethods(new MethodContext(type));
                defined = true;
            }
            return type;
        }
    }

    protected abstract RubyClass createType(Ruby runtime);

    protected abstract void defineSingletonMethods(SingletonMethodContext context);
    protected abstract void defineMethods(MethodContext context);
}