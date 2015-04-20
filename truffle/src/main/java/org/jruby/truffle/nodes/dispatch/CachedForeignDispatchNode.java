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

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.interop.messages.*;
import com.oracle.truffle.interop.node.ForeignObjectAccessNode;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.source.SourceSection;
import org.jruby.truffle.nodes.RubyNode;
import org.jruby.truffle.nodes.interop.RubyToIndexLabelNode;
import org.jruby.truffle.nodes.interop.RubyToIndexLabelNodeFactory;
import org.jruby.truffle.runtime.LexicalScope;
import org.jruby.truffle.runtime.RubyArguments;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.core.RubyBasicObject;
import org.jruby.truffle.runtime.core.RubyNilClass;
import org.jruby.truffle.runtime.core.RubyProc;

public final class CachedForeignDispatchNode extends CachedDispatchNode {

    private final String name;
    private final String nameForMessage;
    private final int arity;

    @Child private ForeignObjectAccessNode directAccess;
    @Child private ForeignObjectAccessNode directWrite;
    @Child private ForeignObjectAccessNode access;
    @Child private PrepareArguments prepareArguments;

    public CachedForeignDispatchNode(RubyContext context, DispatchNode next, Object cachedName, int arity) {
        super(context, cachedName, next, false, DispatchAction.CALL_METHOD);

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
            directAccess = ForeignObjectAccessNode.getAccess(Read.create(Receiver.create(), Argument.create()));
        } else if (name.equals("[]=")) {
            directAccess = ForeignObjectAccessNode.getAccess(Write.create(Receiver.create(), Argument.create(), Argument.create()));
        } else if (name.endsWith("=") && arity == 1) {
            directWrite = ForeignObjectAccessNode.getAccess(Write.create(Receiver.create(), Argument.create(), Argument.create()));
        } else {
            // do not forget to pass the receiver!
            // EXECUTE(READ(rec, a0), a1<receiver>, ...)
            access = ForeignObjectAccessNode.getAccess(Execute.create(Read.create(Receiver.create(),Argument.create()),arity + 1));
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
            Object blockObject,
            Object argumentsObjects) {
        if (receiverObject instanceof TruffleObject) {
            return doDispatch(frame, (TruffleObject) receiverObject, argumentsObjects);
        } else {
            return next.executeDispatch(
                    frame,
                    receiverObject,
                    methodName,
                    (RubyProc) blockObject,
                    argumentsObjects);
        }
    }


    private Object doDispatch(VirtualFrame frame, TruffleObject receiverObject, Object argumentsObjects) {
        Object[] arguments = (Object[]) argumentsObjects;
        if (arguments.length != arity) {
            CompilerDirectives.transferToInterpreter();
            throw new IllegalStateException();
        }
        if (directAccess != null) {
            Object[] args = prepareArguments.convertArguments(frame, arguments, 0);
            return directAccess.executeForeign(frame, receiverObject, args);
        } else if (directWrite != null) {
            Object[] args = prepareArguments.convertArguments(frame, arguments, 1);
            args[0] = nameForMessage;
            return directWrite.executeForeign(frame, receiverObject, args);
        } else if (access != null) {
            Object[] args = prepareArguments.convertArguments(frame, arguments, 2);
            args[0] = name;
            args[1] = receiverObject;
            return access.executeForeign(frame, receiverObject, args);
        } else {
            CompilerDirectives.transferToInterpreter();
            throw new IllegalStateException();
        }
    }

    private static class PrepareArguments extends RubyNode {

        @Children private final RubyToIndexLabelNode[] converters;
        private final int arity;

        public PrepareArguments(RubyContext context, SourceSection sourceSection, int arity) {
            super(context, sourceSection);
            this.converters = new RubyToIndexLabelNode[arity];
            this.arity = arity;
            for (int i = 0; i < arity; i++) {
                this.converters[i] = RubyToIndexLabelNodeFactory.create(context, sourceSection, null);
            }
        }

        @ExplodeLoop
        public Object[] convertArguments(VirtualFrame frame, Object[] arguments, int offset) {
            Object[] result = new Object[arity + offset];
            for (int i = 0; i < arity; i++) {
                result[i + offset] = converters[i].executeWithTarget(frame, arguments[i]);
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
