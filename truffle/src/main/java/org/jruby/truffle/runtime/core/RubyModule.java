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
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.utilities.CyclicAssumption;
import org.jruby.truffle.runtime.*;
import org.jruby.truffle.runtime.methods.InternalMethod;
import org.jruby.truffle.runtime.subsystems.ObjectSpaceManager;

import java.util.*;

/**
 * Represents the Ruby {@code Module} class.
 */
public class RubyModule extends RubyBasicObject implements ModuleChain {

    public final RubyModuleModel model;

    public RubyModule(RubyContext context, RubyClass selfClass, RubyModule lexicalParent, String name, Node currentNode) {
        super(context, selfClass);
        model = new RubyModuleModel(this, context, lexicalParent, name, new CyclicAssumption(name + " is unmodified"));

        if (lexicalParent == null) { // bootstrap or anonymous module
            model.name = model.givenBaseName;
        } else {
            getAdoptedByLexicalParent(lexicalParent, name, currentNode);
        }
    }

    public void getAdoptedByLexicalParent(RubyModule lexicalParent, String name, Node currentNode) {
        model.getAdoptedByLexicalParent(lexicalParent, name, currentNode);
    }

    public void updateAnonymousChildrenModules() {
        model.updateAnonymousChildrenModules();
    }

    public void initCopy(RubyModule from) {
        model.initCopy(from);
    }

    public boolean isOnlyAModule() {
        return model.isOnlyAModule();
    }

    private void checkFrozen(Node currentNode) {
        model.checkFrozen(currentNode);
    }

    public void insertAfter(RubyModule module) {
        model.insertAfter(module);
    }

    public void include(Node currentNode, RubyModule module) {
        model.include(currentNode, module);
    }

    private void performIncludes(ModuleChain inclusionPoint, Stack<RubyModule> moduleAncestors) {
        model.performIncludes(inclusionPoint, moduleAncestors);
    }

    private boolean isIncludedModuleBeforeSuperClass(RubyModule module) {
        return model.isIncludedModuleBeforeSuperClass(module);
    }

    public void prepend(Node currentNode, RubyModule module) {
        model.prepend(currentNode, module);
    }

    public void setConstant(Node currentNode, String name, Object value) {
        model.setConstant(currentNode, name, value);
    }

    public void setAutoloadConstant(Node currentNode, String name, RubyBasicObject filename) {
        model.setAutoloadConstant(currentNode, name, filename);
    }

    public void setConstantInternal(Node currentNode, String name, Object value, boolean autoload) {
        model.setConstantInternal(currentNode, name, value, autoload);
    }

    public RubyConstant removeConstant(Node currentNode, String name) {
        return model.removeConstant(currentNode, name);
    }

    public void setClassVariable(Node currentNode, String variableName, Object value) {
        model.setClassVariable(currentNode, variableName, value);
    }

    public Object removeClassVariable(Node currentNode, String name) {
        return model.removeClassVariable(currentNode, name);
    }

    public void addMethod(Node currentNode, InternalMethod method) {
        model.addMethod(currentNode, method);
    }

    public void removeMethod(String methodName) {
        model.removeMethod(methodName);
    }

    public void undefMethod(Node currentNode, String methodName) {
        model.undefMethod(currentNode, methodName);
    }

    public void undefMethod(Node currentNode, InternalMethod method) {
        model.undefMethod(currentNode, method);
    }

    public InternalMethod deepMethodSearch(String name) {
        return model.deepMethodSearch(name);
    }

    public void alias(Node currentNode, String newName, String oldName) {
        model.alias(currentNode, newName, oldName);
    }

    public void changeConstantVisibility(Node currentNode, String name, boolean isPrivate) {
        model.changeConstantVisibility(currentNode, name, isPrivate);
    }

    public RubyContext getContext() {
        return model.getContext();
    }

    public String getName() {
        return model.getName();
    }

    public boolean hasName() {
        return model.hasName();
    }

    public boolean hasPartialName() {
        return model.hasPartialName();
    }

    public String toString() {
        return model.toString();
    }

    public void newVersion() {
        model.newVersion();
    }

    public void newLexicalVersion() {
        model.newLexicalVersion();
    }

    public void newVersion(Set<RubyModule> alreadyInvalidated, boolean considerLexicalDependents) {
        model.newVersion(alreadyInvalidated, considerLexicalDependents);
    }

    public void addDependent(RubyModule dependent) {
        model.addDependent(dependent);
    }

    public void addLexicalDependent(RubyModule lexicalChild) {
        model.addLexicalDependent(lexicalChild);
    }

    public Assumption getUnmodifiedAssumption() {
        return model.getUnmodifiedAssumption();
    }

    public Map<String, RubyConstant> getConstants() {
        return model.getConstants();
    }

    public Map<String, InternalMethod> getMethods() {
        return model.getMethods();
    }

    public Map<String, Object> getClassVariables() {
        return model.getClassVariables();
    }

    public void visitObjectGraphChildren(ObjectSpaceManager.ObjectGraphVisitor visitor) {
        model.visitObjectGraphChildren(visitor);
    }

    public ModuleChain getParentModule() {
        return model.getParentModule();
    }

    public RubyModule getActualModule() {
        return model.getActualModule();
    }

    public Iterable<RubyModule> ancestors() {
        return model.ancestors();
    }

    public Iterable<RubyModule> parentAncestors() {
        return model.parentAncestors();
    }

    public Iterable<RubyModule> prependedAndIncludedModules() {
        return model.prependedAndIncludedModules();
    }

    public Collection<RubyBasicObject> filterMethods(boolean includeAncestors, MethodFilter filter) {
        return model.filterMethods(includeAncestors, filter);
    }

    public Collection<RubyBasicObject> filterMethodsOnObject(boolean includeAncestors, MethodFilter filter) {
        return model.filterMethodsOnObject(includeAncestors, filter);
    }

    public Collection<RubyBasicObject> filterSingletonMethods(boolean includeAncestors, MethodFilter filter) {
        return model.filterSingletonMethods(includeAncestors, filter);
    }

    private Collection<RubyBasicObject> filterMethods(Map<String, InternalMethod> allMethods, MethodFilter filter) {
        return model.filterMethods(allMethods, filter);
    }

}
