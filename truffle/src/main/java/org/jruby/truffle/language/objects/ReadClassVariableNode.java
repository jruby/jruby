/*
 * Copyright (c) 2013, 2017 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.language.objects;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.profiles.BranchProfile;
import org.jruby.truffle.core.module.ModuleOperations;
import org.jruby.truffle.language.LexicalScope;
import org.jruby.truffle.language.RubyNode;
import org.jruby.truffle.language.control.RaiseException;

public class ReadClassVariableNode extends RubyNode {

    private final LexicalScope lexicalScope;
    private final String name;

    private final BranchProfile missingProfile = BranchProfile.create();

    public ReadClassVariableNode(LexicalScope lexicalScope, String name) {
        this.lexicalScope = lexicalScope;
        this.name = name;
    }

    @Override
    public Object execute(VirtualFrame frame) {
        // TODO CS 21-Feb-16 these two operations are uncached and use loops - same for isDefined below

        final DynamicObject module = lexicalScope.resolveTargetModuleForClassVariables();

        final Object value = ModuleOperations.lookupClassVariable(module, name);

        if (value == null) {
            missingProfile.enter();
            throw new RaiseException(coreExceptions().nameErrorUninitializedClassVariable(module, name, this));
        }

        return value;
    }

    @Override
    public Object isDefined(VirtualFrame frame) {
        final DynamicObject module = lexicalScope.resolveTargetModuleForClassVariables();

        final Object value = ModuleOperations.lookupClassVariable(module, name);

        if (value == null) {
            return nil();
        } else {
            return coreStrings().CLASS_VARIABLE.createInstance();
        }
    }

}
