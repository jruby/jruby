/*
 * Copyright (c) 2014, 2015 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.runtime;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.frame.Frame;
import com.oracle.truffle.api.nodes.Node;

import org.jruby.runtime.Visibility;
import org.jruby.truffle.nodes.RubyNode;
import org.jruby.truffle.runtime.control.RaiseException;
import org.jruby.truffle.runtime.core.RubyClass;
import org.jruby.truffle.runtime.core.RubyModule;
import org.jruby.truffle.runtime.methods.InternalMethod;
import org.jruby.util.IdUtil;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

public abstract class ModuleOperations {

    public static boolean includesModule(RubyModule module, RubyModule other) {
        for (RubyModule ancestor : module.ancestors()) {
            if (ancestor == other) {
                return true;
            }
        }

        return false;
    }

    public static boolean assignableTo(RubyClass thisClass, RubyModule otherClass) {
        RubyNode.notDesignedForCompilation();

        return includesModule(thisClass, otherClass);
    }

    public static boolean canBindMethodTo(RubyModule origin, RubyModule module) {
        if (!(origin instanceof RubyClass)) {
            return true;
        } else {
            return ((module instanceof RubyClass) && ModuleOperations.assignableTo((RubyClass) module, origin));
        }
    }

    @TruffleBoundary
    public static Map<String, RubyConstant> getAllConstants(RubyModule module) {
        CompilerAsserts.neverPartOfCompilation();

        final Map<String, RubyConstant> constants = new HashMap<>();

        // Look in the current module
        constants.putAll(module.getConstants());

        // Look in ancestors
        for (RubyModule ancestor : module.includedModules()) {
            for (Map.Entry<String, RubyConstant> constant : ancestor.getConstants().entrySet()) {
                if (!constants.containsKey(constant.getKey())) {
                    constants.put(constant.getKey(), constant.getValue());
                }
            }
        }

        return constants;
    }

    /**
     * @param lexicalScope The surrounding LexicalScope (as in Constant),
     *                     or null if it is ignored (as in Mod::Constant or ::Constant)
     * @param module The receiver of the constant lookup.
     *               Must be identical to lexicalScope.getLiveModule() if lexicalScope != null.
     */
    @TruffleBoundary
    public static RubyConstant lookupConstant(RubyContext context, LexicalScope lexicalScope, RubyModule module, String name) {
        CompilerAsserts.neverPartOfCompilation();
        assert lexicalScope == null || lexicalScope.getLiveModule() == module;

        RubyConstant constant;

        // Look in the current module
        constant = module.getConstants().get(name);

        if (constant != null) {
            return constant;
        }

        // Look in lexical scope
        if (lexicalScope != null) {
            if (lexicalScope != context.getRootLexicalScope()) {
                // Already looked in the top lexical scope, which is module.
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
            final RubyClass objectClass = context.getCoreLibrary().getObjectClass();

            constant = objectClass.getConstants().get(name);
            if (constant != null) {
                return constant;
            }

            for (RubyModule ancestor : objectClass.includedModules()) {
                constant = ancestor.getConstants().get(name);

                if (constant != null) {
                    return constant;
                }
            }
        }

        // Nothing found
        return null;
    }

    public static RubyConstant lookupScopedConstant(RubyContext context, RubyModule module, String fullName, boolean inherit, Node currentNode) {
        CompilerAsserts.neverPartOfCompilation();

        int start = 0, next;
        if (fullName.startsWith("::")) {
            module = context.getCoreLibrary().getObjectClass();
            start += 2;
        }

        while ((next = fullName.indexOf("::", start)) != -1) {
            String segment = fullName.substring(start, next);
            RubyConstant constant = lookupConstantWithInherit(context, module, segment, inherit, currentNode);
            if (constant == null) {
                return null;
            } else if (constant.getValue() instanceof RubyModule) {
                module = (RubyModule) constant.getValue();
            } else {
                CompilerDirectives.transferToInterpreter();
                throw new RaiseException(context.getCoreLibrary().typeError(fullName.substring(0, next) + " does not refer to class/module", currentNode));
            }
            start = next + 2;
        }

        String lastSegment = fullName.substring(start);
        return lookupConstantWithInherit(context, module, lastSegment, inherit, currentNode);
    }

    private static RubyConstant lookupConstantWithInherit(RubyContext context, RubyModule module, String name, boolean inherit, Node currentNode) {
        if (!IdUtil.isValidConstantName19(name)) {
            CompilerDirectives.transferToInterpreter();
            throw new RaiseException(context.getCoreLibrary().nameError(String.format("wrong constant name %s", name), name, currentNode));
        }

        if (inherit) {
            return ModuleOperations.lookupConstant(context, LexicalScope.NONE, module, name);
        } else {
            return module.getConstants().get(name);
        }
    }

    @TruffleBoundary
    public static Map<String, InternalMethod> getAllMethods(RubyModule module) {
        final Map<String, InternalMethod> methods = new HashMap<>();

        for (RubyModule ancestor : module.ancestors()) {
            for (InternalMethod method : ancestor.getMethods().values()) {
                if (!methods.containsKey(method.getName())) {
                    methods.put(method.getName(), method);
                }
            }
        }

        return methods;
    }

    @TruffleBoundary
    public static Map<String, InternalMethod> withoutUndefinedMethods(Map<String, InternalMethod> methods) {
        Map<String, InternalMethod> definedMethods = new HashMap<>();
        for (Entry<String, InternalMethod> method : methods.entrySet()) {
            if (!method.getValue().isUndefined()) {
                definedMethods.put(method.getKey(), method.getValue());
            }
        }
        return definedMethods;
    }

    @TruffleBoundary
    public static InternalMethod lookupMethod(RubyModule module, String name) {
        CompilerAsserts.neverPartOfCompilation();

        InternalMethod method;

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

    @TruffleBoundary
    public static InternalMethod lookupSuperMethod(RubyModule declaringModule, String name, RubyClass objectMetaClass) {
        CompilerAsserts.neverPartOfCompilation();

        boolean foundDeclaringModule = false;
        for (RubyModule module : objectMetaClass.ancestors()) {
            if (module == declaringModule) {
                foundDeclaringModule = true;
            } else if (foundDeclaringModule) {
                InternalMethod method = module.getMethods().get(name);

                if (method != null) {
                    return method;
                }
            }
        }
        assert foundDeclaringModule : "Did not find the declaring module in "+objectMetaClass.getName()+" ancestors";

        return null;
    }

    @TruffleBoundary
    public static Map<String, Object> getAllClassVariables(RubyModule module) {
        CompilerAsserts.neverPartOfCompilation();

        final Map<String, Object> classVariables = new HashMap<>();

        // Look in the current module
        classVariables.putAll(module.getClassVariables());

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

    @TruffleBoundary
    public static Object lookupClassVariable(RubyModule module, String name) {
        CompilerAsserts.neverPartOfCompilation();

        // Look in the current module
        Object value = module.getClassVariables().get(name);
        if (value != null) {
            return value;
        }

        // If singleton class, check attached module.
        if (module instanceof RubyClass) {
            RubyClass klass = (RubyClass) module;
            if (klass.isSingleton() && klass.getAttached() != null) {
                module = klass.getAttached();

                value = module.getClassVariables().get(name);
                if (value != null) {
                    return value;
                }
            }
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

    @TruffleBoundary
    public static void setClassVariable(RubyModule module, String name, Object value, Node currentNode) {
        CompilerAsserts.neverPartOfCompilation();

        // Look in the current module

        if (module.getClassVariables().containsKey(name)) {
            module.setClassVariable(currentNode, name, value);
            return;
        }

        // Look in ancestors

        for (RubyModule ancestor : module.parentAncestors()) {
            if (ancestor.getClassVariables().containsKey(name)) {
                ancestor.setClassVariable(currentNode, name, value);
                return;
            }
        }

        // Not existing class variable - set in the current module

        module.setClassVariable(currentNode, name, value);
    }

    public static boolean isMethodPrivateFromName(String name) {
        CompilerAsserts.neverPartOfCompilation();

        return (name.equals("initialize") || name.equals("initialize_copy") ||
                name.equals("initialize_clone") || name.equals("initialize_dup") ||
                name.equals("respond_to_missing?"));
    }

}
