package org.jruby.runtime.builtin.definitions;

import org.jruby.Ruby;
import org.jruby.RubyModule;
import org.jruby.runtime.IStaticCallable;

/**
 *
 * @author jpetersen
 * @version $Revision$
 */
public abstract class ModuleDefinition implements IStaticCallable {
    private final Ruby runtime;

    private boolean defined = false;
    private RubyModule module = null;

    /**
     * Constructor for ObjectDefinition.
     */
    protected ModuleDefinition(Ruby runtime) {
        this.runtime = runtime;
    }

    public RubyModule getModule() {
        synchronized (this) {
            if (!defined) {
                module = createModule(runtime);
                defineMethods(new MethodContext(module));
                defineModuleFunctions(new ModuleFunctionsContext(module, this));
                defined = true;
            }
            return module;
        }
    }

    protected abstract RubyModule createModule(Ruby runtime);

    protected abstract void defineModuleFunctions(ModuleFunctionsContext context);
    protected abstract void defineMethods(MethodContext context);
}