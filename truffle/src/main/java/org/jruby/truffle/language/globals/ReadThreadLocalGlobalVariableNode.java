/*
 * Copyright (c) 2015, 2017 Oracle and/or its affiliates. All rights reserved. This
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
import com.oracle.truffle.api.object.DynamicObject;
import org.jruby.truffle.language.RubyNode;
import org.jruby.truffle.language.objects.ReadObjectFieldNode;
import org.jruby.truffle.language.objects.ReadObjectFieldNodeGen;
import org.jruby.truffle.language.threadlocal.ThreadLocalObjectNode;
import org.jruby.truffle.language.threadlocal.ThreadLocalObjectNodeGen;

public class ReadThreadLocalGlobalVariableNode extends RubyNode {

    private final String name;
    private final boolean alwaysDefined;

    @Child private ThreadLocalObjectNode threadLocalVariablesObjectNode;
    @Child private ReadObjectFieldNode readNode;

    public ReadThreadLocalGlobalVariableNode(String name, boolean alwaysDefined) {
        this.name = name;
        this.alwaysDefined = alwaysDefined;
    }

    @Override
    public Object execute(VirtualFrame frame) {
        final DynamicObject threadLocalVariablesObject = getThreadLocalVariablesObjectNode().executeDynamicObject(frame);
        return getReadNode().execute(threadLocalVariablesObject);
    }

    @Override
    public Object isDefined(VirtualFrame frame) {
        if (alwaysDefined || execute(frame) != nil()) {
            return coreStrings().GLOBAL_VARIABLE.createInstance();
        } else {
            return nil();
        }
    }

    private ThreadLocalObjectNode getThreadLocalVariablesObjectNode() {
        if (threadLocalVariablesObjectNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            threadLocalVariablesObjectNode = insert(ThreadLocalObjectNodeGen.create());
        }

        return threadLocalVariablesObjectNode;
    }

    private ReadObjectFieldNode getReadNode() {
        if (readNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            readNode = insert(ReadObjectFieldNodeGen.create(name, nil()));
        }

        return readNode;
    }


}
