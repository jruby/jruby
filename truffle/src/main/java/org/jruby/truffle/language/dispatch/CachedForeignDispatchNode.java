/*
 * Copyright (c) 2014, 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.language.dispatch;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.interop.ArityException;
import com.oracle.truffle.api.interop.ForeignAccess;
import com.oracle.truffle.api.interop.Message;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.interop.UnsupportedTypeException;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.source.SourceSection;
import org.jruby.truffle.RubyContext;
import org.jruby.truffle.interop.RubyToForeignNode;
import org.jruby.truffle.interop.RubyToForeignNodeGen;
import org.jruby.truffle.language.RubyNode;

public final class CachedForeignDispatchNode extends CachedDispatchNode {

    private final String name;
    private final String nameForMessage;
    private final int arity;

    @Child private Node directArray;
    @Child private Node directField;
    @Child private Node directCall;
    @Child private Node nullCheck;
    @Child private Node access;
    @Child private PrepareArguments prepareArguments;
    

    public CachedForeignDispatchNode(RubyContext context, DispatchNode next, Object cachedName, int arity) {
        super(context, cachedName, next, DispatchAction.CALL_METHOD);

        this.name = cachedName.toString();
        this.arity = arity;
        if (name.endsWith("=") && arity == 1) {
            this.nameForMessage = name.substring(0, name.length() - 1);
        } else {
            this.nameForMessage = name;
        }
        initializeNodes(context, arity);
    }

    private void initializeNodes(RubyContext context, int arity) {
        if (name.equals("[]")) {
            directArray = Message.READ.createNode();
        } else if (name.equals("[]=")) {
            directArray = Message.WRITE.createNode();
        } else if (name.endsWith("=") && arity == 1) {
            directField = Message.WRITE.createNode();
        } else if (name.equals("call")) {
        	directCall = Message.createExecute(arity).createNode();
        } else if (name.equals("nil?")) {
            nullCheck = Message.IS_NULL.createNode();
        } else {
            access = Message.createInvoke(arity).createNode();
        }
        prepareArguments = new PrepareArguments(context, getSourceSection(), arity);
    }

    @Override
    protected boolean guard(Object methodName, Object receiver) {
        // TODO CS 8-Mar-15 not sure what the guards are supposed to be here
        return true;
    }

    @Override
    public Object executeDispatch(
            VirtualFrame frame,
            Object receiverObject,
            Object methodName,
            DynamicObject blockObject,
            Object[] argumentsObjects) {
        if (receiverObject instanceof TruffleObject) {
            return doDispatch(frame, (TruffleObject) receiverObject, argumentsObjects);
        } else {
            return next.executeDispatch(
                    frame,
                    receiverObject,
                    methodName,
                    blockObject,
                    argumentsObjects);
        }
    }


    private Object doDispatch(VirtualFrame frame, TruffleObject receiverObject, Object[] arguments) {
        if (arguments.length != arity) {
            CompilerDirectives.transferToInterpreter();
            throw new IllegalStateException();
        }

        try {
            if (directArray != null) {
                Object[] args = prepareArguments.convertArguments(frame, arguments, 0);
                return ForeignAccess.sendExecute(directArray, frame, receiverObject, args);
            } else if (directField != null) {
                Object[] args = prepareArguments.convertArguments(frame, arguments, 1);
                args[0] = nameForMessage;
                return ForeignAccess.sendExecute(directField, frame, receiverObject, args);
            } else if (directCall != null) {
                Object[] args;
                args = prepareArguments.convertArguments(frame, arguments, 0);
                return ForeignAccess.sendExecute(directCall, frame, receiverObject, args);
            } else if (nullCheck != null) {
                Object[] args = prepareArguments.convertArguments(frame, arguments, 0);
                return ForeignAccess.sendExecute(nullCheck, frame, receiverObject, args);
            } else if (access != null) {
                Object[] args = prepareArguments.convertArguments(frame, arguments, 1);
                args[0] = name;
                return ForeignAccess.sendExecute(access, frame, receiverObject, args);
            } else {
                CompilerDirectives.transferToInterpreter();
                throw new IllegalStateException();
            }
        } catch (ArityException | UnsupportedMessageException | UnsupportedTypeException e) {
            CompilerDirectives.transferToInterpreter();
            throw new RuntimeException(e);
        }
    }

    private static class PrepareArguments extends RubyNode {

        @Children private final RubyToForeignNode[] converters;
        private final int arity;

        public PrepareArguments(RubyContext context, SourceSection sourceSection, int arity) {
            super(context, sourceSection);
            this.converters = new RubyToForeignNode[arity];
            this.arity = arity;
            for (int i = 0; i < arity; i++) {
                this.converters[i] = RubyToForeignNodeGen.create(context, sourceSection, null);
            }
        }

        @ExplodeLoop
        public Object[] convertArguments(VirtualFrame frame, Object[] arguments, int offset) {
            Object[] result = new Object[arity + offset];
            for (int i = 0; i < arity; i++) {
                result[i + offset] = converters[i].executeConvert(frame, arguments[i]);
            }
            return result;
        }

        @Override
        public Object execute(VirtualFrame frame) {
            CompilerDirectives.transferToInterpreter();
            throw new IllegalStateException();
        }
    }
}
