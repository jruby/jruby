/*
 * Copyright (c) 2014, 2015 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.nodes.dispatch;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.interop.ForeignAccess;
import com.oracle.truffle.api.interop.Message;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.nodes.Node;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.array.ArrayUtils;
import com.oracle.truffle.api.object.DynamicObject;

public final class CachedForeignGlobalDispatchNode extends CachedDispatchNode {

    private final Object cachedName;
    private final TruffleObject language;
    private final int numberOfArguments;

    @Child private Node access;

    public CachedForeignGlobalDispatchNode(RubyContext context, DispatchNode next, Object cachedName, TruffleObject language, int numberOfArguments) {
        super(context, cachedName, next, false, DispatchAction.CALL_METHOD);
        this.cachedName = cachedName;
        this.language = language;
        this.numberOfArguments = numberOfArguments;
        this.access = create();
    }

    private Node create() {
        // + 1 because language is the first argument
        return Message.createInvoke(numberOfArguments + 1).createNode();
    }

    @Override
    public Object executeDispatch(
            VirtualFrame frame,
            Object receiverObject,
            Object methodName,
            Object blockObject,
            Object argumentsObjects) {
        if (receiverObject instanceof  DynamicObject) {
            Object[] arguments = (Object[]) argumentsObjects;
            if (arguments.length == numberOfArguments) {
                Object[] args = new Object[arguments.length + 2];
                ArrayUtils.arraycopy(arguments, 0, args, 2, arguments.length);
                args[0] = cachedName;
                args[1] = language;
                return ForeignAccess.execute(access, frame, language, args);
            } else {
                CompilerDirectives.transferToInterpreter();
                throw new IllegalStateException("Varargs are not supported");
            }
        } else {
            CompilerDirectives.transferToInterpreter();
            throw new IllegalStateException("Should not happen");
        }
    }

    @Override
    protected boolean guard(Object methodName, Object receiver) {
        // TODO CS 8-Mar-15 not sure what is going on with the guards in this node
        return true;
    }

}