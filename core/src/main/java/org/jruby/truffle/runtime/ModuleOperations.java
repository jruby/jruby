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
import org.jruby.truffle.runtime.methods.RubyMethod;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public abstract class ModuleOperations {

    public static boolean includesModule(ModuleChain module, ModuleChain other) {
        while (module != null) {
            if (module == other) {
                return true;
            }

            module = module.getParentModule();
        }

        return false;
    }

    public static Map<String, RubyConstant> getAllConstants(ModuleChain module) {
        CompilerAsserts.neverPartOfCompilation();

        final Map<String, RubyConstant> methods = new HashMap<>();

        // Look in the current module

        methods.putAll(module.getConstants());

        // Look in lexical ancestors

        ModuleChain lexicalAncestor = module.getLexicalParentModule();

        while (lexicalAncestor != null) {
            for (Map.Entry<String, RubyConstant> constant : lexicalAncestor.getConstants().entrySet()) {
                if (!methods.containsKey(constant.getKey())) {
                    methods.put(constant.getKey(), constant.getValue());
                }
            }

            lexicalAncestor = lexicalAncestor.getLexicalParentModule();
        }

        // Look in ancestors

        ModuleChain ancestor = module.getParentModule();

        while (ancestor != null) {
            for (Map.Entry<String, RubyConstant> constant : ancestor.getConstants().entrySet()) {
                if (!methods.containsKey(constant.getKey())) {
                    methods.put(constant.getKey(), constant.getValue());
                }
            }

            ancestor = ancestor.getParentModule();
        }

        return methods;
    }

    public static RubyConstant lookupConstant(ModuleChain module, String name) {
        CompilerAsserts.neverPartOfCompilation();

        RubyConstant constant;

        // Look in the current module

        constant = module.getConstants().get(name);

        if (constant != null) {
            return constant;
        }

        // Look in lexical ancestors

        ModuleChain lexicalAncestor = module.getLexicalParentModule();

        while (lexicalAncestor != null) {
            constant = lexicalAncestor.getConstants().get(name);

            if (constant != null) {
                return constant;
            }

            lexicalAncestor = lexicalAncestor.getLexicalParentModule();
        }

        // Look in ancestors

        ModuleChain ancestor = module.getParentModule();

        while (ancestor != null) {
            constant = ancestor.getConstants().get(name);

            if (constant != null) {
                return constant;
            }

            ancestor = ancestor.getParentModule();
        }

        // Nothing found

        return null;
    }

    public static Map<String, RubyMethod> getAllMethods(ModuleChain module) {
        CompilerAsserts.neverPartOfCompilation();

        final Map<String, RubyMethod> methods = new HashMap<>();

        // Look in the current module

        methods.putAll(module.getMethods());

        // Look in ancestors

        ModuleChain ancestor = module.getParentModule();

        while (ancestor != null) {
            for (Map.Entry<String, RubyMethod> method : ancestor.getMethods().entrySet()) {
                if (!methods.containsKey(method.getKey())) {
                    methods.put(method.getKey(), method.getValue());
                }
            }

            ancestor = ancestor.getParentModule();
        }

        return methods;
    }

    public static RubyMethod lookupMethod(ModuleChain module, String name) {
        CompilerAsserts.neverPartOfCompilation();

        RubyMethod method;

        // Look in the current module

        method = module.getMethods().get(name);

        if (method != null) {
            return method;
        }

        // Look in ancestors

        ModuleChain ancestor = module.getParentModule();

        while (ancestor != null) {
            method = ancestor.getMethods().get(name);

            if (method != null) {
                return method;
            }

            ancestor = ancestor.getParentModule();
        }

        // Nothing found

        return null;
    }

    public static Map<String, Object> getAllClassVariables(ModuleChain module) {
        CompilerAsserts.neverPartOfCompilation();

        final Map<String, Object> classVariables = new HashMap<>();

        // Look in the current module

        classVariables.putAll(module.getMethods());

        // Look in ancestors

        ModuleChain ancestor = module.getParentModule();

        while (ancestor != null) {
            for (Map.Entry<String, Object> classVariable : ancestor.getClassVariables().entrySet()) {
                if (!classVariables.containsKey(classVariable.getKey())) {
                    classVariables.put(classVariable.getKey(), classVariable.getValue());
                }
            }

            ancestor = ancestor.getParentModule();
        }

        return classVariables;
    }

    public static Object lookupClassVariable(ModuleChain module, String name) {
        CompilerAsserts.neverPartOfCompilation();

        Object value;

        // Look in the current module

        value = module.getClassVariables().get(name);

        if (value != null) {
            return value;
        }

        // Look in ancestors

        ModuleChain ancestor = module.getParentModule();

        while (ancestor != null) {
            value = ancestor.getClassVariables().get(name);

            if (value != null) {
                return value;
            }

            ancestor = ancestor.getParentModule();
        }

        // Nothing found

        return null;
    }

    public static void setClassVariable(ModuleChain module, String name, Object value) {
        CompilerAsserts.neverPartOfCompilation();

        // Look in the current module

        if (module.getClassVariables().containsKey(name)) {
            module.getClassVariables().put(name, value);
        }

        // Look in ancestors

        ModuleChain ancestor = module.getParentModule();

        while (ancestor != null) {
            if (ancestor.getClassVariables().containsKey(name)) {
                ancestor.getClassVariables().put(name, value);
            }

            ancestor = ancestor.getParentModule();
        }

        // Not existing class variable - set in the current module

        module.getClassVariables().put(name, value);
    }

    public static boolean assignableTo(ModuleChain thisModule, ModuleChain otherModule) {
        RubyNode.notDesignedForCompilation();

        ModuleChain ancestor = thisModule;

        do {
            if (ancestor == otherModule) {
                return true;
            }

            ancestor = ancestor.getParentModule();
        } while (ancestor != null);

        return false;
    }

    public static void debugModuleChain(ModuleChain module) {
        while (module != null) {
            System.err.print(module.getClass());

            if (module instanceof RubyClass) {
                System.err.print(" " + ((RubyClass) module).getName());
            } else if (module instanceof IncludedModule) {
                System.err.print(" " + ((IncludedModule) module).getIncludedModule().getName());
            }

            System.err.println();
            module = module.getParentModule();
        }
    }

}
