/*
 * Copyright (c) 2013, 2015 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.nodes.objects;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.source.SourceSection;
import org.jruby.truffle.nodes.RubyNode;
import org.jruby.truffle.runtime.LexicalScope;
import org.jruby.truffle.runtime.ModuleOperations;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.core.RubyModule;

public class WriteClassVariableNode extends RubyNode {

    private final String name;
    private final LexicalScope lexicalScope;
    @Child private RubyNode rhs;

    public WriteClassVariableNode(RubyContext context, SourceSection sourceSection, String name, LexicalScope lexicalScope, RubyNode rhs) {
        super(context, sourceSection);
        this.name = name;
        this.lexicalScope = lexicalScope;
        this.rhs = rhs;
    }

    @Override
    public Object execute(VirtualFrame frame) {
        notDesignedForCompilation();

        final RubyModule moduleObject = lexicalScope.getLiveModule();

        final Object rhsValue = rhs.execute(frame);

        ModuleOperations.setClassVariable(moduleObject, name, rhsValue, this);

        return rhsValue;
    }

    @Override
    public Object isDefined(VirtualFrame frame) {
        return getContext().makeString("assignment");
    }

}
