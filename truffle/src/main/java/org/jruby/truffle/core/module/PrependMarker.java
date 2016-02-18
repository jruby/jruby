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

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.object.DynamicObject;

public class PrependMarker implements ModuleChain {
    @CompilerDirectives.CompilationFinal
    private ModuleChain parentModule;

    public PrependMarker(ModuleChain parentModule) {
        assert parentModule != null;
        this.parentModule = parentModule;
    }

    @Override
    public ModuleChain getParentModule() {
        return parentModule;
    }

    @Override
    public DynamicObject getActualModule() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void insertAfter(DynamicObject module) {
        parentModule = new IncludedModule(module, parentModule);
    }

}
