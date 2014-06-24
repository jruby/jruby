/*
 * Copyright (c) 2014 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.nodes.call;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.Node.Child;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.core.RubyProc;
import org.jruby.truffle.runtime.core.RubyString;
import org.jruby.truffle.runtime.core.RubySymbol;

public class CachedSymbolDynamicNameDispatchNode extends DynamicNameDispatchNode {

    private final RubySymbol cachedName;
    @Child protected DispatchHeadNode dispatchHeadNode;
    @Child protected DynamicNameDispatchNode next;

    public CachedSymbolDynamicNameDispatchNode(RubyContext context, RubySymbol cachedName, DynamicNameDispatchNode next) {
        super(context);
        this.cachedName = cachedName;
        dispatchHeadNode = new DispatchHeadNode(context, cachedName.toString(), false, DispatchHeadNode.MissingBehavior.CALL_METHOD_MISSING);
        this.next = next;
    }

    @Override
    public Object dispatch(VirtualFrame frame, Object receiverObject, RubySymbol name, RubyProc blockObject, Object[] argumentsObjects) {
        if (name != cachedName) {
            return next.dispatch(frame, receiverObject, name, blockObject, argumentsObjects);
        }

        return dispatchHeadNode.dispatch(frame, receiverObject, blockObject, argumentsObjects);
    }

    @Override
    public Object dispatch(VirtualFrame frame, Object receiverObject, RubyString name, RubyProc blockObject, Object[] argumentsObjects) {
        return dispatchHeadNode.dispatch(frame, receiverObject, blockObject, argumentsObjects);
    }

    @Override
    public boolean doesRespondTo(VirtualFrame frame, Object receiverObject, RubySymbol name) {
        if (name != cachedName) {
            return next.doesRespondTo(frame, receiverObject, name);
        }

        return dispatchHeadNode.doesRespondTo(frame, receiverObject);
    }

    @Override
    public boolean doesRespondTo(VirtualFrame frame, Object receiverObject, RubyString name) {
        return dispatchHeadNode.doesRespondTo(frame, receiverObject);
    }


}
