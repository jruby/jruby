/*
 * Copyright (c) 2014 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.nodes.dispatch;

import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.core.RubyBasicObject;
import org.jruby.truffle.runtime.util.ArrayUtils;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.interop.messages.Argument;
import com.oracle.truffle.interop.messages.Execute;
import com.oracle.truffle.interop.messages.Read;
import com.oracle.truffle.interop.messages.Receiver;
import com.oracle.truffle.interop.node.ForeignObjectAccessNode;

public final class CachedForeignGlobalDispatchNode extends CachedDispatchNode {

    private final Object cachedName;
    private final TruffleObject language;
    private final int numberOfArguments;

    @Child private ForeignObjectAccessNode access;

    public CachedForeignGlobalDispatchNode(RubyContext context, DispatchNode next, Object cachedName, TruffleObject language, int numberOfArguments) {
        super(context, cachedName, next, false, DispatchAction.CALL_METHOD);
        this.cachedName = cachedName;
        this.language = language;
        this.numberOfArguments = numberOfArguments;
        this.access = create();
    }

    private ForeignObjectAccessNode create() {
        // + 1 because language is the first argument
        return ForeignObjectAccessNode.getAccess(Execute.create(Read.create(Receiver.create(), Argument.create()), numberOfArguments + 1));
    }

    @Override
    public Object executeDispatch(
            VirtualFrame frame,
            Object receiverObject,
            Object methodName,
            Object blockObject,
            Object argumentsObjects) {
        if (receiverObject instanceof  RubyBasicObject) {
            Object[] arguments = (Object[]) argumentsObjects;
            if (arguments.length == numberOfArguments) {
                Object[] args = new Object[arguments.length + 2];
                ArrayUtils.arraycopy(arguments, 0, args, 2, arguments.length);
                args[0] = cachedName;
                args[1] = language;
                return access.executeForeign(frame, language, args);
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