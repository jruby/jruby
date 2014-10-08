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

import com.oracle.truffle.api.Assumption;
import com.oracle.truffle.api.nodes.Node;
import org.jruby.truffle.runtime.core.RubyClass;
import org.jruby.truffle.runtime.core.RubyModule;
import org.jruby.truffle.runtime.methods.RubyMethod;

import java.util.Map;

public class IncludedModule implements ModuleChain {

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
    public RubyModule getLexicalParentModule() {
        return includedModule.getLexicalParentModule();
    }

    @Override
    public RubyModule getActualModule() {
        return includedModule;
    }

    @Override
    public Map<String, RubyConstant> getConstants() {
        return includedModule.getConstants();
    }

    @Override
    public Map<String, RubyMethod> getMethods() {
        return includedModule.getMethods();
    }

    @Override
    public Map<String, Object> getClassVariables() {
        return includedModule.getClassVariables();
    }

    @Override
    public RubyContext getContext() {
        return includedModule.getContext();
    }

    @Override
    public RubyClass getSingletonClass(Node currentNode) {
        return includedModule.getSingletonClass(currentNode);
    }

    @Override
    public void newVersion() {
        throw new UnsupportedOperationException();

    }

    @Override
    public void addDependent(ModuleChain dependent) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Assumption getUnmodifiedAssumption() {
        throw new UnsupportedOperationException();
    }

}
