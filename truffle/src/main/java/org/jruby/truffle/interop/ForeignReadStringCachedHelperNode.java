/*
 * Copyright (c) 2013, 2016 Oracle and/or its affiliates. All rights reserved. This
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
import org.jruby.truffle.language.objects.ReadObjectFieldNode;
import org.jruby.truffle.language.objects.ReadObjectFieldNodeGen;

@NodeChildren({
        @NodeChild("receiver"),
        @NodeChild("name"),
        @NodeChild("stringName"),
        @NodeChild("isIVar")
})
abstract class ForeignReadStringCachedHelperNode extends RubyNode {

    @Child private DoesRespondDispatchHeadNode definedNode;
    @Child private DoesRespondDispatchHeadNode indexDefinedNode;
    @Child private CallDispatchHeadNode callNode;

    protected final static String INDEX_METHOD_NAME = "[]";

    public abstract Object executeStringCachedHelper(VirtualFrame frame, DynamicObject receiver, Object name, String stringName, boolean isIVar);

    @Specialization(guards = "isIVar")
    public Object readInstanceVariable(
            DynamicObject receiver,
            Object name,
            String stringName,
            boolean isIVar,
            @Cached("createReadObjectFieldNode(stringName)") ReadObjectFieldNode readObjectFieldNode) {
        return readObjectFieldNode.execute(receiver);
    }

    protected ReadObjectFieldNode createReadObjectFieldNode(String name) {
        return ReadObjectFieldNodeGen.create(name, nil());
    }

    @Specialization(guards = {
            "!isIVar",
            "methodDefined(frame, receiver, stringName, getDefinedNode())"
    })
    public Object callMethod(
            VirtualFrame frame,
            DynamicObject receiver,
            Object name,
            String stringName,
            boolean isIVar) {
        return getCallNode().call(frame, receiver, stringName, null);
    }

    @Specialization(guards = {
            "!isIVar",
            "!methodDefined(frame, receiver, stringName, getDefinedNode())",
            "methodDefined(frame, receiver, INDEX_METHOD_NAME, getIndexDefinedNode())"
    })
    public Object index(
            VirtualFrame frame,
            DynamicObject receiver,
            Object name,
            String stringName,
            boolean isIVar) {
        return getCallNode().call(frame, receiver, "[]", null, name);
    }

    protected DoesRespondDispatchHeadNode getDefinedNode() {
        if (definedNode == null) {
            CompilerDirectives.transferToInterpreter();
            definedNode = insert(new DoesRespondDispatchHeadNode(getContext(), true));
        }

        return definedNode;
    }

    protected DoesRespondDispatchHeadNode getIndexDefinedNode() {
        if (indexDefinedNode == null) {
            CompilerDirectives.transferToInterpreter();
            indexDefinedNode = insert(new DoesRespondDispatchHeadNode(getContext(), true));
        }

        return indexDefinedNode;
    }

    protected boolean methodDefined(VirtualFrame frame, DynamicObject receiver, String stringName,
                                    DoesRespondDispatchHeadNode definedNode) {
        return definedNode.doesRespondTo(frame, stringName, receiver);
    }

    protected CallDispatchHeadNode getCallNode() {
        if (callNode == null) {
            CompilerDirectives.transferToInterpreter();
            callNode = insert(DispatchHeadNodeFactory.createMethodCall(getContext(), true));
        }

        return callNode;
    }

}
