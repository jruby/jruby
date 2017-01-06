/*
 * Copyright (c) 2013, 2017 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.interop;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.NodeChildren;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.object.DynamicObject;
import org.jruby.truffle.language.RubyNode;
import org.jruby.truffle.language.dispatch.CallDispatchHeadNode;
import org.jruby.truffle.language.dispatch.DispatchHeadNodeFactory;
import org.jruby.truffle.language.dispatch.DoesRespondDispatchHeadNode;
import org.jruby.truffle.language.objects.WriteObjectFieldNode;
import org.jruby.truffle.language.objects.WriteObjectFieldNodeGen;

@NodeChildren({
        @NodeChild("receiver"),
        @NodeChild("name"),
        @NodeChild("stringName"),
        @NodeChild("isIVar"),
        @NodeChild("value")
})
abstract class ForeignWriteStringCachedHelperNode extends RubyNode {

    @Child private DoesRespondDispatchHeadNode definedNode;
    @Child private DoesRespondDispatchHeadNode indexDefinedNode;
    @Child private CallDispatchHeadNode callNode;

    protected final static String INDEX_SET_METHOD_NAME = "[]=";

    public abstract Object executeStringCachedHelper(VirtualFrame frame, DynamicObject receiver, Object name,
            Object stringName, boolean isIVar, Object value);

    @Specialization(guards = "isIVar")
    public Object readInstanceVariable(
            DynamicObject receiver,
            Object name,
            Object stringName,
            boolean isIVar,
            Object value,
            @Cached("createWriteObjectFieldNode(stringName)") WriteObjectFieldNode writeObjectFieldNode) {
        writeObjectFieldNode.execute(receiver, value);
        return value;
    }

    protected WriteObjectFieldNode createWriteObjectFieldNode(Object name) {
        return WriteObjectFieldNodeGen.create(name);
    }

    @Specialization(guards = { "not(isIVar)", "methodDefined(frame, receiver, writeMethodName, getDefinedNode())" })
    public Object callMethod(
            VirtualFrame frame,
            DynamicObject receiver,
            Object name,
            Object stringName,
            boolean isIVar,
            Object value,
            @Cached("createWriteMethodName(stringName)") String writeMethodName) {
        return getCallNode().call(frame, receiver, writeMethodName, value);
    }

    // Workaround for DSL bug
    protected boolean not(boolean value) {
        return !value;
    }

    protected String createWriteMethodName(Object name) {
        return name + "=";
    }

    @Specialization(guards = {
            "!isIVar",
            "!methodDefined(frame, receiver, writeMethodName, getDefinedNode())",
            "methodDefined(frame, receiver, INDEX_SET_METHOD_NAME, getIndexDefinedNode())"
    })
    public Object index(
            VirtualFrame frame,
            DynamicObject receiver,
            Object name,
            Object stringName,
            boolean isIVar,
            Object value,
            @Cached("createWriteMethodName(stringName)") String writeMethodName) {
        return getCallNode().call(frame, receiver, "[]=", name, value);
    }

    protected DoesRespondDispatchHeadNode getDefinedNode() {
        if (definedNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            definedNode = insert(new DoesRespondDispatchHeadNode(true));
        }

        return definedNode;
    }

    protected DoesRespondDispatchHeadNode getIndexDefinedNode() {
        if (indexDefinedNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            indexDefinedNode = insert(new DoesRespondDispatchHeadNode(true));
        }

        return indexDefinedNode;
    }

    protected boolean methodDefined(VirtualFrame frame, DynamicObject receiver, Object stringName,
                                    DoesRespondDispatchHeadNode definedNode) {
        if (stringName == null) {
            return false;
        } else {
            return definedNode.doesRespondTo(frame, stringName, receiver);
        }
    }

    protected CallDispatchHeadNode getCallNode() {
        if (callNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            callNode = insert(DispatchHeadNodeFactory.createMethodCall(true));
        }

        return callNode;
    }

}
