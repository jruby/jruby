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

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.source.SourceSection;
import org.jcodings.specific.UTF8Encoding;
import org.jruby.truffle.nodes.RubyNode;
import org.jruby.truffle.nodes.ThreadLocalObjectNode;
import org.jruby.truffle.nodes.objectstorage.ReadHeadObjectFieldNode;
import org.jruby.truffle.nodes.objectstorage.ReadHeadObjectFieldNodeGen;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.core.StringOperations;

public class ReadThreadLocalGlobalVariableNode extends RubyNode {

    @Child private ThreadLocalObjectNode threadLocalVariablesObjectNode;
    @Child private ReadHeadObjectFieldNode readNode;

    public ReadThreadLocalGlobalVariableNode(RubyContext context, SourceSection sourceSection, String name) {
        super(context, sourceSection);
        this.threadLocalVariablesObjectNode = new ThreadLocalObjectNode(context, sourceSection);
        readNode = ReadHeadObjectFieldNodeGen.create(name, nil());
    }

    @Override
    public Object execute(VirtualFrame frame) {
        final DynamicObject threadLocalVariablesObject = threadLocalVariablesObjectNode.executeDynamicObject(frame);
        return readNode.execute(threadLocalVariablesObject);
    }

    @Override
    public Object isDefined(VirtualFrame frame) {
        CompilerDirectives.transferToInterpreter();
        final DynamicObject threadLocalVariablesObject = threadLocalVariablesObjectNode.executeDynamicObject(frame);

        if (readNode.getName().equals("$~") || readNode.getName().equals("$!") || readNode.execute(threadLocalVariablesObject) != nil()) {
            return create7BitString("global-variable", UTF8Encoding.INSTANCE);
        } else {
            return nil();
        }
    }

}
