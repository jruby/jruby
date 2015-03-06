/*
 * Copyright (c) 2013, 2015 Oracle and/or its affiliates. All rights reserved. This
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
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.frame.Frame;
import com.oracle.truffle.api.frame.FrameInstance;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.FrameSlotKind;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.utilities.CyclicAssumption;

import org.jruby.runtime.Visibility;
import org.jruby.truffle.nodes.RubyNode;
import org.jruby.truffle.nodes.objects.Allocator;
import org.jruby.truffle.runtime.*;
import org.jruby.truffle.runtime.control.RaiseException;
import org.jruby.truffle.runtime.methods.InternalMethod;
import org.jruby.truffle.runtime.subsystems.ObjectSpaceManager;

import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Represents the Ruby {@code Module} class.
 */
public class RubyModule extends RubyBasicObject implements ModuleChain {

    /**
     * A reference to an included RubyModule.
     */
    private static class IncludedModule implements ModuleChain {
        private final RubyModule includedModule;
        private final ModuleChain parentModule;

        public IncludedModule(RubyModule includedModule, ModuleChain parentModule) {
            this.includedModule = includedModule;
            this.parentModule = parentModule;
        }

        @Override
        public ModuleChain getParentModule() {
            return parentModule;
        }

        @Override
        public RubyModule getActualModule() {
            return includedModule;
        }

        @Override
        public String toString() {
            return super.toString() + "(" + includedModule + ")";
        }
    }

    public static void debugModuleChain(RubyModule module) {
        ModuleChain chain = module;
        while (chain != null) {
            System.err.print(chain.getClass());

            RubyModule real = chain.getActualModule();
            System.err.print(" " + real.getName());

            System.err.println();
            chain = chain.getParentModule();
        }
    }

    /**
     * The slot within a module definition method frame where we store the implicit state that is
     * the current visibility for new methods.
     */
    public static final Object VISIBILITY_FRAME_SLOT_ID = new Object();

    // The context is stored here - objects can obtain it via their class (which is a module)
    private final RubyContext context;

    @CompilationFinal protected ModuleChain parentModule;

    private String name;

    private final Map<String, InternalMethod> methods = new ConcurrentHashMap<>();
    private final Map<String, RubyConstant> constants = new ConcurrentHashMap<>();
    private final Map<String, Object> classVariables = new ConcurrentHashMap<>();

    private final CyclicAssumption unmodifiedAssumption;

    /**
     * Keep track of other modules that depend on the configuration of this module in some way. The
     * include subclasses and modules that include this module.
     */
    private final Set<RubyModule> dependents = Collections.newSetFromMap(new WeakHashMap<RubyModule, Boolean>());
    /**
     * Lexical dependent modules, to take care of changes to a module constants.
     */
    private final Set<RubyModule> lexicalDependents = Collections.newSetFromMap(new WeakHashMap<RubyModule, Boolean>());

    public RubyModule(RubyContext context, RubyModule lexicalParent, String name, Node currentNode) {
        this(context, context.getCoreLibrary().getModuleClass(), lexicalParent, name, currentNode);
    }

    protected RubyModule(RubyContext context, RubyClass selfClass, RubyModule lexicalParent, String name, Node currentNode) {
        super(selfClass, context);
        this.context = context;

        unmodifiedAssumption = new CyclicAssumption(name + " is unmodified");

        if (lexicalParent != null) {
            getAdoptedByLexicalParent(lexicalParent, name, currentNode);
        }
    }

    public void getAdoptedByLexicalParent(RubyModule lexicalParent, String name, Node currentNode) {
        lexicalParent.setConstantInternal(currentNode, name, this, false);
        lexicalParent.addLexicalDependent(this);

        if (this.name == null) {
            // Tricky, we need to compare with the Object class, but we only have a Class at hand.
            RubyClass classClass = logicalClass.getLogicalClass();
            RubyClass objectClass = classClass.getSuperClass().getSuperClass();

            if (lexicalParent == objectClass) {
                this.name = name;
                updateAnonymousChildrenModules();
            } else if (lexicalParent.hasName()) {
                this.name = lexicalParent.getName() + "::" + name;
                updateAnonymousChildrenModules();
            }
            // else: Our lexicalParent is also an anonymous module
            // and will name us when it gets named via updateAnonymousChildrenModules()
        }
    }

    private void updateAnonymousChildrenModules() {
        RubyNode.notDesignedForCompilation("2d9f64b69c8d4adc84cd28c7229b049e");

        for (Entry<String, RubyConstant> entry : constants.entrySet()) {
            RubyConstant constant = entry.getValue();
            if (constant.getValue() instanceof RubyModule) {
                RubyModule module = (RubyModule) constant.getValue();
                if (!module.hasName()) {
                    module.getAdoptedByLexicalParent(this, entry.getKey(), null);
                }
            }
        }
    }

    @TruffleBoundary
    public void initCopy(RubyModule other) {
        // Do not copy name, the copy is an anonymous module
        this.parentModule = other.parentModule;
        this.methods.putAll(other.methods);
        this.constants.putAll(other.constants);
        this.classVariables.putAll(other.classVariables);
    }

    /** If this instance is a module and not a class. */
    public boolean isOnlyAModule() {
        return !(this instanceof RubyClass);
    }

    @TruffleBoundary
    public void include(Node currentNode, RubyModule module) {
        RubyNode.notDesignedForCompilation("fb601538ced148a29447c1980a9f0e25");

        checkFrozen(currentNode);

        // We need to traverse the module chain in reverse order
        Stack<RubyModule> moduleAncestors = new Stack<>();
        for (RubyModule ancestor : module.ancestors()) {
            moduleAncestors.push(ancestor);
        }

        while (!moduleAncestors.isEmpty()) {
            RubyModule mod = moduleAncestors.pop();
            parentModule = new IncludedModule(mod, parentModule);
            mod.addDependent(this);
        }
        newVersion();
    }

    /**
     * Set the value of a constant, possibly redefining it.
     */
    @TruffleBoundary
    public void setConstant(Node currentNode, String name, Object value) {
        if (value instanceof RubyModule) {
            ((RubyModule) value).getAdoptedByLexicalParent(this, name, currentNode);
        } else {
            setConstantInternal(currentNode, name, value, false);
        }
    }

    @TruffleBoundary
    public void setAutoloadConstant(Node currentNode, String name, RubyString filename) {
        setConstantInternal(currentNode, name, filename, true);
    }

    private void setConstantInternal(Node currentNode, String name, Object value, boolean autoload) {
        RubyNode.notDesignedForCompilation("19542d6d685b4fa48280d774e28589af");

        checkFrozen(currentNode);

        RubyConstant previous = constants.get(name);
        if (previous == null) {
            constants.put(name, new RubyConstant(this, value, false, autoload));
        } else {
            // TODO(CS): warn when redefining a constant
            // TODO (nirvdrum 18-Feb-15): But don't warn when redefining an autoloaded constant.
            constants.put(name, new RubyConstant(this, value, previous.isPrivate(), autoload));
        }

        newLexicalVersion();
    }

    @TruffleBoundary
    public RubyConstant removeConstant(Node currentNode, String name) {
        RubyNode.notDesignedForCompilation("838fb230b46b47899680bb9da1b3b42d");

        checkFrozen(currentNode);
        RubyConstant oldConstant = constants.remove(name);
        newLexicalVersion();
        return oldConstant;
    }

    @TruffleBoundary
    public void setClassVariable(Node currentNode, String variableName, Object value) {
        RubyNode.notDesignedForCompilation("e5e58e513ffb47c983a7850dda99741a");

        checkFrozen(currentNode);

        classVariables.put(variableName, value);
    }

    @TruffleBoundary
    public void removeClassVariable(Node currentNode, String variableName) {
        RubyNode.notDesignedForCompilation("98b79a2639124c8dbd48486d77302b8f");

        checkFrozen(currentNode);

        classVariables.remove(variableName);
    }

    @TruffleBoundary
    public void addMethod(Node currentNode, InternalMethod method) {
        RubyNode.notDesignedForCompilation("14fea16da3744bfc8cbe09098f761101");

        assert method != null;

        checkFrozen(currentNode);
        methods.put(method.getName(), method.withDeclaringModule(this));
        newVersion();
    }

    @TruffleBoundary
    public void removeMethod(Node currentNode, String methodName) {
        RubyNode.notDesignedForCompilation("0ca00262894042e0a6ba556639f2a2a8");

        checkFrozen(currentNode);

        methods.remove(methodName);
        newVersion();
    }

    @TruffleBoundary
    public void undefMethod(Node currentNode, String methodName) {
        RubyNode.notDesignedForCompilation("a984af057e624742a02bb8b2e5cb5b4d");
        final InternalMethod method = ModuleOperations.lookupMethod(this, methodName);
        if (method == null) {
            throw new UnsupportedOperationException();
        } else {
            undefMethod(currentNode, method);
        }
    }

    @TruffleBoundary
    public void undefMethod(Node currentNode, InternalMethod method) {
        RubyNode.notDesignedForCompilation("44295f2e1bba4ebea3297d0c5a2c7d04");
        addMethod(currentNode, method.undefined());
    }

    /**
     * Also searches on Object for modules.
     * Used for alias_method, visibility changes, etc.
     */
    @TruffleBoundary
    private InternalMethod deepMethodSearch(String name) {
        InternalMethod method = ModuleOperations.lookupMethod(this, name);

        // Also search on Object if we are a Module. JRuby calls it deepMethodSearch().
        if (method == null && isOnlyAModule()) { // TODO: handle undefined methods
            method = ModuleOperations.lookupMethod(context.getCoreLibrary().getObjectClass(), name);
        }

        return method;
    }

    @TruffleBoundary
    public void alias(Node currentNode, String newName, String oldName) {
        RubyNode.notDesignedForCompilation("206a8325a9a846228fe4951534067ce6");

        InternalMethod method = deepMethodSearch(oldName);

        if (method == null) {
            CompilerDirectives.transferToInterpreter();
            throw new RaiseException(getContext().getCoreLibrary().noMethodError(oldName, this, currentNode));
        }

        addMethod(currentNode, method.withNewName(newName));
    }

    @TruffleBoundary
    public void changeConstantVisibility(Node currentNode, RubySymbol constant, boolean isPrivate) {
        RubyNode.notDesignedForCompilation("f56c670b1e174fd28d6c1f2b754bd201");

        RubyConstant rubyConstant = ModuleOperations.lookupConstant(getContext(), LexicalScope.NONE, this, constant.toString());
        checkFrozen(currentNode);

        if (rubyConstant != null) {
            rubyConstant.setPrivate(isPrivate);
            newLexicalVersion();
        } else {
            throw new RaiseException(context.getCoreLibrary().nameErrorUninitializedConstant(this, constant.toString(), currentNode));
        }
    }

    @TruffleBoundary
    public void appendFeatures(Node currentNode, RubyModule other) {
        RubyNode.notDesignedForCompilation("4aeeb4829438414f9a5425aa995e395c");

        // TODO(CS): check only run once
        other.include(currentNode, this);
    }

    public RubyContext getContext() {
        return context;
    }

    public String getName() {
        if (name != null) {
            return name;
        } else {
            return "#<" + logicalClass.getName() + ":0x" + Long.toHexString(getObjectID()) + ">";
        }
    }

    public boolean hasName() {
        return name != null;
    }

    @Override
    public String toString() {
        return super.toString() + "(" + name + ")";
    }

    public void newVersion() {
        RubyNode.notDesignedForCompilation("72e30b43cbd44caaadd3ad833dddc2d3");

        newVersion(new HashSet<RubyModule>(), false);
    }

    public void newLexicalVersion() {
        RubyNode.notDesignedForCompilation("ddf723a763d945f4b9d85937b338ad9d");

        newVersion(new HashSet<RubyModule>(), true);
    }

    private void newVersion(Set<RubyModule> alreadyInvalidated, boolean considerLexicalDependents) {
        if (alreadyInvalidated.contains(this))
            return;

        unmodifiedAssumption.invalidate();
        alreadyInvalidated.add(this);

        // Make dependents new versions
        for (RubyModule dependent : dependents) {
            dependent.newVersion(alreadyInvalidated, considerLexicalDependents);
        }

        if (considerLexicalDependents) {
            for (RubyModule dependent : lexicalDependents) {
                dependent.newVersion(alreadyInvalidated, considerLexicalDependents);
            }
        }
    }

    public void addDependent(RubyModule dependent) {
        RubyNode.notDesignedForCompilation("e223f65ac4f84e6b8c6173ecb8e366fb");

        dependents.add(dependent);
    }

    public void addLexicalDependent(RubyModule lexicalChild) {
        RubyNode.notDesignedForCompilation("b851a4d2cef9449398cbad4aa7399add");

        if (lexicalChild != this)
            lexicalDependents.add(lexicalChild);
    }

    public Assumption getUnmodifiedAssumption() {
        RubyNode.notDesignedForCompilation("2feca56bc3ac40f6a5e9bb2b0b20912f");

        return unmodifiedAssumption.getAssumption();
    }

    public static void setCurrentVisibility(Visibility visibility) {
        RubyNode.notDesignedForCompilation("36eca2a85e584df49043d0303de6f18d");

        final Frame callerFrame = Truffle.getRuntime().getCallerFrame().getFrame(FrameInstance.FrameAccess.READ_WRITE, false);

        assert callerFrame != null;
        assert callerFrame.getFrameDescriptor() != null;

        final FrameSlot visibilitySlot = callerFrame.getFrameDescriptor().findOrAddFrameSlot(
                RubyModule.VISIBILITY_FRAME_SLOT_ID, "visibility for frame", FrameSlotKind.Object);

        callerFrame.setObject(visibilitySlot, visibility);
    }

    public void visibilityMethod(Node currentNode, Object[] arguments, Visibility visibility) {
        RubyNode.notDesignedForCompilation("4f9cf35573a64c3fba0aeb033e678361");

        if (arguments.length == 0) {
            setCurrentVisibility(visibility);
        } else {
            for (Object arg : arguments) {
                final String methodName;

                if (arg instanceof RubySymbol) {
                    methodName = ((RubySymbol) arg).toString();
                } else if (arg instanceof RubyString) {
                    methodName = ((RubyString) arg).toString();
                } else {
                    throw new UnsupportedOperationException();
                }

                final InternalMethod method = deepMethodSearch(methodName);

                if (method == null) {
                    throw new RuntimeException("Couldn't find method " + arg.toString());
                }

                /*
                 * If the method was already defined in this class, that's fine
                 * {@link addMethod} will overwrite it, otherwise we do actually
                 * want to add a copy of the method with a different visibility
                 * to this module.
                 */
                if (visibility == Visibility.MODULE_FUNCTION) {
                    addMethod(currentNode, method.withVisibility(Visibility.PRIVATE));
                    getSingletonClass(currentNode).addMethod(currentNode, method.withVisibility(Visibility.PUBLIC));
                } else {
                    addMethod(currentNode, method.withVisibility(visibility));
                }
            }
        }
    }

    public Map<String, RubyConstant> getConstants() {
        return constants;
    }

    public Map<String, InternalMethod> getMethods() {
        return methods;
    }

    public Map<String, Object> getClassVariables() {
        return classVariables;
    }

    @Override
    public void visitObjectGraphChildren(ObjectSpaceManager.ObjectGraphVisitor visitor) {
        for (RubyConstant constant : constants.values()) {
            if (constant.getValue() instanceof RubyBasicObject) {
                ((RubyBasicObject) constant.getValue()).visitObjectGraph(visitor);
            }
        }

        for (Object classVariable : classVariables.values()) {
            if (classVariable instanceof RubyBasicObject) {
                ((RubyBasicObject) classVariable).visitObjectGraph(visitor);
            }
        }

        for (InternalMethod method : methods.values()) {
            if (method.getDeclarationFrame() != null) {
                getContext().getObjectSpaceManager().visitFrame(method.getDeclarationFrame(), visitor);
            }
        }

        for (RubyModule ancestor : ancestors()) {
            ancestor.visitObjectGraph(visitor);
        }
    }

    public ModuleChain getParentModule() {
        return parentModule;
    }

    public RubyModule getActualModule() {
        return this;
    }

    private class AncestorIterator implements Iterator<RubyModule> {
        ModuleChain module;

        public AncestorIterator(ModuleChain top) {
            module = top;
        }

        @Override
        public boolean hasNext() {
            return module != null;
        }

        @Override
        public RubyModule next() throws NoSuchElementException {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }
            ModuleChain mod = module;
            module = module.getParentModule();
            return mod.getActualModule();
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException("remove");
        }
    }

    private class IncludedModulesIterator extends AncestorIterator {
        public IncludedModulesIterator(ModuleChain top) {
            super(top);
        }

        @Override
        public boolean hasNext() {
            return super.hasNext() && !(module instanceof RubyClass);
        }
    }

    public Iterable<RubyModule> ancestors() {
        final RubyModule top = this;
        return new Iterable<RubyModule>() {
            @Override
            public Iterator<RubyModule> iterator() {
                return new AncestorIterator(top);
            }
        };
    }

    public Iterable<RubyModule> parentAncestors() {
        final ModuleChain top = parentModule;
        return new Iterable<RubyModule>() {
            @Override
            public Iterator<RubyModule> iterator() {
                return new AncestorIterator(top);
            }
        };
    }

    public Iterable<RubyModule> includedModules() {
        final ModuleChain top = parentModule;
        return new Iterable<RubyModule>() {
            @Override
            public Iterator<RubyModule> iterator() {
                return new IncludedModulesIterator(top);
            }
        };
    }

    public static class ModuleAllocator implements Allocator {

        @Override
        public RubyBasicObject allocate(RubyContext context, RubyClass rubyClass, Node currentNode) {
            return new RubyModule(context, null, null, currentNode);
        }

    }

}
