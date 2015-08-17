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
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.utilities.CyclicAssumption;
import org.jruby.runtime.Visibility;
import org.jruby.truffle.nodes.RubyGuards;
import org.jruby.truffle.nodes.core.BasicObjectNodes;
import org.jruby.truffle.nodes.core.ClassNodes;
import org.jruby.truffle.nodes.core.ModuleNodes;
import org.jruby.truffle.runtime.*;
import org.jruby.truffle.runtime.control.RaiseException;
import org.jruby.truffle.runtime.methods.InternalMethod;
import org.jruby.truffle.runtime.subsystems.ObjectSpaceManager;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ModuleFields implements ModuleChain {

    public static void debugModuleChain(DynamicObject module) {
        assert RubyGuards.isRubyModule(module);
        ModuleChain chain = ModuleNodes.getFields(module);
        while (chain != null) {
            System.err.print(chain.getClass());
            if (!(chain instanceof PrependMarker)) {
                DynamicObject real = chain.getActualModule();
                System.err.print(" " + ModuleNodes.getFields(real).getName());
            }
            System.err.println();
            chain = chain.getParentModule();
        }
    }

    public DynamicObject rubyModuleObject;

    // The context is stored here - objects can obtain it via their class (which is a module)
    private final RubyContext context;

    public final ModuleChain start;
    @CompilerDirectives.CompilationFinal
    public ModuleChain parentModule;

    public final DynamicObject lexicalParent;
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
    private final Set<DynamicObject> dependents = Collections.newSetFromMap(new WeakHashMap<DynamicObject, Boolean>());
    /**
     * Lexical dependent modules, to take care of changes to a module constants.
     */
    private final Set<DynamicObject> lexicalDependents = Collections.newSetFromMap(new WeakHashMap<DynamicObject, Boolean>());

    public ModuleFields(RubyContext context, DynamicObject lexicalParent, String givenBaseName) {
        assert lexicalParent == null || RubyGuards.isRubyModule(lexicalParent);
        this.context = context;
        this.lexicalParent = lexicalParent;
        this.givenBaseName = givenBaseName;
        this.unmodifiedAssumption = new CyclicAssumption(name + " is unmodified");
        start = new PrependMarker(this);
    }

    public void getAdoptedByLexicalParent(DynamicObject lexicalParent, String name, Node currentNode) {
        assert RubyGuards.isRubyModule(lexicalParent);

        ModuleNodes.getFields(lexicalParent).setConstantInternal(currentNode, name, rubyModuleObject, false);
        ModuleNodes.getFields(lexicalParent).addLexicalDependent(rubyModuleObject);

        if (this.name == null) {
            // Tricky, we need to compare with the Object class, but we only have a Class at hand.
            final DynamicObject classClass = BasicObjectNodes.getLogicalClass(getLogicalClass());
            final DynamicObject objectClass = ClassNodes.getSuperClass(ClassNodes.getSuperClass(classClass));

            if (lexicalParent == objectClass) {
                this.name = name;
                updateAnonymousChildrenModules();
            } else if (ModuleNodes.getFields(lexicalParent).hasName()) {
                this.name = ModuleNodes.getFields(lexicalParent).getName() + "::" + name;
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
                DynamicObject module = (DynamicObject) constant.getValue();
                if (!ModuleNodes.getFields(module).hasName()) {
                    ModuleNodes.getFields(module).getAdoptedByLexicalParent(rubyModuleObject, entry.getKey(), null);
                }
            }
        }
    }

    @CompilerDirectives.TruffleBoundary
    public void initCopy(DynamicObject from) {
        assert RubyGuards.isRubyModule(from);

        // Do not copy name, the copy is an anonymous module
        this.methods.putAll(ModuleNodes.getFields(from).methods);
        this.constants.putAll(ModuleNodes.getFields(from).constants);
        this.classVariables.putAll(ModuleNodes.getFields(from).classVariables);

        if (ModuleNodes.getFields(from).start.getParentModule() != ModuleNodes.getFields(from)) {
            this.parentModule = ModuleNodes.getFields(from).start.getParentModule();
        } else {
            this.parentModule = ModuleNodes.getFields(from).parentModule;
        }

        for (DynamicObject ancestor : ModuleNodes.getFields(from).ancestors()) {
            ModuleNodes.getFields(ancestor).addDependent(rubyModuleObject);
        }
    }

    // TODO (eregon, 12 May 2015): ideally all callers would be nodes and check themselves.
    public void checkFrozen(Node currentNode) {
        if (getContext().getCoreLibrary() != null && DebugOperations.verySlowIsFrozen(getContext(), rubyModuleObject)) {
            CompilerDirectives.transferToInterpreter();
            throw new RaiseException(getContext().getCoreLibrary().frozenError(ModuleNodes.getFields(getLogicalClass()).getName(), currentNode));
        }
    }

    public void insertAfter(DynamicObject module) {
        parentModule = new IncludedModule(module, parentModule);
    }

    @CompilerDirectives.TruffleBoundary
    public void include(Node currentNode, DynamicObject module) {
        assert RubyGuards.isRubyModule(module);

        checkFrozen(currentNode);

        // If the module we want to include already includes us, it is cyclic
        if (ModuleOperations.includesModule(module, rubyModuleObject)) {
            throw new RaiseException(getContext().getCoreLibrary().argumentError("cyclic include detected", currentNode));
        }

        // We need to include the module ancestors in reverse order for a given inclusionPoint
        ModuleChain inclusionPoint = this;
        Deque<DynamicObject> modulesToInclude = new ArrayDeque<>();
        for (DynamicObject ancestor : ModuleNodes.getFields(module).ancestors()) {
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

    public void performIncludes(ModuleChain inclusionPoint, Deque<DynamicObject> moduleAncestors) {
        while (!moduleAncestors.isEmpty()) {
            DynamicObject mod = moduleAncestors.pop();
            assert RubyGuards.isRubyModule(mod);
            inclusionPoint.insertAfter(mod);
            ModuleNodes.getFields(mod).addDependent(rubyModuleObject);
        }
    }

    public boolean isIncludedModuleBeforeSuperClass(DynamicObject module) {
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
    public void prepend(Node currentNode, DynamicObject module) {
        assert RubyGuards.isRubyModule(module);

        checkFrozen(currentNode);

        // If the module we want to prepend already includes us, it is cyclic
        if (ModuleOperations.includesModule(module, rubyModuleObject)) {
            throw new RaiseException(getContext().getCoreLibrary().argumentError("cyclic prepend detected", currentNode));
        }

        ModuleChain mod = ModuleNodes.getFields(module).start;
        ModuleChain cur = start;
        while (mod != null && !(RubyGuards.isRubyModule(mod) && RubyGuards.isRubyClass(((ModuleFields) mod).rubyModuleObject))) {
            if (!(mod instanceof PrependMarker)) {
                if (!ModuleOperations.includesModule(rubyModuleObject, mod.getActualModule())) {
                    cur.insertAfter(mod.getActualModule());
                    ModuleNodes.getFields(mod.getActualModule()).addDependent(rubyModuleObject);
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
            ModuleNodes.getFields(((DynamicObject) value)).getAdoptedByLexicalParent(rubyModuleObject, name, currentNode);
        } else {
            setConstantInternal(currentNode, name, value, false);
        }
    }

    @CompilerDirectives.TruffleBoundary
    public void setAutoloadConstant(Node currentNode, String name, DynamicObject filename) {
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
        if (RubyGuards.isRubyModule(rubyModuleObject) && !RubyGuards.isRubyClass(rubyModuleObject)) { // TODO: handle undefined methods
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
                return ModuleNodes.getFields(lexicalParent).getName() + "::" + givenBaseName;
            } else if (getLogicalClass() == rubyModuleObject) { // For the case of class Class during initialization
                return "#<cyclic>";
            } else {
                return "#<" + ModuleNodes.getFields(getLogicalClass()).getName() + ":0x" + Long.toHexString(BasicObjectNodes.verySlowGetObjectID(rubyModuleObject)) + ">";
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
        newVersion(new HashSet<DynamicObject>(), false);
    }

    public void newLexicalVersion() {
        newVersion(new HashSet<DynamicObject>(), true);
    }

    public void newVersion(Set<DynamicObject> alreadyInvalidated, boolean considerLexicalDependents) {
        if (alreadyInvalidated.contains(rubyModuleObject))
            return;

        unmodifiedAssumption.invalidate();
        alreadyInvalidated.add(rubyModuleObject);

        // Make dependents new versions
        for (DynamicObject dependent : dependents) {
            ModuleNodes.getFields(dependent).newVersion(alreadyInvalidated, considerLexicalDependents);
        }

        if (considerLexicalDependents) {
            for (DynamicObject dependent : lexicalDependents) {
                ModuleNodes.getFields(dependent).newVersion(alreadyInvalidated, considerLexicalDependents);
            }
        }
    }

    public void addDependent(DynamicObject dependent) {
        RubyGuards.isRubyModule(dependent);
        dependents.add(dependent);
    }

    public void addLexicalDependent(DynamicObject lexicalChild) {
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
            if (constant.getValue() instanceof DynamicObject) {
                BasicObjectNodes.visitObjectGraph(((DynamicObject) constant.getValue()), visitor);
            }
        }

        for (Object classVariable : classVariables.values()) {
            if (classVariable instanceof DynamicObject) {
                BasicObjectNodes.visitObjectGraph(((DynamicObject) classVariable), visitor);
            }
        }

        for (InternalMethod method : methods.values()) {
            if (method.getDeclarationFrame() != null) {
                getContext().getObjectSpaceManager().visitFrame(method.getDeclarationFrame(), visitor);
            }
        }

        for (DynamicObject ancestor : ancestors()) {
            BasicObjectNodes.visitObjectGraph(ancestor, visitor);
        }
    }

    public ModuleChain getParentModule() {
        return parentModule;
    }

    public DynamicObject getActualModule() {
        return rubyModuleObject;
    }

    public Iterable<DynamicObject> ancestors() {
        final ModuleChain top = start;
        return new Iterable<DynamicObject>() {
            @Override
            public Iterator<DynamicObject> iterator() {
                return new AncestorIterator(top);
            }
        };
    }

    public Iterable<DynamicObject> parentAncestors() {
        final ModuleChain top = start;
        return new Iterable<DynamicObject>() {
            @Override
            public Iterator<DynamicObject> iterator() {
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
    public Iterable<DynamicObject> prependedAndIncludedModules() {
        final ModuleChain top = start;
        final ModuleFields currentModule = this;
        return new Iterable<DynamicObject>() {
            @Override
            public Iterator<DynamicObject> iterator() {
                return new IncludedModulesIterator(top, currentModule);
            }
        };
    }

    public Collection<DynamicObject> filterMethods(boolean includeAncestors, MethodFilter filter) {
        final Map<String, InternalMethod> allMethods;
        if (includeAncestors) {
            allMethods = ModuleOperations.getAllMethods(rubyModuleObject);
        } else {
            allMethods = getMethods();
        }
        return filterMethods(allMethods, filter);
    }

    public Collection<DynamicObject> filterMethodsOnObject(boolean includeAncestors, MethodFilter filter) {
        final Map<String, InternalMethod> allMethods;
        if (includeAncestors) {
            allMethods = ModuleOperations.getAllMethods(rubyModuleObject);
        } else {
            allMethods = ModuleOperations.getMethodsUntilLogicalClass(rubyModuleObject);
        }
        return filterMethods(allMethods, filter);
    }

    public Collection<DynamicObject> filterSingletonMethods(boolean includeAncestors, MethodFilter filter) {
        final Map<String, InternalMethod> allMethods;
        if (includeAncestors) {
            allMethods = ModuleOperations.getMethodsBeforeLogicalClass(rubyModuleObject);
        } else {
            allMethods = getMethods();
        }
        return filterMethods(allMethods, filter);
    }

    public Collection<DynamicObject> filterMethods(Map<String, InternalMethod> allMethods, MethodFilter filter) {
        final Map<String, InternalMethod> methods = ModuleOperations.withoutUndefinedMethods(allMethods);

        final Set<DynamicObject> filtered = new HashSet<>();
        for (InternalMethod method : methods.values()) {
            if (filter.filter(method)) {
                filtered.add(getContext().getSymbolTable().getSymbol(method.getName()));
            }
        }

        return filtered;
    }

    public DynamicObject getLogicalClass() {
        return BasicObjectNodes.getLogicalClass(rubyModuleObject);
    }

}
