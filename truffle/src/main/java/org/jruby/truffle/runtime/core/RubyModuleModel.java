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
import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.object.DynamicObjectFactory;
import com.oracle.truffle.api.utilities.CyclicAssumption;
import org.jruby.runtime.Visibility;
import org.jruby.truffle.nodes.RubyGuards;
import org.jruby.truffle.nodes.core.BasicObjectNodes;
import org.jruby.truffle.nodes.core.ClassNodes;
import org.jruby.truffle.nodes.core.ModuleNodes;
import org.jruby.truffle.nodes.objects.Allocator;
import org.jruby.truffle.runtime.*;
import org.jruby.truffle.runtime.control.RaiseException;
import org.jruby.truffle.runtime.methods.InternalMethod;
import org.jruby.truffle.runtime.subsystems.ObjectSpaceManager;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class RubyModuleModel implements ModuleChain {

    public static void debugModuleChain(RubyBasicObject module) {
        assert RubyGuards.isRubyModule(module);
        ModuleChain chain = ModuleNodes.getModel(module);
        while (chain != null) {
            System.err.print(chain.getClass());
            if (!(chain instanceof PrependMarker)) {
                RubyBasicObject real = chain.getActualModule();
                System.err.print(" " + ModuleNodes.getModel(real).getName());
            }
            System.err.println();
            chain = chain.getParentModule();
        }
    }

    public RubyBasicObject rubyModuleObject;

    // The context is stored here - objects can obtain it via their class (which is a module)
    private final RubyContext context;

    protected final ModuleChain start;
    @CompilerDirectives.CompilationFinal
    protected ModuleChain parentModule;

    public final RubyBasicObject lexicalParent;
    public final String givenBaseName;
    /**
     * Full name, including named parent
     */
    public String name;

    private final Map<String, InternalMethod> methods = new ConcurrentHashMap<>();
    private final Map<String, RubyConstant> constants = new ConcurrentHashMap<>();
    private final Map<String, Object> classVariables = new ConcurrentHashMap<>();

    private final CyclicAssumption unmodifiedAssumption;

    /**
     * Keep track of other modules that depend on the configuration of this module in some way. The
     * include subclasses and modules that include this module.
     */
    private final Set<RubyBasicObject> dependents = Collections.newSetFromMap(new WeakHashMap<RubyBasicObject, Boolean>());
    /**
     * Lexical dependent modules, to take care of changes to a module constants.
     */
    private final Set<RubyBasicObject> lexicalDependents = Collections.newSetFromMap(new WeakHashMap<RubyBasicObject, Boolean>());

    // Only used for classes

    public final boolean isSingleton;
    public final RubyBasicObject attached;
    public Allocator allocator;
    public DynamicObjectFactory factory;

    public RubyModuleModel(RubyContext context, RubyBasicObject lexicalParent, String givenBaseName, boolean isSingleton, RubyBasicObject attached, Allocator allocator, DynamicObjectFactory factory) {
        assert lexicalParent == null || RubyGuards.isRubyModule(lexicalParent);
        assert attached == null || RubyGuards.isRubyModule(attached);
        this.context = context;
        this.lexicalParent = lexicalParent;
        this.givenBaseName = givenBaseName;
        this.unmodifiedAssumption = new CyclicAssumption(name + " is unmodified");
        start = new PrependMarker(this);
        this.isSingleton = isSingleton;
        this.attached = attached;
        this.allocator = allocator;
        this.factory = factory;
    }

    public void getAdoptedByLexicalParent(RubyBasicObject lexicalParent, String name, Node currentNode) {
        assert RubyGuards.isRubyModule(lexicalParent);

        ModuleNodes.getModel(lexicalParent).setConstantInternal(currentNode, name, rubyModuleObject, false);
        ModuleNodes.getModel(lexicalParent).addLexicalDependent(rubyModuleObject);

        if (this.name == null) {
            // Tricky, we need to compare with the Object class, but we only have a Class at hand.
            final RubyBasicObject classClass = BasicObjectNodes.getLogicalClass(getLogicalClass());
            final RubyBasicObject objectClass = ModuleNodes.getModel(ModuleNodes.getModel(classClass).getSuperClass()).getSuperClass();

            if (lexicalParent == objectClass) {
                this.name = name;
                updateAnonymousChildrenModules();
            } else if (ModuleNodes.getModel(lexicalParent).hasName()) {
                this.name = ModuleNodes.getModel(lexicalParent).getName() + "::" + name;
                updateAnonymousChildrenModules();
            }
            // else: Our lexicalParent is also an anonymous module
            // and will name us when it gets named via updateAnonymousChildrenModules()
        }
    }

    public void updateAnonymousChildrenModules() {
        for (Map.Entry<String, RubyConstant> entry : constants.entrySet()) {
            RubyConstant constant = entry.getValue();
            if (RubyGuards.isRubyModule(constant.getValue())) {
                RubyBasicObject module = (RubyBasicObject) constant.getValue();
                if (!ModuleNodes.getModel(module).hasName()) {
                    ModuleNodes.getModel(module).getAdoptedByLexicalParent(rubyModuleObject, entry.getKey(), null);
                }
            }
        }
    }

    @CompilerDirectives.TruffleBoundary
    public void initCopy(RubyBasicObject from) {
        assert RubyGuards.isRubyModule(from);

        // Do not copy name, the copy is an anonymous module
        this.methods.putAll(ModuleNodes.getModel(from).methods);
        this.constants.putAll(ModuleNodes.getModel(from).constants);
        this.classVariables.putAll(ModuleNodes.getModel(from).classVariables);

        if (ModuleNodes.getModel(from).start.getParentModule() != ModuleNodes.getModel(from)) {
            this.parentModule = ModuleNodes.getModel(from).start.getParentModule();
        } else {
            this.parentModule = ModuleNodes.getModel(from).parentModule;
        }

        for (RubyBasicObject ancestor : ModuleNodes.getModel(from).ancestors()) {
            ModuleNodes.getModel(ancestor).addDependent(rubyModuleObject);
        }

        if (isClass()) {
            ModuleNodes.getModel(rubyModuleObject).allocator = ModuleNodes.getModel(from).allocator;
            // isSingleton is false as we cannot copy a singleton class.
            // and therefore attached is null.
        }
    }

    /**
     * If this instance is a module and not a class.
     */
    public boolean isOnlyAModule() {
        return !isClass();
    }

    public boolean isClass() {
        return RubyGuards.isRubyClass(rubyModuleObject);
    }

    // TODO (eregon, 12 May 2015): ideally all callers would be nodes and check themselves.
    public void checkFrozen(Node currentNode) {
        if (getContext().getCoreLibrary() != null && DebugOperations.verySlowIsFrozen(getContext(), rubyModuleObject)) {
            CompilerDirectives.transferToInterpreter();
            throw new RaiseException(getContext().getCoreLibrary().frozenError(ModuleNodes.getModel(getLogicalClass()).getName(), currentNode));
        }
    }

    public void insertAfter(RubyBasicObject module) {
        parentModule = new IncludedModule(module, parentModule);
    }

    @CompilerDirectives.TruffleBoundary
    public void include(Node currentNode, RubyBasicObject module) {
        assert RubyGuards.isRubyModule(module);

        checkFrozen(currentNode);

        // If the module we want to include already includes us, it is cyclic
        if (ModuleOperations.includesModule(module, rubyModuleObject)) {
            throw new RaiseException(getContext().getCoreLibrary().argumentError("cyclic include detected", currentNode));
        }

        // We need to include the module ancestors in reverse order for a given inclusionPoint
        ModuleChain inclusionPoint = this;
        Deque<RubyBasicObject> modulesToInclude = new ArrayDeque<>();
        for (RubyBasicObject ancestor : ModuleNodes.getModel(module).ancestors()) {
            if (ModuleOperations.includesModule(rubyModuleObject, ancestor)) {
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

    public void performIncludes(ModuleChain inclusionPoint, Deque<RubyBasicObject> moduleAncestors) {
        while (!moduleAncestors.isEmpty()) {
            RubyBasicObject mod = moduleAncestors.pop();
            assert RubyGuards.isRubyModule(mod);
            inclusionPoint.insertAfter(mod);
            ModuleNodes.getModel(mod).addDependent(rubyModuleObject);
        }
    }

    public boolean isIncludedModuleBeforeSuperClass(RubyBasicObject module) {
        assert RubyGuards.isRubyModule(module);
        ModuleChain included = parentModule;
        while (included instanceof IncludedModule) {
            if (included.getActualModule() == module) {
                return true;
            }
            included = included.getParentModule();
        }
        return false;
    }

    @CompilerDirectives.TruffleBoundary
    public void prepend(Node currentNode, RubyBasicObject module) {
        assert RubyGuards.isRubyModule(module);

        checkFrozen(currentNode);

        // If the module we want to prepend already includes us, it is cyclic
        if (ModuleOperations.includesModule(module, rubyModuleObject)) {
            throw new RaiseException(getContext().getCoreLibrary().argumentError("cyclic prepend detected", currentNode));
        }

        ModuleChain mod = ModuleNodes.getModel(module).start;
        ModuleChain cur = start;
        while (mod != null && !(RubyGuards.isRubyModule(mod) && ((RubyModuleModel) mod).isClass())) {
            if (!(mod instanceof PrependMarker)) {
                if (!ModuleOperations.includesModule(rubyModuleObject, mod.getActualModule())) {
                    cur.insertAfter(mod.getActualModule());
                    ModuleNodes.getModel(mod.getActualModule()).addDependent(rubyModuleObject);
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
    @CompilerDirectives.TruffleBoundary
    public void setConstant(Node currentNode, String name, Object value) {
        if (getContext().getCoreLibrary().isLoadingRubyCore()) {
            final RubyConstant currentConstant = constants.get(name);

            if (currentConstant != null) {
                return;
            }
        }

        if (RubyGuards.isRubyModule(value)) {
            ModuleNodes.getModel(((RubyBasicObject) value)).getAdoptedByLexicalParent(rubyModuleObject, name, currentNode);
        } else {
            setConstantInternal(currentNode, name, value, false);
        }
    }

    @CompilerDirectives.TruffleBoundary
    public void setAutoloadConstant(Node currentNode, String name, RubyBasicObject filename) {
        assert RubyGuards.isRubyString(filename);
        setConstantInternal(currentNode, name, filename, true);
    }

    public void setConstantInternal(Node currentNode, String name, Object value, boolean autoload) {
        checkFrozen(currentNode);

        RubyConstant previous = constants.get(name);
        if (previous == null) {
            constants.put(name, new RubyConstant(rubyModuleObject, value, false, autoload));
        } else {
            // TODO(CS): warn when redefining a constant
            // TODO (nirvdrum 18-Feb-15): But don't warn when redefining an autoloaded constant.
            constants.put(name, new RubyConstant(rubyModuleObject, value, previous.isPrivate(), autoload));
        }

        newLexicalVersion();
    }

    @CompilerDirectives.TruffleBoundary
    public RubyConstant removeConstant(Node currentNode, String name) {
        checkFrozen(currentNode);
        RubyConstant oldConstant = constants.remove(name);
        newLexicalVersion();
        return oldConstant;
    }

    @CompilerDirectives.TruffleBoundary
    public void setClassVariable(Node currentNode, String variableName, Object value) {
        checkFrozen(currentNode);

        classVariables.put(variableName, value);
    }

    @CompilerDirectives.TruffleBoundary
    public Object removeClassVariable(Node currentNode, String name) {
        checkFrozen(currentNode);

        final Object found = classVariables.remove(name);
        if (found == null) {
            CompilerDirectives.transferToInterpreter();
            throw new RaiseException(context.getCoreLibrary().nameErrorClassVariableNotDefined(name, rubyModuleObject, currentNode));
        }
        return found;
    }

    @CompilerDirectives.TruffleBoundary
    public void addMethod(Node currentNode, InternalMethod method) {
        assert method != null;

        if (getContext().getCoreLibrary().isLoadingRubyCore()) {
            final InternalMethod currentMethod = methods.get(method.getName());

            if (currentMethod != null && currentMethod.getSharedMethodInfo().getSourceSection() instanceof CoreSourceSection) {
                return;
            }
        }

        checkFrozen(currentNode);
        methods.put(method.getName(), method.withDeclaringModule(rubyModuleObject));
        newVersion();

        if (context.getCoreLibrary().isLoaded() && !method.isUndefined()) {
            DebugOperations.send(context, rubyModuleObject, "method_added", null, context.getSymbolTable().getSymbol(method.getName()));
        }
    }

    @CompilerDirectives.TruffleBoundary
    public void removeMethod(String methodName) {
        methods.remove(methodName);
        newVersion();
    }

    @CompilerDirectives.TruffleBoundary
    public void undefMethod(Node currentNode, String methodName) {
        final InternalMethod method = ModuleOperations.lookupMethod(rubyModuleObject, methodName);
        if (method == null) {
            throw new UnsupportedOperationException();
        } else {
            undefMethod(currentNode, method);
        }
    }

    @CompilerDirectives.TruffleBoundary
    public void undefMethod(Node currentNode, InternalMethod method) {
        addMethod(currentNode, method.undefined());
    }

    /**
     * Also searches on Object for modules.
     * Used for alias_method, visibility changes, etc.
     */
    @CompilerDirectives.TruffleBoundary
    public InternalMethod deepMethodSearch(String name) {
        InternalMethod method = ModuleOperations.lookupMethod(rubyModuleObject, name);
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

    @CompilerDirectives.TruffleBoundary
    public void alias(Node currentNode, String newName, String oldName) {
        InternalMethod method = deepMethodSearch(oldName);

        if (method == null) {
            CompilerDirectives.transferToInterpreter();
            throw new RaiseException(getContext().getCoreLibrary().noMethodErrorOnModule(oldName, rubyModuleObject, currentNode));
        }

        InternalMethod aliasMethod = method.withName(newName);

        if (ModuleOperations.isMethodPrivateFromName(newName)) {
            aliasMethod = aliasMethod.withVisibility(Visibility.PRIVATE);
        }

        addMethod(currentNode, aliasMethod);
    }

    @CompilerDirectives.TruffleBoundary
    public void changeConstantVisibility(Node currentNode, String name, boolean isPrivate) {
        checkFrozen(currentNode);
        RubyConstant rubyConstant = constants.get(name);

        if (rubyConstant != null) {
            rubyConstant.setPrivate(isPrivate);
            newLexicalVersion();
        } else {
            throw new RaiseException(context.getCoreLibrary().nameErrorUninitializedConstant(rubyModuleObject, name, currentNode));
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
                return ModuleNodes.getModel(lexicalParent).getName() + "::" + givenBaseName;
            } else if (getLogicalClass() == rubyModuleObject) { // For the case of class Class during initialization
                return "#<cyclic>";
            } else {
                return "#<" + ModuleNodes.getModel(getLogicalClass()).getName() + ":0x" + Long.toHexString(BasicObjectNodes.verySlowGetObjectID(rubyModuleObject)) + ">";
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
        newVersion(new HashSet<RubyBasicObject>(), false);
    }

    public void newLexicalVersion() {
        newVersion(new HashSet<RubyBasicObject>(), true);
    }

    public void newVersion(Set<RubyBasicObject> alreadyInvalidated, boolean considerLexicalDependents) {
        if (alreadyInvalidated.contains(rubyModuleObject))
            return;

        unmodifiedAssumption.invalidate();
        alreadyInvalidated.add(rubyModuleObject);

        // Make dependents new versions
        for (RubyBasicObject dependent : dependents) {
            ModuleNodes.getModel(dependent).newVersion(alreadyInvalidated, considerLexicalDependents);
        }

        if (considerLexicalDependents) {
            for (RubyBasicObject dependent : lexicalDependents) {
                ModuleNodes.getModel(dependent).newVersion(alreadyInvalidated, considerLexicalDependents);
            }
        }
    }

    public void addDependent(RubyBasicObject dependent) {
        RubyGuards.isRubyModule(dependent);
        dependents.add(dependent);
    }

    public void addLexicalDependent(RubyBasicObject lexicalChild) {
        assert RubyGuards.isRubyModule(lexicalChild);
        if (lexicalChild != rubyModuleObject)
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

    public void visitObjectGraphChildren(ObjectSpaceManager.ObjectGraphVisitor visitor) {
        for (RubyConstant constant : constants.values()) {
            if (constant.getValue() instanceof RubyBasicObject) {
                BasicObjectNodes.visitObjectGraph(((RubyBasicObject) constant.getValue()), visitor);
            }
        }

        for (Object classVariable : classVariables.values()) {
            if (classVariable instanceof RubyBasicObject) {
                BasicObjectNodes.visitObjectGraph(((RubyBasicObject) classVariable), visitor);
            }
        }

        for (InternalMethod method : methods.values()) {
            if (method.getDeclarationFrame() != null) {
                getContext().getObjectSpaceManager().visitFrame(method.getDeclarationFrame(), visitor);
            }
        }

        for (RubyBasicObject ancestor : ancestors()) {
            BasicObjectNodes.visitObjectGraph(ancestor, visitor);
        }
    }

    public ModuleChain getParentModule() {
        return parentModule;
    }

    public RubyBasicObject getActualModule() {
        return rubyModuleObject;
    }

    public Iterable<RubyBasicObject> ancestors() {
        final ModuleChain top = start;
        return new Iterable<RubyBasicObject>() {
            @Override
            public Iterator<RubyBasicObject> iterator() {
                return new AncestorIterator(top);
            }
        };
    }

    public Iterable<RubyBasicObject> parentAncestors() {
        final ModuleChain top = start;
        return new Iterable<RubyBasicObject>() {
            @Override
            public Iterator<RubyBasicObject> iterator() {
                final AncestorIterator iterator = new AncestorIterator(top);
                if (iterator.hasNext()) {
                    iterator.next();
                }
                return iterator;
            }
        };
    }

    /**
     * Iterates over include'd and prepend'ed modules.
     */
    public Iterable<RubyBasicObject> prependedAndIncludedModules() {
        final ModuleChain top = start;
        final RubyModuleModel currentModule = this;
        return new Iterable<RubyBasicObject>() {
            @Override
            public Iterator<RubyBasicObject> iterator() {
                return new IncludedModulesIterator(top, currentModule);
            }
        };
    }

    public Collection<RubyBasicObject> filterMethods(boolean includeAncestors, MethodFilter filter) {
        final Map<String, InternalMethod> allMethods;
        if (includeAncestors) {
            allMethods = ModuleOperations.getAllMethods(rubyModuleObject);
        } else {
            allMethods = getMethods();
        }
        return filterMethods(allMethods, filter);
    }

    public Collection<RubyBasicObject> filterMethodsOnObject(boolean includeAncestors, MethodFilter filter) {
        final Map<String, InternalMethod> allMethods;
        if (includeAncestors) {
            allMethods = ModuleOperations.getAllMethods(rubyModuleObject);
        } else {
            allMethods = ModuleOperations.getMethodsUntilLogicalClass(rubyModuleObject);
        }
        return filterMethods(allMethods, filter);
    }

    public Collection<RubyBasicObject> filterSingletonMethods(boolean includeAncestors, MethodFilter filter) {
        final Map<String, InternalMethod> allMethods;
        if (includeAncestors) {
            allMethods = ModuleOperations.getMethodsBeforeLogicalClass(rubyModuleObject);
        } else {
            allMethods = getMethods();
        }
        return filterMethods(allMethods, filter);
    }

    public Collection<RubyBasicObject> filterMethods(Map<String, InternalMethod> allMethods, MethodFilter filter) {
        final Map<String, InternalMethod> methods = ModuleOperations.withoutUndefinedMethods(allMethods);

        final Set<RubyBasicObject> filtered = new HashSet<>();
        for (InternalMethod method : methods.values()) {
            if (filter.filter(method)) {
                filtered.add(getContext().getSymbolTable().getSymbol(method.getName()));
            }
        }

        return filtered;
    }

    public RubyBasicObject getLogicalClass() {
        return BasicObjectNodes.getLogicalClass(rubyModuleObject);
    }

    public void initialize(RubyBasicObject superclass) {
        assert isClass();
        assert RubyGuards.isRubyClass(superclass);
        unsafeSetSuperclass(superclass);
        ensureSingletonConsistency();
        ModuleNodes.getModel(rubyModuleObject).allocator = ModuleNodes.getModel(superclass).allocator;
    }

    /**
     * This method supports initialization and solves boot-order problems and should not normally be
     * used.
     */
    public void unsafeSetSuperclass(RubyBasicObject superClass) {
        assert RubyGuards.isRubyClass(superClass);
        assert isClass();
        assert parentModule == null;

        parentModule = ModuleNodes.getModel(superClass).start;
        ModuleNodes.getModel(superClass).addDependent(rubyModuleObject);

        newVersion();
    }

    public RubyBasicObject ensureSingletonConsistency() {
        assert isClass();
        createOneSingletonClass();
        return rubyModuleObject;
    }

    public RubyBasicObject getSingletonClass() {
        assert isClass();
        // We also need to create the singleton class of a singleton class for proper lookup and consistency.
        // See rb_singleton_class() documentation in MRI.
        return ModuleNodes.getModel(createOneSingletonClass()).ensureSingletonConsistency();
    }

    public RubyBasicObject createOneSingletonClass() {
        assert isClass();
        CompilerAsserts.neverPartOfCompilation();

        if (ModuleNodes.getModel(BasicObjectNodes.getMetaClass(rubyModuleObject)).isSingleton()) {
            return BasicObjectNodes.getMetaClass(rubyModuleObject);
        }

        final RubyBasicObject singletonSuperclass;
        if (getSuperClass() == null) {
            singletonSuperclass = getLogicalClass();
        } else {
            singletonSuperclass = ModuleNodes.getModel(getSuperClass()).createOneSingletonClass();
        }

        String name = String.format("#<Class:%s>", getName());
        BasicObjectNodes.setMetaClass(rubyModuleObject, ClassNodes.createRubyClass(getContext(), getLogicalClass(), null, singletonSuperclass, name, true, rubyModuleObject, null));

        return BasicObjectNodes.getMetaClass(rubyModuleObject);
    }


    public boolean isSingleton() {
        assert isClass();
        return isSingleton;
    }

    public RubyBasicObject getAttached() {
        assert isClass();
        return attached;
    }

    public RubyBasicObject getSuperClass() {
        assert isClass();
        CompilerAsserts.neverPartOfCompilation();

        for (RubyBasicObject ancestor : parentAncestors()) {
            if (RubyGuards.isRubyClass(ancestor)) {
                return ancestor;
            }
        }

        return null;
    }

    @Override
    public DynamicObjectFactory getFactory() {
        CompilerAsserts.neverPartOfCompilation();

        if (factory == null) {
            if (parentModule == null) {
                return null;
            }

            return parentModule.getFactory();
        }

        return factory;
    }
}
