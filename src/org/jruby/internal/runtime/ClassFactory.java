
package org.jruby.internal.runtime;

import org.jruby.Ruby;
import org.jruby.RubyClass;
import org.jruby.RubyModule;
import org.jruby.exceptions.SecurityError;

public class ClassFactory {
    private final Ruby runtime;

    public ClassFactory(Ruby runtime) {
        this.runtime = runtime;
    }

    public RubyClass defineClass(RubyClass superClass, RubyModule parentClass, String name) {
        if (superClass == null) {
            superClass = runtime.getClasses().getObjectClass();
        }
        RubyClass newClass = RubyClass.newClass(runtime, superClass, parentClass, name);
        newClass.makeMetaClass(superClass.getMetaClass());
        newClass.inheritedBy(superClass);
        runtime.getClasses().putClass(name, newClass);
        return newClass;
    }

    public RubyModule defineModule(String name, RubyModule parentModule) {
        RubyModule newModule = RubyModule.newModule(runtime, name, parentModule);
        runtime.getClasses().putClass(name, newModule);
        return newModule;
    }

    /**
     * In the current context, get the named module. If it doesn't exist a
     * new module is created.
     */
    public RubyModule getOrCreateModule(String name) {
        RubyModule module;
        if (runtime.getRubyClass().isConstantDefined(name)) {
            module = (RubyModule) runtime.getRubyClass().getConstant(name);
            if (runtime.getSafeLevel() >= 4) {
                throw new SecurityError(runtime, "Extending module prohibited.");
            }
        } else {
            module = runtime.defineModule(name);
            runtime.getRubyClass().setConstant(name, module);
        }
        if (runtime.getWrapper() != null) {
            module.getSingletonClass().includeModule(runtime.getWrapper());
            module.includeModule(runtime.getWrapper());
        }
        return module;
    }
}
