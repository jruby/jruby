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
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.core.RubyProc;
import org.jruby.truffle.runtime.core.RubyString;
import org.jruby.truffle.runtime.core.RubySymbol;

public class CachedStringDynamicNameDispatchNode extends DynamicNameDispatchNode {

    private final RubyString cachedName;
    @Child protected DispatchHeadNode dispatchHeadNode;
    @Child protected DynamicNameDispatchNode next;

    public CachedStringDynamicNameDispatchNode(RubyContext context, RubyString cachedName, DynamicNameDispatchNode next) {
        super(context);
        this.cachedName = cachedName;
        dispatchHeadNode = new DispatchHeadNode(context, cachedName.toString(), false, DispatchHeadNode.MissingBehavior.CALL_METHOD_MISSING);
        this.next = next;
    }

    @Override
    public Object dispatch(VirtualFrame frame, Object receiverObject, RubySymbol name, RubyProc blockObject, Object[] argumentsObjects) {
        return dispatchHeadNode.dispatch(frame, receiverObject, blockObject, argumentsObjects);
    }

    @Override
    public Object dispatch(VirtualFrame frame, Object receiverObject, RubyString name, RubyProc blockObject, Object[] argumentsObjects) {
        // TODO(CS): how to compare strings efficiently?
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean doesRespondTo(VirtualFrame frame, Object receiverObject, RubySymbol name) {
        return dispatchHeadNode.doesRespondTo(frame, receiverObject);
    }

    @Override
    public boolean doesRespondTo(VirtualFrame frame, Object receiverObject, RubyString name) {
        // TODO(CS): how to compare strings efficiently?
        throw new UnsupportedOperationException();
    }


}
