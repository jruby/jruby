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
import org.jruby.truffle.runtime.ModuleChain;

/**
 * A reference to an included RubyModule.
 */
public class IncludedModule implements ModuleChain {
    private final RubyModule includedModule;
    @CompilerDirectives.CompilationFinal
    private ModuleChain parentModule;

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
