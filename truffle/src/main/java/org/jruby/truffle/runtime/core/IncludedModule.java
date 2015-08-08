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

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.object.DynamicObjectFactory;
import org.jruby.truffle.nodes.RubyGuards;
import org.jruby.truffle.nodes.core.ModuleNodes;
import org.jruby.truffle.runtime.ModuleChain;

/**
 * A reference to an included RubyModule.
 */
public class IncludedModule implements ModuleChain {
    private final RubyBasicObject includedModule;
    @CompilerDirectives.CompilationFinal
    private ModuleChain parentModule;

    public IncludedModule(RubyBasicObject includedModule, ModuleChain parentModule) {
        assert RubyGuards.isRubyModule(includedModule);
        this.includedModule = includedModule;
        this.parentModule = parentModule;
    }

    @Override
    public ModuleChain getParentModule() {
        return parentModule;
    }

    @Override
    public RubyBasicObject getActualModule() {
        return includedModule;
    }

    @Override
    public String toString() {
        return super.toString() + "(" + includedModule + ")";
    }

    @Override
    public void insertAfter(RubyBasicObject module) {
        parentModule = new IncludedModule(module, parentModule);
    }

    @Override
    public DynamicObjectFactory getFactory() {
        return ModuleNodes.getModel(includedModule).getFactory();
    }
}
