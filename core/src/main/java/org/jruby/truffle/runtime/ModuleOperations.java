/*
 * Copyright (c) 2014 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.runtime;

import com.oracle.truffle.api.CompilerAsserts;
import org.jruby.truffle.nodes.RubyNode;
import org.jruby.truffle.runtime.core.RubyClass;
import org.jruby.truffle.runtime.core.RubyModule;
import org.jruby.truffle.runtime.methods.RubyMethod;

import java.util.HashMap;
import java.util.Map;

public abstract class ModuleOperations {

    public static boolean includesModule(RubyModule module, RubyModule other) {
        for (RubyModule ancestor : module.ancestors()) {
            if (ancestor == other) {
                return true;
            }
        }

        return false;
    }

    public static boolean assignableTo(RubyClass thisClass, RubyClass otherClass) {
        RubyNode.notDesignedForCompilation();

        return includesModule(thisClass, otherClass);
    }

    public static Map<String, RubyConstant> getAllConstants(RubyModule module) {
        CompilerAsserts.neverPartOfCompilation();

        final Map<String, RubyConstant> constants = new HashMap<>();

        // Look in the current module
        constants.putAll(module.getConstants());

        // TODO(eregon): Look in lexical scope?

        // Look in ancestors
        for (RubyModule ancestor : module.parentAncestors()) {
            for (Map.Entry<String, RubyConstant> constant : ancestor.getConstants().entrySet()) {
                if (!constants.containsKey(constant.getKey())) {
                    constants.put(constant.getKey(), constant.getValue());
                }
            }
        }

        return constants;
    }

    /**
     * @param lexicalScope The surrounding LexicalScope, null if it is ignored (as in Mod::Constant)
     * @param module The receiver of the constant lookup.
     *               Identical to lexicalScope.getLiveModule() if there no qualifier (Constant).
     */
    public static RubyConstant lookupConstant(LexicalScope lexicalScope, RubyModule module, String name) {
        CompilerAsserts.neverPartOfCompilation();

        RubyConstant constant;

        // Look in the current module
        constant = module.getConstants().get(name);

        if (constant != null) {
            return constant;
        }

        // Look in lexical scope
        final RubyContext context = module.getContext();
        final RubyClass objectClass = context.getCoreLibrary().getObjectClass();

        if (lexicalScope != null) {
            if (lexicalScope.getLiveModule() == module && lexicalScope != context.getRootLexicalScope()) {
                // Already looked in module.
                lexicalScope = lexicalScope.getParent();
            }

            while (lexicalScope != context.getRootLexicalScope()) {
                constant = lexicalScope.getLiveModule().getConstants().get(name);

                if (constant != null) {
                    return constant;
                }

                lexicalScope = lexicalScope.getParent();
            }
        }

        // Look in ancestors
        for (RubyModule ancestor : module.parentAncestors()) {
            constant = ancestor.getConstants().get(name);

            if (constant != null) {
                return constant;
            }
        }

        // Look in Object and its included modules
        if (module.isOnlyAModule()) {
            for (RubyModule ancestor : objectClass.selfAndIncludedModules()) {
                constant = ancestor.getConstants().get(name);

                if (constant != null) {
                    return constant;
                }
            }
        }

        // Nothing found
        return null;
    }

    public static Map<String, RubyMethod> getAllMethods(RubyModule module) {
        CompilerAsserts.neverPartOfCompilation();

        final Map<String, RubyMethod> methods = new HashMap<>();

        // Look in the current module
        methods.putAll(module.getMethods());

        // Look in ancestors
        for (RubyModule ancestor : module.parentAncestors()) {
            for (Map.Entry<String, RubyMethod> method : ancestor.getMethods().entrySet()) {
                if (!methods.containsKey(method.getKey())) {
                    methods.put(method.getKey(), method.getValue());
                }
            }
        }

        return methods;
    }

    public static RubyMethod lookupMethod(RubyModule module, String name) {
        CompilerAsserts.neverPartOfCompilation();

        RubyMethod method;

        // Look in the current module
        method = module.getMethods().get(name);

        if (method != null) {
            return method;
        }

        // Look in ancestors
        for (RubyModule ancestor : module.parentAncestors()) {
            method = ancestor.getMethods().get(name);

            if (method != null) {
                return method;
            }
        }

        // Nothing found

        return null;
    }

    public static Map<String, Object> getAllClassVariables(RubyModule module) {
        CompilerAsserts.neverPartOfCompilation();

        final Map<String, Object> classVariables = new HashMap<>();

        // Look in the current module
        classVariables.putAll(module.getMethods());

        // Look in ancestors
        for (RubyModule ancestor : module.parentAncestors()) {
            for (Map.Entry<String, Object> classVariable : ancestor.getClassVariables().entrySet()) {
                if (!classVariables.containsKey(classVariable.getKey())) {
                    classVariables.put(classVariable.getKey(), classVariable.getValue());
                }
            }
        }

        return classVariables;
    }

    public static Object lookupClassVariable(RubyModule module, String name) {
        CompilerAsserts.neverPartOfCompilation();

        Object value;

        // Look in the current module
        value = module.getClassVariables().get(name);

        if (value != null) {
            return value;
        }

        // Look in ancestors
        for (RubyModule ancestor : module.parentAncestors()) {
            value = ancestor.getClassVariables().get(name);

            if (value != null) {
                return value;
            }
        }

        // Nothing found

        return null;
    }

    public static void setClassVariable(RubyModule module, String name, Object value) {
        CompilerAsserts.neverPartOfCompilation();

        // Look in the current module

        if (module.getClassVariables().containsKey(name)) {
            module.getClassVariables().put(name, value);
        }

        // Look in ancestors

        for (RubyModule ancestor : module.parentAncestors()) {
            if (ancestor.getClassVariables().containsKey(name)) {
                ancestor.getClassVariables().put(name, value);
            }
        }

        // Not existing class variable - set in the current module

        module.getClassVariables().put(name, value);
    }

}
