/*
 * Copyright (c) 2013, 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.core.module;

import com.oracle.truffle.api.Assumption;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.source.SourceSection;
import com.oracle.truffle.api.utilities.CyclicAssumption;
import org.jruby.truffle.Layouts;
import org.jruby.truffle.Log;
import org.jruby.truffle.RubyContext;
import org.jruby.truffle.core.klass.ClassNodes;
import org.jruby.truffle.core.method.MethodFilter;
import org.jruby.truffle.language.RubyConstant;
import org.jruby.truffle.language.RubyGuards;
import org.jruby.truffle.language.Visibility;
import org.jruby.truffle.language.control.RaiseException;
import org.jruby.truffle.language.methods.InternalMethod;
import org.jruby.truffle.language.objects.IsFrozenNode;
import org.jruby.truffle.language.objects.ObjectGraphNode;
import org.jruby.truffle.language.objects.ObjectIDOperations;
import org.jruby.truffle.language.objects.shared.SharedObjects;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class ModuleFields implements ModuleChain, ObjectGraphNode {

    public static void debugModuleChain(DynamicObject module) {
        assert RubyGuards.isRubyModule(module);
        ModuleChain chain = Layouts.MODULE.getFields(module);
        final StringBuilder builder = new StringBuilder();
        while (chain != null) {
            builder.append(chain.getClass());
            if (!(chain instanceof PrependMarker)) {
                DynamicObject real = chain.getActualModule();
                builder.append(" " + Layouts.MODULE.getFields(real).getName());
            }
            builder.append(System.lineSeparator());
            chain = chain.getParentModule();
        }
        Log.LOGGER.info(builder.toString());
    }

    public DynamicObject rubyModuleObject;

    // The context is stored here - objects can obtain it via their class (which is a module)
    private final RubyContext context;

    private final SourceSection sourceSection;

    public final ModuleChain start;
    @CompilationFinal public ModuleChain parentModule;

    public final DynamicObject lexicalParent;
    public final String givenBaseName;

    private boolean hasFullName = false;
    private String name = null;

    private final Map<String, InternalMethod> methods = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, RubyConstant> constants = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Object> classVariables = new ConcurrentHashMap<>();

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

    public ModuleFields(RubyContext context, SourceSection sourceSection, DynamicObject lexicalParent, String givenBaseName) {
        assert lexicalParent == null || RubyGuards.isRubyModule(lexicalParent);
        this.context = context;
        this.sourceSection = sourceSection;
        this.lexicalParent = lexicalParent;
        this.givenBaseName = givenBaseName;
        this.unmodifiedAssumption = new CyclicAssumption(String.valueOf(givenBaseName) + " is unmodified");
        start = new PrependMarker(this);
    }

    public void getAdoptedByLexicalParent(
            RubyContext context,
            DynamicObject lexicalParent,
            String name,
            Node currentNode) {
        assert RubyGuards.isRubyModule(lexicalParent);

        Layouts.MODULE.getFields(lexicalParent).setConstantInternal(
                context,
                currentNode,
                name,
                rubyModuleObject,
                false);
        Layouts.MODULE.getFields(lexicalParent).addLexicalDependent(rubyModuleObject);

        if (!hasFullName()) {
            // Tricky, we need to compare with the Object class, but we only have a Class at hand.
            final DynamicObject classClass = Layouts.BASIC_OBJECT.getLogicalClass(getLogicalClass());
            final DynamicObject objectClass = ClassNodes.getSuperClass(ClassNodes.getSuperClass(classClass));

            if (lexicalParent == objectClass) {
                this.setFullName(name);
                updateAnonymousChildrenModules(context);
            } else if (Layouts.MODULE.getFields(lexicalParent).hasFullName()) {
                this.setFullName(Layouts.MODULE.getFields(lexicalParent).getName() + "::" + name);
                updateAnonymousChildrenModules(context);
            }
            // else: Our lexicalParent is also an anonymous module
            // and will name us when it gets named via updateAnonymousChildrenModules()
        }
    }

    public void updateAnonymousChildrenModules(RubyContext context) {
        for (Map.Entry<String, RubyConstant> entry : constants.entrySet()) {
            RubyConstant constant = entry.getValue();
            if (RubyGuards.isRubyModule(constant.getValue())) {
                DynamicObject module = (DynamicObject) constant.getValue();
                if (!Layouts.MODULE.getFields(module).hasFullName()) {
                    Layouts.MODULE.getFields(module).getAdoptedByLexicalParent(
                            context,
                            rubyModuleObject,
                            entry.getKey(),
                            null);
                }
            }
        }
    }

    private boolean hasPrependedModules() {
        return start.getParentModule() != this;
    }

    @TruffleBoundary
    public void initCopy(DynamicObject from) {
        assert RubyGuards.isRubyModule(from);

        // Do not copy name, the copy is an anonymous module
        final ModuleFields fromFields = Layouts.MODULE.getFields(from);
        this.methods.putAll(fromFields.methods);
        this.constants.putAll(fromFields.constants);
        this.classVariables.putAll(fromFields.classVariables);

        if (fromFields.hasPrependedModules()) {
            // Then the parent is the first in the prepend chain
            this.parentModule = fromFields.start.getParentModule();
        } else {
            this.parentModule = fromFields.parentModule;
        }

        for (DynamicObject ancestor : fromFields.parentAncestors()) {
            Layouts.MODULE.getFields(ancestor).addDependent(rubyModuleObject);
        }

        if (Layouts.CLASS.isClass(rubyModuleObject)) {
            // Singleton classes cannot be instantiated
            if (!Layouts.CLASS.getIsSingleton(from)) {
                ClassNodes.setInstanceFactory(rubyModuleObject, from);
            }

            Layouts.CLASS.setSuperclass(rubyModuleObject, Layouts.CLASS.getSuperclass(from));
        }
    }

    // TODO (eregon, 12 May 2015): ideally all callers would be nodes and check themselves.
    public void checkFrozen(RubyContext context, Node currentNode) {
        if (context.getCoreLibrary() != null && IsFrozenNode.isFrozen(rubyModuleObject)) {
            throw new RaiseException(context.getCoreExceptions().frozenError(rubyModuleObject, currentNode));
        }
    }

    public void insertAfter(DynamicObject module) {
        parentModule = new IncludedModule(module, parentModule);
    }

    @TruffleBoundary
    public void include(RubyContext context, Node currentNode, DynamicObject module) {
        assert RubyGuards.isRubyModule(module);

        checkFrozen(context, currentNode);

        // If the module we want to include already includes us, it is cyclic
        if (ModuleOperations.includesModule(module, rubyModuleObject)) {
            throw new RaiseException(context.getCoreExceptions().argumentError("cyclic include detected", currentNode));
        }

        SharedObjects.propagate(rubyModuleObject, module);

        // We need to include the module ancestors in reverse order for a given inclusionPoint
        ModuleChain inclusionPoint = this;
        Deque<DynamicObject> modulesToInclude = new ArrayDeque<>();
        for (DynamicObject ancestor : Layouts.MODULE.getFields(module).ancestors()) {
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
            Layouts.MODULE.getFields(mod).addDependent(rubyModuleObject);
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

    @TruffleBoundary
    public void prepend(RubyContext context, Node currentNode, DynamicObject module) {
        assert RubyGuards.isRubyModule(module);

        checkFrozen(context, currentNode);

        // If the module we want to prepend already includes us, it is cyclic
        if (ModuleOperations.includesModule(module, rubyModuleObject)) {
            throw new RaiseException(context.getCoreExceptions().argumentError("cyclic prepend detected", currentNode));
        }

        SharedObjects.propagate(rubyModuleObject, module);

        ModuleChain mod = Layouts.MODULE.getFields(module).start;
        ModuleChain cur = start;
        while (mod != null && !(mod instanceof ModuleFields && RubyGuards.isRubyClass(((ModuleFields) mod).rubyModuleObject))) {
            if (!(mod instanceof PrependMarker)) {
                if (!ModuleOperations.includesModule(rubyModuleObject, mod.getActualModule())) {
                    cur.insertAfter(mod.getActualModule());
                    Layouts.MODULE.getFields(mod.getActualModule()).addDependent(rubyModuleObject);
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
    public void setConstant(RubyContext context, Node currentNode, String name, Object value) {
        if (context.getCoreLibrary().isLoadingRubyCore()) {
            final RubyConstant currentConstant = constants.get(name);

            if (currentConstant != null) {
                return;
            }
        }

        if (RubyGuards.isRubyModule(value)) {
            Layouts.MODULE.getFields((DynamicObject) value).getAdoptedByLexicalParent(context, rubyModuleObject, name, currentNode);
        } else {
            setConstantInternal(context, currentNode, name, value, false);
        }
    }

    @TruffleBoundary
    public void setAutoloadConstant(RubyContext context, Node currentNode, String name, DynamicObject filename) {
        assert RubyGuards.isRubyString(filename);
        setConstantInternal(context, currentNode, name, filename, true);
    }

    public void setConstantInternal(RubyContext context, Node currentNode, String name, Object value, boolean autoload) {
        checkFrozen(context, currentNode);
        // TODO(CS): warn when redefining a constant
        // TODO (nirvdrum 18-Feb-15): But don't warn when redefining an autoloaded constant.

        SharedObjects.propagate(rubyModuleObject, value);

        while (true) {
            final RubyConstant previous = constants.get(name);
            final boolean isPrivate = previous != null && previous.isPrivate();
            final boolean isDeprecated = previous != null && previous.isDeprecated();
            final RubyConstant newValue = new RubyConstant(rubyModuleObject, value, isPrivate, autoload, isDeprecated);

            if ((previous == null) ? (constants.putIfAbsent(name, newValue) == null) : constants.replace(name, previous, newValue)) {
                newLexicalVersion();
                break;
            }
        }
    }

    @TruffleBoundary
    public RubyConstant removeConstant(RubyContext context, Node currentNode, String name) {
        checkFrozen(context, currentNode);
        RubyConstant oldConstant = constants.remove(name);
        newLexicalVersion();
        return oldConstant;
    }

    @TruffleBoundary
    public void addMethod(RubyContext context, Node currentNode, InternalMethod method) {
        assert ModuleOperations.canBindMethodTo(method.getDeclaringModule(), rubyModuleObject) ||
                ModuleOperations.assignableTo(context.getCoreLibrary().getObjectClass(), method.getDeclaringModule()) ||
                // TODO (pitr-ch 24-Jul-2016): find out why undefined methods sometimes do not match above assertion
                // e.g. "block in _routes route_set.rb:525" in rails/actionpack/lib/action_dispatch/routing/
                (method.isUndefined() && methods.get(method.getName()) != null);

        if (context.getCoreLibrary().isLoadingRubyCore()) {
            final InternalMethod currentMethod = methods.get(method.getName());

            if (currentMethod != null && currentMethod.getSharedMethodInfo().getSourceSection().getSource() == null) {
                return;
            }
        }

        checkFrozen(context, currentNode);

        if (SharedObjects.isShared(rubyModuleObject)) {
            for (DynamicObject object : method.getAdjacentObjects()) {
                SharedObjects.writeBarrier(object);
            }
        }

        methods.put(method.getName(), method);

        if (!context.getCoreLibrary().isInitializing()) {
            newVersion();
        }

        if (context.getCoreLibrary().isLoaded() && !method.isUndefined()) {
            if (RubyGuards.isSingletonClass(rubyModuleObject)) {
                DynamicObject receiver = Layouts.CLASS.getAttached(rubyModuleObject);
                context.send(
                        receiver,
                        "singleton_method_added",
                        null,
                        context.getSymbolTable().getSymbol(method.getName()));
            } else {
                context.send(
                        rubyModuleObject,
                        "method_added",
                        null,
                        context.getSymbolTable().getSymbol(method.getName()));
            }
        }
    }

    @TruffleBoundary
    public void removeMethod(String methodName) {
        methods.remove(methodName);
        newVersion();
    }

    @TruffleBoundary
    public void undefMethod(RubyContext context, Node currentNode, String methodName) {
        final InternalMethod method = ModuleOperations.lookupMethod(rubyModuleObject, methodName);
        if (method == null) {
            throw new RaiseException(context.getCoreExceptions().nameErrorUndefinedMethod(
                    methodName,
                    rubyModuleObject,
                    currentNode));
        } else {
            addMethod(context, currentNode, method.undefined());
        }
    }

    /**
     * Also searches on Object for modules.
     * Used for alias_method, visibility changes, etc.
     */
    @TruffleBoundary
    public InternalMethod deepMethodSearch(RubyContext context, String name) {
        InternalMethod method = ModuleOperations.lookupMethod(rubyModuleObject, name);
        if (method != null && !method.isUndefined()) {
            return method;
        }

        // Also search on Object if we are a Module. JRuby calls it deepMethodSearch().
        if (!RubyGuards.isRubyClass(rubyModuleObject)) { // TODO: handle undefined methods
            method = ModuleOperations.lookupMethod(context.getCoreLibrary().getObjectClass(), name);

            if (method != null && !method.isUndefined()) {
                return method;
            }
        }

        return null;
    }

    @TruffleBoundary(throwsControlFlowException = true)
    public void alias(RubyContext context, Node currentNode, String newName, String oldName) {
        InternalMethod method = deepMethodSearch(context, oldName);

        if (method == null) {
            throw new RaiseException(context.getCoreExceptions().nameErrorUndefinedMethod(
                    oldName,
                    rubyModuleObject,
                    currentNode));
        }

        InternalMethod aliasMethod = method.withName(newName);

        if (ModuleOperations.isMethodPrivateFromName(newName)) {
            aliasMethod = aliasMethod.withVisibility(Visibility.PRIVATE);
        }

        addMethod(context, currentNode, aliasMethod);
    }

    @TruffleBoundary
    public void changeConstantVisibility(
            final RubyContext context,
            final Node currentNode,
            final String name,
            final boolean isPrivate) {

        while (true) {
            final RubyConstant previous = constants.get(name);

            if (previous == null) {
                throw new RaiseException(context.getCoreExceptions().nameErrorUninitializedConstant(rubyModuleObject, name, currentNode));
            }

            if (constants.replace(name, previous, previous.withPrivate(isPrivate))) {
                newLexicalVersion();
                break;
            }
        }
    }

    @TruffleBoundary
    public void deprecateConstant(RubyContext context, Node currentNode, String name) {
        while (true) {
            final RubyConstant previous = constants.get(name);

            if (previous == null) {
                throw new RaiseException(context.getCoreExceptions().nameErrorUninitializedConstant(rubyModuleObject, name, currentNode));
            }

            if (constants.replace(name, previous, previous.withDeprecated())) {
                newLexicalVersion();
                break;
            }
        }
    }

    public RubyContext getContext() {
        return context;
    }

    public String getName() {
        final String name = this.name;
        if (name == null) {
            // Lazily compute the anonymous name because it is expensive
            CompilerDirectives.transferToInterpreterAndInvalidate();
            final String anonymousName = createAnonymousName();
            this.name = anonymousName;
            return anonymousName;
        }
        return name;
    }

    public void setFullName(String name) {
        assert name != null;
        hasFullName = true;
        this.name = name;
    }

    @TruffleBoundary
    private String createAnonymousName() {
        if (givenBaseName != null) {
            return Layouts.MODULE.getFields(lexicalParent).getName() + "::" + givenBaseName;
        } else if (getLogicalClass() == rubyModuleObject) { // For the case of class Class during initialization
            return "#<cyclic>";
        } else {
            return "#<" + Layouts.MODULE.getFields(getLogicalClass()).getName() + ":0x" + Long.toHexString(ObjectIDOperations.verySlowGetObjectID(context, rubyModuleObject)) + ">";
        }
    }

    public boolean hasFullName() {
        return hasFullName;
    }

    public boolean hasPartialName() {
        return hasFullName() || givenBaseName != null;
    }

    @Override
    public String toString() {
        return super.toString() + "(" + getName() + ")";
    }

    public void newVersion() {
        newVersion(new HashSet<>(), false);
    }

    public void newLexicalVersion() {
        newVersion(new HashSet<>(), true);
    }

    public void newVersion(Set<DynamicObject> alreadyInvalidated, boolean considerLexicalDependents) {
        if (alreadyInvalidated.contains(rubyModuleObject)) {
            return;
        }

        unmodifiedAssumption.invalidate();
        alreadyInvalidated.add(rubyModuleObject);

        // Make dependents new versions
        for (DynamicObject dependent : dependents) {
            Layouts.MODULE.getFields(dependent).newVersion(alreadyInvalidated, considerLexicalDependents);
        }

        if (considerLexicalDependents) {
            for (DynamicObject dependent : lexicalDependents) {
                Layouts.MODULE.getFields(dependent).newVersion(alreadyInvalidated, considerLexicalDependents);
            }
        }
    }

    public void addDependent(DynamicObject dependent) {
        RubyGuards.isRubyModule(dependent);
        dependents.add(dependent);
    }

    public void addLexicalDependent(DynamicObject lexicalChild) {
        assert RubyGuards.isRubyModule(lexicalChild);
        if (lexicalChild != rubyModuleObject) {
            lexicalDependents.add(lexicalChild);
        }
    }

    public Assumption getUnmodifiedAssumption() {
        return unmodifiedAssumption.getAssumption();
    }

    public Iterable<Entry<String, RubyConstant>> getConstants() {
        return constants.entrySet();
    }

    @TruffleBoundary
    public RubyConstant getConstant(String name) {
        return constants.get(name);
    }

    public Iterable<InternalMethod> getMethods() {
        return methods.values();
    }

    @TruffleBoundary
    public InternalMethod getMethod(String name) {
        return methods.get(name);
    }

    public ConcurrentMap<String, Object> getClassVariables() {
        return classVariables;
    }

    public ModuleChain getParentModule() {
        return parentModule;
    }

    public DynamicObject getActualModule() {
        return rubyModuleObject;
    }

    public Iterable<DynamicObject> ancestors() {
        final ModuleChain top = start;
        return () -> new AncestorIterator(top);
    }

    public Iterable<DynamicObject> parentAncestors() {
        final ModuleChain top = start;
        return () -> {
            final AncestorIterator iterator = new AncestorIterator(top);
            if (iterator.hasNext()) {
                iterator.next();
            }
            return iterator;
        };
    }

    /**
     * Iterates over include'd and prepend'ed modules.
     */
    public Iterable<DynamicObject> prependedAndIncludedModules() {
        final ModuleChain top = start;
        final ModuleFields currentModule = this;
        return () -> new IncludedModulesIterator(top, currentModule);
    }

    public Collection<DynamicObject> filterMethods(RubyContext context, boolean includeAncestors, MethodFilter filter) {
        final Map<String, InternalMethod> allMethods;
        if (includeAncestors) {
            allMethods = ModuleOperations.getAllMethods(rubyModuleObject);
        } else {
            allMethods = methods;
        }
        return filterMethods(context, allMethods, filter);
    }

    public Collection<DynamicObject> filterMethodsOnObject(
            RubyContext context,
            boolean includeAncestors,
            MethodFilter filter) {
        final Map<String, InternalMethod> allMethods;
        if (includeAncestors) {
            allMethods = ModuleOperations.getAllMethods(rubyModuleObject);
        } else {
            allMethods = ModuleOperations.getMethodsUntilLogicalClass(rubyModuleObject);
        }
        return filterMethods(context, allMethods, filter);
    }

    public Collection<DynamicObject> filterSingletonMethods(
            RubyContext context,
            boolean includeAncestors,
            MethodFilter filter) {
        final Map<String, InternalMethod> allMethods;
        if (includeAncestors) {
            allMethods = ModuleOperations.getMethodsBeforeLogicalClass(rubyModuleObject);
        } else {
            allMethods = methods;
        }
        return filterMethods(context, allMethods, filter);
    }

    public Collection<DynamicObject> filterMethods(
            RubyContext context,
            Map<String, InternalMethod> allMethods,
            MethodFilter filter) {
        final Map<String, InternalMethod> methods = ModuleOperations.withoutUndefinedMethods(allMethods);

        final Set<DynamicObject> filtered = new HashSet<>();
        for (InternalMethod method : methods.values()) {
            if (filter.filter(method)) {
                filtered.add(context.getSymbolTable().getSymbol(method.getName()));
            }
        }

        return filtered;
    }

    public DynamicObject getLogicalClass() {
        return Layouts.BASIC_OBJECT.getLogicalClass(rubyModuleObject);
    }

    @Override
    public Set<DynamicObject> getAdjacentObjects() {
        final Set<DynamicObject> adjacent = new HashSet<>();

        if (lexicalParent != null) {
            adjacent.add(lexicalParent);
        }

        for (DynamicObject module : prependedAndIncludedModules()) {
            adjacent.add(module);
        }

        if (Layouts.CLASS.isClass(rubyModuleObject)) {
            DynamicObject superClass = ClassNodes.getSuperClass(rubyModuleObject);
            if (superClass != null) {
                adjacent.add(superClass);
            }
        }

        for (RubyConstant constant : constants.values()) {
            final Object value = constant.getValue();

            if (value instanceof DynamicObject) {
                adjacent.add((DynamicObject) value);
            }
        }

        for (Object value : classVariables.values()) {
            if (value instanceof DynamicObject) {
                adjacent.add((DynamicObject) value);
            }
        }

        for (InternalMethod method : methods.values()) {
            adjacent.addAll(method.getAdjacentObjects());
        }

        return adjacent;
    }

    public SourceSection getSourceSection() {
        return sourceSection;
    }
}
