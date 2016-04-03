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
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.interop.AcceptMessage;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.object.DynamicObject;
import org.jruby.truffle.RubyContext;
import org.jruby.truffle.RubyLanguage;
import org.jruby.truffle.language.RubyObjectType;
import org.jruby.truffle.language.dispatch.DispatchAction;
import org.jruby.truffle.language.dispatch.DispatchHeadNode;
import org.jruby.truffle.language.dispatch.MissingBehavior;

@AcceptMessage(value = "INVOKE", receiverType = RubyObjectType.class, language = RubyLanguage.class)
public final class ForeignInvokeNode extends ForeignInvokeBaseNode {

    @Child private Node findContextNode;
    @Child private DispatchHeadNode dispatchHeadNode;

    @Override
    public Object access(VirtualFrame frame, DynamicObject receiver, String name, Object[] arguments) {
        return getDispatchHeadNode().dispatch(frame, receiver, name, null, arguments);
    }

    private DispatchHeadNode getDispatchHeadNode() {
        if (dispatchHeadNode == null) {
            CompilerDirectives.transferToInterpreter();
            findContextNode = insert(RubyLanguage.INSTANCE.unprotectedCreateFindContextNode());
            final RubyContext context = RubyLanguage.INSTANCE.unprotectedFindContext(findContextNode);
            dispatchHeadNode = insert(new DispatchHeadNode(context, true, MissingBehavior.CALL_METHOD_MISSING, DispatchAction.CALL_METHOD));
        }

        return dispatchHeadNode;
    }

}
