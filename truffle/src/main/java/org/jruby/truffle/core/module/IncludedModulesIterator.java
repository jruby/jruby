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

public class IncludedModulesIterator extends AncestorIterator {
    private final ModuleFields currentModule;

    public IncludedModulesIterator(ModuleChain top, ModuleFields currentModule) {
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
