/*
 * Copyright (c) 2015, 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.language.globals;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.source.SourceSection;
import org.jruby.truffle.RubyContext;
import org.jruby.truffle.language.RubyNode;
import org.jruby.truffle.language.objects.ReadObjectFieldNode;
import org.jruby.truffle.language.objects.ReadObjectFieldNodeGen;

public class ReadGlobalVariableNode extends RubyNode {

    private final String name;

    @Child private ReadObjectFieldNode readNode;

    public ReadGlobalVariableNode(RubyContext context, SourceSection sourceSection, String name) {
        super(context, sourceSection);
        this.name = name;
    }

    @Override
    public Object execute(VirtualFrame frame) {
        return getReadNode().execute(coreLibrary().getGlobalVariablesObject());
    }

    @Override
    public Object isDefined(VirtualFrame frame) {
        if (getReadNode().execute(coreLibrary().getGlobalVariablesObject()) != nil()) {
            return coreStrings().GLOBAL_VARIABLE.createInstance();
        } else {
            return nil();
        }
    }

    private ReadObjectFieldNode getReadNode() {
        if (readNode == null) {
            CompilerDirectives.transferToInterpreter();
            readNode = insert(ReadObjectFieldNodeGen.create(getContext(), name, nil()));
        }

        return readNode;
    }

}
