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
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.utilities.CyclicAssumption;
import org.jruby.runtime.Visibility;
import org.jruby.truffle.nodes.RubyGuards;
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
        @CompilationFinal private ModuleChain parentModule;

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

        @Override
        public void insertAfter(RubyModule module) {
            parentModule = new IncludedModule(module, parentModule);
        }
    }

    private static class PrependMarker implements ModuleChain {
        @CompilationFinal private ModuleChain parentModule;

        public PrependMarker(ModuleChain parentModule) {
            this.parentModule = parentModule;
        }

        @Override
        public ModuleChain getParentModule() {
            return parentModule;
        }

        @Override
        public RubyModule getActualModule() {
            throw new UnsupportedOperationException();
        }

        @Override
        public void insertAfter(RubyModule module) {
            parentModule = new IncludedModule(module, parentModule);
        }

    }

    public static void debugModuleChain(RubyModule module) {
        ModuleChain chain = module;
        while (chain != null) {
            System.err.print(chain.getClass());
            if (!(chain instanceof PrependMarker)) {
                RubyModule real = chain.getActualModule();
                System.err.print(" " + real.getName());
            }
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

    protected final ModuleChain start = new PrependMarker(this);
    @CompilationFinal protected ModuleChain parentModule;

    private final RubyModule lexicalParent;
    private final String givenBaseName;
    /** Full name, including named parent */
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

    public RubyModule(RubyContext context, RubyClass selfClass, RubyModule lexicalParent, String name, Node currentNode) {
        super(context, selfClass);
        this.context = context;
        this.lexicalParent = lexicalParent;
        this.givenBaseName = name;

        unmodifiedAssumption = new CyclicAssumption(name + " is unmodified");

        if (lexicalParent == null) { // bootstrap or anonymous module
            this.name = givenBaseName;
        } else {
            getAdoptedByLexicalParent(lexicalParent, name, currentNode);
        }
    }

    public void getAdoptedByLexicalParent(RubyModule lexicalParent, String name, Node currentNode) {
        lexicalParent.setConstantInternal(currentNode, name, this, false);
        lexicalParent.addLexicalDependent(this);

        if (this.name == null) {
            // Tricky, we need to compare with the Object class, but we only have a Class at hand.
            RubyClass classClass = getLogicalClass().getLogicalClass();
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
    public void initCopy(RubyModule from) {
        // Do not copy name, the copy is an anonymous module
        this.methods.putAll(from.methods);
        this.constants.putAll(from.constants);
        this.classVariables.putAll(from.classVariables);

        if (from.start.getParentModule() != from) {
            this.parentModule = from.start.getParentModule();
        } else {
            this.parentModule = from.parentModule;
        }

        for (RubyModule ancestor : from.ancestors()) {
            ancestor.addDependent(this);
        }
    }

    /** If this instance is a module and not a class. */
    public boolean isOnlyAModule() {
        return !(this instanceof RubyClass);
    }

    // TODO (eregon, 12 May 2015): ideally all callers would be nodes and check themselves.
    private void checkFrozen(Node currentNode) {
        if (getContext().getCoreLibrary() != null && DebugOperations.verySlowIsFrozen(getContext(), this)) {
            CompilerDirectives.transferToInterpreter();
            throw new RaiseException(getContext().getCoreLibrary().frozenError(getLogicalClass().getName(), currentNode));
        }
    }

    @Override
    public void insertAfter(RubyModule module) {
        parentModule = new IncludedModule(module, parentModule);
    }

    @TruffleBoundary
    public void include(Node currentNode, RubyModule module) {
        checkFrozen(currentNode);

        // If the module we want to include already includes us, it is cyclic
        if (ModuleOperations.includesModule(module, this)) {
            throw new RaiseException(getContext().getCoreLibrary().argumentError("cyclic include detected", currentNode));
        }

        // We need to include the module ancestors in reverse order for a given inclusionPoint
        ModuleChain inclusionPoint = this;
        Stack<RubyModule> modulesToInclude = new Stack<>();
        for (RubyModule ancestor : module.ancestors()) {
            if (ModuleOperations.includesModule(this, ancestor)) {
                if (isIncludedModuleBeforeSuperClass(ancestor)) {
                    // Include the modules at the appropriate inclusionPoint
                    performIncludes(inclusionPoint, modulesToInclude);
                    assert modulesToInclude.isEmpty();

                    // We need to include the others after that module
                    inclusionPoint = parentModule;
                    while (inclusionPoint.getActualModule() != ancestor) {
                        inclusionPoint = inclusionPoint.getParentModule();
                    }
                } else {
                    // Just ignore this module, as it is included above the superclass
                }
            } else {
                modulesToInclude.push(ancestor);
            }
        }

        performIncludes(inclusionPoint, modulesToInclude);

        newVersion();
    }

    private void performIncludes(ModuleChain inclusionPoint, Stack<RubyModule> moduleAncestors) {
        while (!moduleAncestors.isEmpty()) {
            RubyModule mod = moduleAncestors.pop();
            inclusionPoint.insertAfter(mod);
            mod.addDependent(this);
        }
    }

    private boolean isIncludedModuleBeforeSuperClass(RubyModule module) {
        ModuleChain included = parentModule;
        while (included instanceof IncludedModule) {
            if (included.getActualModule() == module) {
                return true;
            }
            included = included.getParentModule();
        }
        return false;
    }

    @TruffleBoundary
    public void prepend(Node currentNode, RubyModule module) {
        checkFrozen(currentNode);

        // If the module we want to prepend already includes us, it is cyclic
        if (ModuleOperations.includesModule(module, this)) {
            throw new RaiseException(getContext().getCoreLibrary().argumentError("cyclic prepend detected", currentNode));
        }

        ModuleChain mod = module.start;
        ModuleChain cur = start;
        while (mod != null && !(mod instanceof RubyClass)) {
            if (!(mod instanceof PrependMarker)) {
                if (!ModuleOperations.includesModule(this, mod.getActualModule())) {
                    cur.insertAfter(mod.getActualModule());
                    mod.getActualModule().addDependent(this);
                    cur = cur.getParentModule();
                }
            }
            mod = mod.getParentModule();
        }

        newVersion();
    }

    /**
     * Set the value of a constant, possibly redefining it.
     */
    @TruffleBoundary
    public void setConstant(Node currentNode, String name, Object value) {
        if (getContext().getCoreLibrary().isLoadingRubyCore()) {
            final RubyConstant currentConstant = constants.get(name);

            if (currentConstant != null) {
                return;
            }
        }

        if (value instanceof RubyModule) {
            ((RubyModule) value).getAdoptedByLexicalParent(this, name, currentNode);
        } else {
            setConstantInternal(currentNode, name, value, false);
        }
    }

    @TruffleBoundary
    public void setAutoloadConstant(Node currentNode, String name, RubyBasicObject filename) {
        assert RubyGuards.isRubyString(filename);
        setConstantInternal(currentNode, name, filename, true);
    }

    private void setConstantInternal(Node currentNode, String name, Object value, boolean autoload) {
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
        checkFrozen(currentNode);
        RubyConstant oldConstant = constants.remove(name);
        newLexicalVersion();
        return oldConstant;
    }

    @TruffleBoundary
    public void setClassVariable(Node currentNode, String variableName, Object value) {
        checkFrozen(currentNode);

        classVariables.put(variableName, value);
    }

    @TruffleBoundary
    public Object removeClassVariable(Node currentNode, String name) {
        checkFrozen(currentNode);

        final Object found = classVariables.remove(name);
        if (found == null) {
            CompilerDirectives.transferToInterpreter();
            throw new RaiseException(context.getCoreLibrary().nameErrorClassVariableNotDefined(name, this, currentNode));
        }
        return found;
    }

    @TruffleBoundary
    public void addMethod(Node currentNode, InternalMethod method) {
        assert method != null;

        if (getContext().getCoreLibrary().isLoadingRubyCore()) {
            final InternalMethod currentMethod = methods.get(method.getName());

            if (currentMethod != null && currentMethod.getSharedMethodInfo().getSourceSection() instanceof CoreSourceSection) {
                return;
            }
        }

        checkFrozen(currentNode);
        methods.put(method.getName(), method.withDeclaringModule(this));
        newVersion();

        if (context.getCoreLibrary().isLoaded() && !method.isUndefined()) {
            DebugOperations.send(context, this, "method_added", null, context.getSymbolTable().getSymbol(method.getName()));
        }
    }

    @TruffleBoundary
    public void removeMethod(String methodName) {
        methods.remove(methodName);
        newVersion();
    }

    @TruffleBoundary
    public void undefMethod(Node currentNode, String methodName) {
        final InternalMethod method = ModuleOperations.lookupMethod(this, methodName);
        if (method == null) {
            throw new UnsupportedOperationException();
        } else {
            undefMethod(currentNode, method);
        }
    }

    @TruffleBoundary
    public void undefMethod(Node currentNode, InternalMethod method) {
        addMethod(currentNode, method.undefined());
    }

    /**
     * Also searches on Object for modules.
     * Used for alias_method, visibility changes, etc.
     */
    @TruffleBoundary
    public InternalMethod deepMethodSearch(String name) {
        InternalMethod method = ModuleOperations.lookupMethod(this, name);
        if (method != null && !method.isUndefined()) {
            return method;
        }

        // Also search on Object if we are a Module. JRuby calls it deepMethodSearch().
        if (isOnlyAModule()) { // TODO: handle undefined methods
            method = ModuleOperations.lookupMethod(context.getCoreLibrary().getObjectClass(), name);

            if (method != null && !method.isUndefined()) {
                return method;
            }
        }

        return null;
    }

    @TruffleBoundary
    public void alias(Node currentNode, String newName, String oldName) {
        InternalMethod method = deepMethodSearch(oldName);

        if (method == null) {
            CompilerDirectives.transferToInterpreter();
            throw new RaiseException(getContext().getCoreLibrary().noMethodErrorOnModule(oldName, this, currentNode));
        }

        InternalMethod aliasMethod = method.withName(newName);

        if (ModuleOperations.isMethodPrivateFromName(newName)) {
            aliasMethod = aliasMethod.withVisibility(Visibility.PRIVATE);
        }

        addMethod(currentNode, aliasMethod);
    }

    @TruffleBoundary
    public void changeConstantVisibility(Node currentNode, String name, boolean isPrivate) {
        checkFrozen(currentNode);
        RubyConstant rubyConstant = constants.get(name);

        if (rubyConstant != null) {
            rubyConstant.setPrivate(isPrivate);
            newLexicalVersion();
        } else {
            throw new RaiseException(context.getCoreLibrary().nameErrorUninitializedConstant(this, name, currentNode));
        }
    }

    public RubyContext getContext() {
        return context;
    }

    public String getName() {
        if (name != null) {
            return name;
        } else {
            CompilerDirectives.transferToInterpreter();
            if (givenBaseName != null) {
                return lexicalParent.getName() + "::" + givenBaseName;
            } else if (getLogicalClass() == this) { // For the case of class Class during initialization
                return "#<cyclic>";
            } else {
                return "#<" + getLogicalClass().getName() + ":0x" + Long.toHexString(verySlowGetObjectID()) + ">";
            }
        }
    }

    public boolean hasName() {
        return name != null;
    }

    public boolean hasPartialName() {
        return hasName() || givenBaseName != null;
    }

    @Override
    public String toString() {
        return super.toString() + "(" + getName() + ")";
    }

    public void newVersion() {
        newVersion(new HashSet<RubyModule>(), false);
    }

    public void newLexicalVersion() {
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
        dependents.add(dependent);
    }

    public void addLexicalDependent(RubyModule lexicalChild) {
        if (lexicalChild != this)
            lexicalDependents.add(lexicalChild);
    }

    public Assumption getUnmodifiedAssumption() {
        return unmodifiedAssumption.getAssumption();
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

    @Override
    public ModuleChain getParentModule() {
        return parentModule;
    }

    @Override
    public RubyModule getActualModule() {
        return this;
    }

    private static class AncestorIterator implements Iterator<RubyModule> {
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
            if (mod instanceof PrependMarker) {
                mod = mod.getParentModule();
            }

            module = mod.getParentModule();

            return mod.getActualModule();
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException("remove");
        }
    }

    private static class IncludedModulesIterator extends AncestorIterator {
        private final RubyModule currentModule;

        public IncludedModulesIterator(ModuleChain top, RubyModule currentModule) {
            super(top instanceof PrependMarker ? top.getParentModule() : top);
            this.currentModule = currentModule;
        }

        @Override
        public boolean hasNext() {
            if (!super.hasNext()) {
                return false;
            }

            if (module == currentModule) {
                module = module.getParentModule(); // skip self
                return hasNext();
            }

            return module instanceof IncludedModule;
        }
    }

    public Iterable<RubyModule> ancestors() {
        final ModuleChain top = start;
        return new Iterable<RubyModule>() {
            @Override
            public Iterator<RubyModule> iterator() {
                return new AncestorIterator(top);
            }
        };
    }

    public Iterable<RubyModule> parentAncestors() {
        final ModuleChain top = start;
        return new Iterable<RubyModule>() {
            @Override
            public Iterator<RubyModule> iterator() {
                final AncestorIterator iterator = new AncestorIterator(top);
                if (iterator.hasNext()) {
                    iterator.next();
                }
                return iterator;
            }
        };
    }

    /** Iterates over include'd and prepend'ed modules. */
    public Iterable<RubyModule> prependedAndIncludedModules() {
        final ModuleChain top = start;
        final RubyModule currentModule = this;
        return new Iterable<RubyModule>() {
            @Override
            public Iterator<RubyModule> iterator() {
                return new IncludedModulesIterator(top, currentModule);
            }
        };
    }

    public static interface MethodFilter {

        public static final MethodFilter PUBLIC = new RubyModule.MethodFilter() {
            @Override
            public boolean filter(InternalMethod method) {
                return method.getVisibility() == Visibility.PUBLIC;
            }
        };

        public static final MethodFilter PUBLIC_PROTECTED = new RubyModule.MethodFilter() {
            @Override
            public boolean filter(InternalMethod method) {
                return method.getVisibility() == Visibility.PUBLIC ||
                        method.getVisibility() == Visibility.PROTECTED;
            }
        };

        public static final MethodFilter PROTECTED = new RubyModule.MethodFilter() {
            @Override
            public boolean filter(InternalMethod method) {
                return method.getVisibility() == Visibility.PROTECTED;
            }
        };

        public static final MethodFilter PRIVATE = new RubyModule.MethodFilter() {
            @Override
            public boolean filter(InternalMethod method) {
                return method.getVisibility() == Visibility.PRIVATE;
            }
        };

        boolean filter(InternalMethod method);

    }

    public Collection<RubyBasicObject> filterMethods(boolean includeAncestors, MethodFilter filter) {
        final Map<String, InternalMethod> allMethods;
        if (includeAncestors) {
            allMethods = ModuleOperations.getAllMethods(this);
        } else {
            allMethods = getMethods();
        }
        return filterMethods(allMethods, filter);
    }

    public Collection<RubyBasicObject> filterMethodsOnObject(boolean includeAncestors, MethodFilter filter) {
        final Map<String, InternalMethod> allMethods;
        if (includeAncestors) {
            allMethods = ModuleOperations.getAllMethods(this);
        } else {
            allMethods = ModuleOperations.getMethodsUntilLogicalClass(this);
        }
        return filterMethods(allMethods, filter);
    }

    public Collection<RubyBasicObject> filterSingletonMethods(boolean includeAncestors, MethodFilter filter) {
        final Map<String, InternalMethod> allMethods;
        if (includeAncestors) {
            allMethods = ModuleOperations.getMethodsBeforeLogicalClass(this);
        } else {
            allMethods = getMethods();
        }
        return filterMethods(allMethods, filter);
    }

    private Collection<RubyBasicObject> filterMethods(Map<String, InternalMethod> allMethods, MethodFilter filter) {
        final Map<String, InternalMethod> methods = ModuleOperations.withoutUndefinedMethods(allMethods);

        final Set<RubyBasicObject> filtered = new HashSet<>();
        for (InternalMethod method : methods.values()) {
            if (filter.filter(method)) {
                filtered.add(getContext().getSymbolTable().getSymbol(method.getName()));
            }
        }

        return filtered;
    }

    public static class ModuleAllocator implements Allocator {

        @Override
        public RubyBasicObject allocate(RubyContext context, RubyClass rubyClass, Node currentNode) {
            return new RubyModule(context, rubyClass, null, null, currentNode);
        }

    }

}
