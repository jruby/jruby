
package org.jruby.internal.runtime;

import org.jruby.Ruby;
import org.jruby.RubyClass;
import org.jruby.RubyModule;
import org.jruby.exceptions.SecurityError;
import org.jruby.exceptions.TypeError;
import org.jruby.runtime.builtin.IRubyObject;

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

    private boolean matchingClassExists(String className, RubyClass superClass) {
        if (! isConstantDefined(className)) {
            return false;
        }
        IRubyObject type = getConstant(className);
        if (! (type instanceof RubyClass)) {
            throw new TypeError(runtime, className + " is not a class");
        }
        RubyClass rubyClass = (RubyClass) type;
        if (superClass != null) {
            if (rubyClass.getSuperClass().getRealClass() != superClass) {
                return false;
            }
        }
        if (runtime.getSafeLevel() >= 4) {
            throw new SecurityError(runtime, "extending class prohibited");
        }
        return true;
    }

    private RubyClass createClass(String className, RubyClass superClass) {
        if (superClass == null) {
            superClass = runtime.getClasses().getObjectClass();
        }
        RubyClass result = runtime.defineClass(className, superClass);
        setConstant(className, result);
        return result;
    }

    private boolean isConstantDefined(String name) {
        return runtime.getRubyClass().isConstantDefined(name);
    }

    private void setConstant(String name, IRubyObject value) {
        runtime.getRubyClass().setConstant(name, value);
    }

    private IRubyObject getConstant(String name) {
        return runtime.getRubyClass().getConstant(name);
    }
}
