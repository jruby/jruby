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

import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.core.RubyBasicObject;

public abstract class NewCachedDispatchNode extends NewDispatchNode {

    @Child protected NewDispatchNode next;

    public NewCachedDispatchNode(RubyContext context, NewDispatchNode next) {
        super(context);
        this.next = next;
    }

    public NewCachedDispatchNode(NewCachedDispatchNode prev) {
        super(prev.getContext());
    }

    protected static final boolean isPrimitive(Object methodReceiverObject, Object callingSelf, Object receiverObject, Object methodName, Object blockObject, Object argumentsObjects) {
        return !(receiverObject instanceof  RubyBasicObject);
    }

}
