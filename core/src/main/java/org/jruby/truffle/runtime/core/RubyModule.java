/*
 * Copyright (c) 2013 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.runtime.core;

import com.oracle.truffle.api.Assumption;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.*;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.frame.Frame;
import com.oracle.truffle.api.frame.FrameInstance;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.utilities.CyclicAssumption;
import org.jruby.runtime.Visibility;
import org.jruby.truffle.nodes.RubyNode;
import org.jruby.truffle.runtime.*;
import org.jruby.truffle.runtime.control.RaiseException;
import org.jruby.truffle.runtime.methods.RubyMethod;
import org.jruby.truffle.runtime.subsystems.ObjectSpaceManager;

import java.util.*;

/**
 * Represents the Ruby {@code Module} class.
 */
public class RubyModule extends RubyObject implements ModuleChain {

    /**
     * The slot within a module definition method frame where we store the implicit state that is
     * the current visibility for new methods.
     */
    public static final Object VISIBILITY_FRAME_SLOT_ID = new Object();

    /**
     * The slot within a module definition method frame where we store the implicit state that is
     * the flag for whether or not new methods will be module methods (functions is the term).
     */
    public static final Object MODULE_FUNCTION_FLAG_FRAME_SLOT_ID = new Object();

    // The context is stored here - objects can obtain it via their class (which is a module)
    private final RubyContext context;

    @CompilationFinal protected ModuleChain lexicalParentModule;
    @CompilationFinal protected ModuleChain parentModule;

    @CompilationFinal private String name;

    private final Map<String, RubyMethod> methods = new HashMap<>();
    private final Map<String, RubyConstant> constants = new HashMap<>();
    private final Map<String, Object> classVariables = new HashMap<>();

    private final CyclicAssumption unmodifiedAssumption;

    /**
     * Keep track of other modules that depend on the configuration of this module in some way. The
     * include subclasses and modules that include this module.
     */
    private final Set<ModuleChain> dependents = Collections.newSetFromMap(new WeakHashMap<ModuleChain, Boolean>());

    /**
     * The class from which we create the object that is {@code Module}. A subclass of
     * {@link RubyClass} so that we can override {@link RubyClass#newInstance(org.jruby.truffle.nodes.RubyNode)}} and allocate a
     * {@link RubyModule} rather than a normal {@link RubyBasicObject}.
     */
    public static class RubyModuleClass extends RubyClass {

        public RubyModuleClass(RubyContext context) {
            super(null, context, null, null, null, "Module");
        }

        @Override
        public RubyBasicObject newInstance(RubyNode currentNode) {
            return new RubyModule(this, null, "(unnamed module)");
        }

    }

    public RubyModule(RubyClass moduleClass, ModuleChain lexicalParentModule, String name) {
        this(moduleClass.getContext(), moduleClass, lexicalParentModule, name);
    }

    public RubyModule(RubyContext context, RubyClass moduleClass, ModuleChain lexicalParentModule, String name) {
        super(moduleClass);

        this.context = context;
        this.lexicalParentModule = lexicalParentModule;
        this.name = name;

        unmodifiedAssumption = new CyclicAssumption(name + " is unmodified");
    }

    public void initCopy(RubyModule other) {
        this.name = other.name;
        this.parentModule = other.parentModule;
        this.lexicalParentModule = other.lexicalParentModule;
        this.methods.putAll(other.methods);
        this.constants.putAll(other.constants);
        this.classVariables.putAll(other.classVariables);
    }

    public void include(Node currentNode, RubyModule module) {
        RubyNode.notDesignedForCompilation();

        checkFrozen(currentNode);
        parentModule = new IncludedModule(module, parentModule);
        newVersion();
        module.addDependent(this);
    }

    /**
     * Set the value of a constant, possibly redefining it.
     */
    public void setConstant(RubyNode currentNode, String constantName, Object value) {
        RubyNode.notDesignedForCompilation();

        assert RubyContext.shouldObjectBeVisible(value) : value.getClass();
        checkFrozen(currentNode);
        getConstants().put(constantName, new RubyConstant(value, false));
        newVersion();
        // TODO(CS): warn when redefining a constant
    }

    public void removeConstant(RubyNode currentNode, String data) {
        RubyNode.notDesignedForCompilation();

        checkFrozen(currentNode);
        getConstants().remove(data);
        newVersion();
    }

    public void removeClassVariable(RubyNode currentNode, String variableName) {
        RubyNode.notDesignedForCompilation();

        checkFrozen(currentNode);

        classVariables.remove(variableName);
    }

    public void setModuleConstant(RubyNode currentNode, String constantName, Object value) {
        RubyNode.notDesignedForCompilation();

        checkFrozen(currentNode);

        setConstant(currentNode, constantName, value);
        getSingletonClass(currentNode).setConstant(currentNode, constantName, value);
    }

    public void addMethod(RubyNode currentNode, RubyMethod method) {
        RubyNode.notDesignedForCompilation();

        assert method != null;
        assert getMethods() != null;

        checkFrozen(currentNode);
        getMethods().put(method.getName(), method);
        newVersion();
    }

    public void removeMethod(RubyNode currentNode, String methodName) {
        RubyNode.notDesignedForCompilation();

        checkFrozen(currentNode);

        getMethods().remove(methodName);
        newVersion();
    }

    public void undefMethod(RubyNode currentNode, String methodName) {
        RubyNode.notDesignedForCompilation();
        final RubyMethod method = ModuleOperations.lookupMethod(this, methodName);
        if (method == null) {
            throw new UnsupportedOperationException();
        } else {
            undefMethod(currentNode, ModuleOperations.lookupMethod(this, methodName));
        }
    }

    public void undefMethod(RubyNode currentNode, RubyMethod method) {
        RubyNode.notDesignedForCompilation();
        addMethod(currentNode, method.undefined());
    }

    public void alias(RubyNode currentNode, String newName, String oldName) {
        RubyNode.notDesignedForCompilation();

        final RubyMethod method = ModuleOperations.lookupMethod(this, oldName);

        if (method == null) {
            CompilerDirectives.transferToInterpreter();
            throw new RaiseException(getContext().getCoreLibrary().noMethodError(oldName, getName(), currentNode));
        }

        addMethod(currentNode, method.withNewName(newName));
    }

    public void changeConstantVisibility(RubyNode currentNode, RubySymbol constant, boolean isPrivate) {
        RubyNode.notDesignedForCompilation();

        RubyConstant rubyConstant = ModuleOperations.lookupConstant(this, constant.toString());
        checkFrozen(currentNode);

        if (rubyConstant != null) {
            rubyConstant.setPrivate(isPrivate);
        } else {
            throw new RaiseException(context.getCoreLibrary().nameErrorUninitializedConstant(constant.toString(), currentNode));
        }

        newVersion();
    }

    public void appendFeatures(RubyNode currentNode, RubyModule other) {
        RubyNode.notDesignedForCompilation();

        // TODO(CS): check only run once

        for (Map.Entry<String, RubyConstant> constantEntry : getConstants().entrySet()) {
            final String constantName = constantEntry.getKey();
            final Object constantValue = constantEntry.getValue().getValue();
            other.setModuleConstant(currentNode, constantName, constantValue);
        }

        for (Map.Entry<String, RubyMethod> methodEntry : getMethods().entrySet()) {
            final String methodName = methodEntry.getKey();
            final RubyMethod method = methodEntry.getValue();
            other.addMethod(currentNode, method.withNewName(methodName));
        }
    }

    public RubyContext getContext() {
        return context;
    }

    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return super.toString() + "(" + name + ")";
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }

    public void newVersion() {
        RubyNode.notDesignedForCompilation();

        unmodifiedAssumption.invalidate();

        // Make dependents new versions

        for (ModuleChain dependent : dependents) {
            dependent.newVersion();
        }

        // TODO(CS): is the lexical child a dependent?
    }

    public void addDependent(ModuleChain dependent) {
        RubyNode.notDesignedForCompilation();

        dependents.add(dependent);
    }

    public Assumption getUnmodifiedAssumption() {
        RubyNode.notDesignedForCompilation();

        return unmodifiedAssumption.getAssumption();
    }

    public static void setCurrentVisibility(Visibility visibility) {
        RubyNode.notDesignedForCompilation();

        final Frame callerFrame = Truffle.getRuntime().getCallerFrame().getFrame(FrameInstance.FrameAccess.READ_WRITE, false);

        assert callerFrame != null;
        assert callerFrame.getFrameDescriptor() != null;

        final FrameSlot visibilitySlot = callerFrame.getFrameDescriptor().findFrameSlot(VISIBILITY_FRAME_SLOT_ID);
        assert visibilitySlot != null;

        callerFrame.setObject(visibilitySlot, visibility);
    }

    public void visibilityMethod(RubyNode currentNode, Object[] arguments, Visibility visibility) {
        RubyNode.notDesignedForCompilation();

        if (arguments.length == 0) {
            setCurrentVisibility(visibility);
        } else {
            for (Object arg : arguments) {
                final String methodName;

                if (arg instanceof RubySymbol) {
                    methodName = ((RubySymbol) arg).toString();
                } else {
                    throw new UnsupportedOperationException();
                }

                final RubyMethod method = ModuleOperations.lookupMethod(this, methodName);

                if (method == null) {
                    throw new RuntimeException("Couldn't find method " + arg.toString());
                }

                /*
                 * If the method was already defined in this class, that's fine {@link addMethod}
                 * will overwrite it, otherwise we do actually want to add a copy of the method with
                 * a different visibility to this module.
                 */

                addMethod(currentNode, method.withNewVisibility(visibility));
            }
        }
    }

    public void moduleEval(RubyNode currentNode, String source) {
        RubyNode.notDesignedForCompilation();

        getContext().eval(source, this, currentNode);
    }

    public Map<String, RubyConstant> getConstants() {
        return constants;
    }

    public Map<String, RubyMethod> getMethods() {
        return methods;
    }

    @Override
    public Map<String, Object> getClassVariables() {
        return classVariables;
    }

    @Override
    public void visitObjectGraphChildren(ObjectSpaceManager.ObjectGraphVisitor visitor) {
        for (RubyConstant constant : constants.values()) {
            getContext().getCoreLibrary().box(constant.getValue()).visitObjectGraph(visitor);
        }

        for (RubyMethod method : methods.values()) {
            if (method.getDeclarationFrame() != null) {
                getContext().getObjectSpaceManager().visitFrame(method.getDeclarationFrame(), visitor);
            }
        }
    }

    public ModuleChain getLexicalParentModule() {
        return lexicalParentModule;
    }

    public ModuleChain getParentModule() {
        return parentModule;
    }

    public RubyModule getActualModule() {
        return this;
    }

}
