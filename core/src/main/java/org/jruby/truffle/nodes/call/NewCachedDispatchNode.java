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
import org.jruby.truffle.runtime.core.RubyString;
import org.jruby.truffle.runtime.core.RubySymbol;

public abstract class NewCachedDispatchNode extends NewDispatchNode {

    private final Object cachedName;

    @Child protected NewDispatchNode next;

    public NewCachedDispatchNode(RubyContext context, Object cachedName, NewDispatchNode next) {
        super(context);

        assert (cachedName instanceof String) || (cachedName instanceof RubySymbol) || (cachedName instanceof RubyString);
        this.cachedName = cachedName;

        this.next = next;
    }

    public NewCachedDispatchNode(NewCachedDispatchNode prev) {
        super(prev.getContext());
        cachedName = prev.cachedName;
        next = prev.next;
    }

    protected static final boolean isPrimitive(Object methodReceiverObject, Object callingSelf, Object receiverObject, Object methodName, Object blockObject, Object argumentsObjects) {
        return !(receiverObject instanceof  RubyBasicObject);
    }

    protected final boolean guardName(Object methodReceiverObject, Object callingSelf, Object receiverObject, Object methodName, Object blockObject, Object argumentsObjects) {
        if (cachedName instanceof String) {
            return cachedName.equals(methodName);
        } else if (cachedName instanceof RubySymbol) {
            return cachedName == methodName;
        } else if (cachedName instanceof RubyString) {
            return (methodName instanceof RubyString) && ((RubyString) cachedName).getBytes().equals(((RubyString) methodName).getBytes());
        } else {
            throw new UnsupportedOperationException();
        }
    }

}
