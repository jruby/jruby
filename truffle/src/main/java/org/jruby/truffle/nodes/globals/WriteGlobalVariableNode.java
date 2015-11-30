/*
 * Copyright (c) 2015 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.nodes.globals;

import org.jcodings.specific.UTF8Encoding;
import org.jruby.truffle.nodes.RubyNode;
import org.jruby.truffle.nodes.objectstorage.WriteHeadObjectFieldNode;
import org.jruby.truffle.nodes.objectstorage.WriteHeadObjectFieldNodeGen;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.core.StringOperations;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.source.SourceSection;

public class WriteGlobalVariableNode extends RubyNode {

    private final DynamicObject globalVariablesObject;

    @Child private RubyNode rhs;
    @Child private WriteHeadObjectFieldNode writeNode;

    public WriteGlobalVariableNode(RubyContext context, SourceSection sourceSection, String name, RubyNode rhs) {
        super(context, sourceSection);
        this.globalVariablesObject = context.getCoreLibrary().getGlobalVariablesObject();
        this.rhs = rhs;
        writeNode = WriteHeadObjectFieldNodeGen.create(name);
    }

    @Override
    public Object execute(VirtualFrame frame) {
        final Object value = rhs.execute(frame);
        writeNode.execute(globalVariablesObject, value);
        return value;
    }

    @Override
    public Object isDefined(VirtualFrame frame) {
        return create7BitString(StringOperations.encodeByteList("assignment", UTF8Encoding.INSTANCE));
    }

}
