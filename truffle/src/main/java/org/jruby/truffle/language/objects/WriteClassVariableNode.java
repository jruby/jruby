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
import org.jruby.truffle.core.module.ModuleOperations;
import org.jruby.truffle.language.LexicalScope;
import org.jruby.truffle.language.RubyNode;

public class WriteClassVariableNode extends RubyNode {

    private final String name;
    private final LexicalScope lexicalScope;

    @Child private RubyNode rhs;

    public WriteClassVariableNode(LexicalScope lexicalScope,
                                  String name, RubyNode rhs) {
        this.lexicalScope = lexicalScope;
        this.name = name;
        this.rhs = rhs;
    }

    @Override
    public Object execute(VirtualFrame frame) {
        final Object rhsValue = rhs.execute(frame);

        // TODO CS 21-Feb-16 these two operations are uncached and use loops

        final DynamicObject module = lexicalScope.resolveTargetModuleForClassVariables();

        ModuleOperations.setClassVariable(getContext(), module, name, rhsValue, this);

        return rhsValue;
    }

    @Override
    public Object isDefined(VirtualFrame frame) {
        return coreStrings().ASSIGNMENT.createInstance();
    }

}
