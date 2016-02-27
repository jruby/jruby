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
import org.jruby.truffle.language.objects.WriteObjectFieldNode;
import org.jruby.truffle.language.objects.WriteObjectFieldNodeGen;

public class WriteGlobalVariableNode extends RubyNode {

    private final String name;

    @Child private RubyNode rhs;
    @Child private WriteObjectFieldNode writeNode;

    public WriteGlobalVariableNode(RubyContext context, SourceSection sourceSection, String name, RubyNode rhs) {
        super(context, sourceSection);
        this.name = name;
        this.rhs = rhs;
    }

    @Override
    public Object execute(VirtualFrame frame) {
        final Object value = rhs.execute(frame);
        getWriteNode().execute(coreLibrary().getGlobalVariablesObject(), value);
        return value;
    }

    @Override
    public Object isDefined(VirtualFrame frame) {
        return coreStrings().ASSIGNMENT.createInstance();
    }

    private WriteObjectFieldNode getWriteNode() {
        if (writeNode == null) {
            CompilerDirectives.transferToInterpreter();
            writeNode = insert(WriteObjectFieldNodeGen.create(getContext(), name));
        }

        return writeNode;
    }

}
