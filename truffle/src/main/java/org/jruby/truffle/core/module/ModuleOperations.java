/*
 * Copyright (c) 2014, 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.core.module;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.object.DynamicObject;
import org.jruby.truffle.Layouts;
import org.jruby.truffle.RubyContext;
import org.jruby.truffle.core.string.StringUtils;
import org.jruby.truffle.language.LexicalScope;
import org.jruby.truffle.language.RubyConstant;
import org.jruby.truffle.language.RubyGuards;
import org.jruby.truffle.language.Visibility;
import org.jruby.truffle.language.control.RaiseException;
import org.jruby.truffle.language.methods.InternalMethod;
import org.jruby.truffle.language.objects.shared.SharedObjects;
import org.jruby.truffle.parser.Identifiers;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Function;

public abstract class ModuleOperations {

    public static boolean includesModule(DynamicObject module, DynamicObject other) {
        CompilerAsserts.neverPartOfCompilation();
        assert RubyGuards.isRubyModule(module);
        //assert RubyGuards.isRubyModule(other);

        for (DynamicObject ancestor : Layouts.MODULE.getFields(module).ancestors()) {
            if (ancestor == other) {
                return true;
            }
        }

        return false;
    }

    public static boolean assignableTo(DynamicObject thisClass, DynamicObject otherClass) {
        return includesModule(thisClass, otherClass);
    }

    public static boolean canBindMethodTo(DynamicObject origin, DynamicObject module) {
        assert RubyGuards.isRubyModule(origin);
        assert RubyGuards.isRubyModule(module);

        if (!(RubyGuards.isRubyClass(origin))) {
            return true;
        } else {
            return ((RubyGuards.isRubyClass(module)) && ModuleOperations.assignableTo(module, origin));
        }
    }

    @TruffleBoundary
    public static Iterable<Entry<String, RubyConstant>> getAllConstants(DynamicObject module) {
        CompilerAsserts.neverPartOfCompilation();

        assert RubyGuards.isRubyModule(module);

        final Map<String, RubyConstant> constants = new HashMap<>();

        // Look in the current module
        for (Map.Entry<String, RubyConstant> constant : Layouts.MODULE.getFields(module).getConstants()) {
            constants.put(constant.getKey(), constant.getValue());
        }

        // Look in ancestors
        for (DynamicObject ancestor : Layouts.MODULE.getFields(module).prependedAndIncludedModules()) {
            for (Map.Entry<String, RubyConstant> constant : Layouts.MODULE.getFields(ancestor).getConstants()) {
                if (!constants.containsKey(constant.getKey())) {
                    constants.put(constant.getKey(), constant.getValue());
                }
            }
        }

        return constants.entrySet();
    }

    @TruffleBoundary
    public static RubyConstant lookupConstant(RubyContext context, DynamicObject module, String name) {
        CompilerAsserts.neverPartOfCompilation();
        assert RubyGuards.isRubyModule(module);

        // Look in the current module
        RubyConstant constant = Layouts.MODULE.getFields(module).getConstant(name);
        if (constant != null) {
            return constant;
        }

        // Look in ancestors
        for (DynamicObject ancestor : Layouts.MODULE.getFields(module).parentAncestors()) {
            constant = Layouts.MODULE.getFields(ancestor).getConstant(name);
            if (constant != null) {
                return constant;
            }
        }

        // Nothing found
        return null;
    }

    private static RubyConstant lookupConstantInObject(RubyContext context, DynamicObject module, String name) {
        // Look in Object and its included modules for modules (not for classes)
        if (!RubyGuards.isRubyClass(module)) {
            final DynamicObject objectClass = context.getCoreLibrary().getObjectClass();

            RubyConstant constant = Layouts.MODULE.getFields(objectClass).getConstant(name);
            if (constant != null) {
                return constant;
            }

            for (DynamicObject ancestor : Layouts.MODULE.getFields(objectClass).prependedAndIncludedModules()) {
                constant = Layouts.MODULE.getFields(ancestor).getConstant(name);
                if (constant != null) {
                    return constant;
                }
            }
        }

        return null;
    }

    public static RubyConstant lookupConstantAndObject(RubyContext context, DynamicObject module, String name) {
        final RubyConstant constant = lookupConstant(context, module, name);
        if (constant != null) {
            return constant;
        }

        return lookupConstantInObject(context, module, name);
    }

    @TruffleBoundary
    public static RubyConstant lookupConstantWithLexicalScope(RubyContext context, LexicalScope lexicalScope, String name) {
        CompilerAsserts.neverPartOfCompilation();

        final DynamicObject module = lexicalScope.getLiveModule();

        RubyConstant constant = null;

        // Look in lexical scope
        while (lexicalScope != context.getRootLexicalScope()) {
            constant = Layouts.MODULE.getFields(lexicalScope.getLiveModule()).getConstant(name);
            if (constant != null) {
                return constant;
            }

            lexicalScope = lexicalScope.getParent();
        }

        return lookupConstantAndObject(context, module, name);
    }

    public static RubyConstant lookupScopedConstant(RubyContext context, DynamicObject module, String fullName, boolean inherit, Node currentNode) {
        CompilerAsserts.neverPartOfCompilation();

        int start = 0, next;
        if (fullName.startsWith("::")) {
            module = context.getCoreLibrary().getObjectClass();
            start += 2;
        }

        while ((next = fullName.indexOf("::", start)) != -1) {
            final String segment = fullName.substring(start, next);
            final RubyConstant constant = lookupConstantWithInherit(context, module, segment, inherit, currentNode);
            if (constant == null) {
                return null;
            } else if (RubyGuards.isRubyModule(constant.getValue())) {
                module = (DynamicObject) constant.getValue();
            } else {
                throw new RaiseException(context.getCoreExceptions().typeError(fullName.substring(0, next) + " does not refer to class/module", currentNode));
            }
            start = next + 2;
        }

        final String lastSegment = fullName.substring(start);
        if (!Identifiers.isValidConstantName19(lastSegment)) {
            throw new RaiseException(context.getCoreExceptions().nameError(StringUtils.format("wrong constant name %s", fullName), module, fullName, currentNode));
        }

        return lookupConstantWithInherit(context, module, lastSegment, inherit, currentNode);
    }

    @TruffleBoundary(throwsControlFlowException = true)
    public static RubyConstant lookupConstantWithInherit(RubyContext context, DynamicObject module, String name, boolean inherit, Node currentNode) {
        assert RubyGuards.isRubyModule(module);

        if (!Identifiers.isValidConstantName19(name)) {
            throw new RaiseException(context.getCoreExceptions().nameError(StringUtils.format("wrong constant name %s", name), module, name, currentNode));
        }

        if (inherit) {
            return ModuleOperations.lookupConstantAndObject(context, module, name);
        } else {
            return Layouts.MODULE.getFields(module).getConstant(name);
        }
    }

    @TruffleBoundary
    public static Map<String, InternalMethod> getAllMethods(DynamicObject module) {
        assert RubyGuards.isRubyModule(module);

        final Map<String, InternalMethod> methods = new HashMap<>();

        for (DynamicObject ancestor : Layouts.MODULE.getFields(module).ancestors()) {
            for (InternalMethod method : Layouts.MODULE.getFields(ancestor).getMethods()) {
                if (!methods.containsKey(method.getName())) {
                    methods.put(method.getName(), method);
                }
            }
        }

        return methods;
    }

    @TruffleBoundary
    public static Map<String, InternalMethod> getMethodsBeforeLogicalClass(DynamicObject module) {
        assert RubyGuards.isRubyModule(module);

        final Map<String, InternalMethod> methods = new HashMap<>();

        for (DynamicObject ancestor : Layouts.MODULE.getFields(module).ancestors()) {
            // When we find a class which is not a singleton class, we are done
            if (RubyGuards.isRubyClass(ancestor) && !Layouts.CLASS.getIsSingleton(ancestor)) {
                break;
            }

            for (InternalMethod method : Layouts.MODULE.getFields(ancestor).getMethods()) {
                if (!methods.containsKey(method.getName())) {
                    methods.put(method.getName(), method);
                }
            }
        }

        return methods;
    }

    @TruffleBoundary
    public static Map<String, InternalMethod> getMethodsUntilLogicalClass(DynamicObject module) {
        assert RubyGuards.isRubyModule(module);

        final Map<String, InternalMethod> methods = new HashMap<>();

        for (DynamicObject ancestor : Layouts.MODULE.getFields(module).ancestors()) {
            for (InternalMethod method : Layouts.MODULE.getFields(ancestor).getMethods()) {
                if (!methods.containsKey(method.getName())) {
                    methods.put(method.getName(), method);
                }
            }

            // When we find a class which is not a singleton class, we are done
            if (RubyGuards.isRubyClass(ancestor) && !Layouts.CLASS.getIsSingleton(ancestor)) {
                break;
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
    public static InternalMethod lookupMethod(DynamicObject module, String name) {
        CompilerAsserts.neverPartOfCompilation();

        assert RubyGuards.isRubyModule(module);

        // Look in ancestors
        for (DynamicObject ancestor : Layouts.MODULE.getFields(module).ancestors()) {
            InternalMethod method = Layouts.MODULE.getFields(ancestor).getMethod(name);

            if (method != null) {
                return method;
            }
        }

        // Nothing found
        return null;
    }

    public static InternalMethod lookupMethod(DynamicObject module, String name, Visibility visibility) {
        InternalMethod method = lookupMethod(module, name);
        return (method != null && method.getVisibility() == visibility) ? method : null;
    }

    public static InternalMethod lookupSuperMethod(InternalMethod currentMethod, DynamicObject objectMetaClass) {
        assert RubyGuards.isRubyModule(objectMetaClass);
        final String name = currentMethod.getSharedMethodInfo().getName(); // use the original name
        return lookupSuperMethod(currentMethod.getDeclaringModule(), name, objectMetaClass);
    }

    @TruffleBoundary
    private static InternalMethod lookupSuperMethod(DynamicObject declaringModule, String name, DynamicObject objectMetaClass) {
        assert RubyGuards.isRubyModule(declaringModule);
        assert RubyGuards.isRubyModule(objectMetaClass);

        boolean foundDeclaringModule = false;
        for (DynamicObject module : Layouts.MODULE.getFields(objectMetaClass).ancestors()) {
            if (module == declaringModule) {
                foundDeclaringModule = true;
            } else if (foundDeclaringModule) {
                InternalMethod method = Layouts.MODULE.getFields(module).getMethod(name);

                if (method != null) {
                    return method;
                }
            }
        }
        assert foundDeclaringModule : "Did not find the declaring module in " + Layouts.MODULE.getFields(objectMetaClass).getName() + " ancestors";

        return null;
    }

    @TruffleBoundary
    public static Map<String, Object> getAllClassVariables(DynamicObject module) {
        CompilerAsserts.neverPartOfCompilation();

        assert RubyGuards.isRubyModule(module);

        final Map<String, Object> classVariables = new HashMap<>();

        classVariableLookup(module, module1 -> {
            classVariables.putAll(Layouts.MODULE.getFields(module1).getClassVariables());
            return null;
        });

        return classVariables;
    }

    @TruffleBoundary
    public static Object lookupClassVariable(DynamicObject module, final String name) {
        assert RubyGuards.isRubyModule(module);

        return classVariableLookup(module, module1 -> Layouts.MODULE.getFields(module1).getClassVariables().get(name));
    }

    @TruffleBoundary(throwsControlFlowException = true)
    public static void setClassVariable(final RubyContext context, DynamicObject module, final String name, final Object value, final Node currentNode) {
        assert RubyGuards.isRubyModule(module);
        ModuleFields moduleFields = Layouts.MODULE.getFields(module);
        moduleFields.checkFrozen(context, currentNode);
        SharedObjects.propagate(module, value);

        // if the cvar is not already defined we need to take lock and ensure there is only one
        // defined in the class tree
        if (!trySetClassVariable(module, name, value)) {
            synchronized (context.getClassVariableDefinitionLock()) {
                if (!trySetClassVariable(module, name, value)) {
                    moduleFields.getClassVariables().put(name, value);
                }
            }
        }
    }

    private static boolean trySetClassVariable(DynamicObject topModule, final String name, final Object value) {
        final DynamicObject found = classVariableLookup(topModule, module -> {
            final ModuleFields moduleFields = Layouts.MODULE.getFields(module);
            if (moduleFields.getClassVariables().replace(name, value) != null) {
                return module;
            } else {
                return null;
            }
        });
        return found != null;
    }

    @TruffleBoundary(throwsControlFlowException = true)
    public static Object removeClassVariable(ModuleFields moduleFields, RubyContext context, Node currentNode, String name) {
        moduleFields.checkFrozen(context, currentNode);

        final Object found = moduleFields.getClassVariables().remove(name);
        if (found == null) {
            throw new RaiseException(context.getCoreExceptions().nameErrorClassVariableNotDefined(name, moduleFields.rubyModuleObject, currentNode));
        }
        return found;
    }

    private static <R> R classVariableLookup(DynamicObject module, Function<DynamicObject, R> action) {
        CompilerAsserts.neverPartOfCompilation();

        // Look in the current module
        R result = action.apply(module);
        if (result != null) {
            return result;
        }

        // If singleton class of a module, check the attached module.
        if (RubyGuards.isRubyClass(module)) {
            DynamicObject klass = module;
            if (Layouts.CLASS.getIsSingleton(klass) && Layouts.MODULE.isModule(Layouts.CLASS.getAttached(klass))) {
                module = Layouts.CLASS.getAttached(klass);

                result = action.apply(module);
                if (result != null) {
                    return result;
                }
            }
        }

        // Look in ancestors
        for (DynamicObject ancestor : Layouts.MODULE.getFields(module).parentAncestors()) {
            result = action.apply(ancestor);
            if (result != null) {
                return result;
            }
        }

        return null;
    }

    public static boolean isMethodPrivateFromName(String name) {
        CompilerAsserts.neverPartOfCompilation();

        return (name.equals("initialize") || name.equals("initialize_copy") ||
                name.equals("initialize_clone") || name.equals("initialize_dup") ||
                name.equals("respond_to_missing?"));
    }

}
