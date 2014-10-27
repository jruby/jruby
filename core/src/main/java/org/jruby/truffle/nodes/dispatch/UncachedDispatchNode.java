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

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.IndirectCallNode;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.Truffle;
import org.jruby.truffle.nodes.cast.BoxingNode;
import org.jruby.truffle.runtime.RubyArguments;
import org.jruby.truffle.runtime.RubyConstant;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.control.RaiseException;
import org.jruby.truffle.runtime.core.*;
import org.jruby.truffle.runtime.methods.RubyMethod;
import org.jruby.truffle.runtime.LexicalScope;

public abstract class UncachedDispatchNode extends DispatchNode {

    private final boolean ignoreVisibility;

    @Child protected IndirectCallNode callNode;
    @Child protected BoxingNode box;

    public UncachedDispatchNode(RubyContext context, boolean ignoreVisibility) {
        super(context);
        this.ignoreVisibility = ignoreVisibility;
        callNode = Truffle.getRuntime().createIndirectCallNode();
        box = new BoxingNode(context, null, null);
    }

    public UncachedDispatchNode(UncachedDispatchNode prev) {
        super(prev);
        ignoreVisibility = prev.ignoreVisibility;
        callNode = prev.callNode;
        box = prev.box;
    }

    @Specialization(guards = "actionIsReadConstant")
    public Object dispatchReadConstant(
            VirtualFrame frame,
            Object methodReceiverObject,
            LexicalScope lexicalScope,
            RubyModule receiverObject,
            Object methodName,
            Object blockObject,
            Object argumentsObjects,
            Dispatch.DispatchAction dispatchAction) {
        // Need to be much more fine grained with TruffleBoundary here
        CompilerDirectives.transferToInterpreter();

        final RubyConstant constant = lookupConstant(lexicalScope, receiverObject,
                methodName.toString(), ignoreVisibility, dispatchAction);

        if (constant != null) {
            return constant.getValue();
        }

        final RubyClass callerClass = box.box(RubyArguments.getSelf(frame.getArguments())).getMetaClass();

        final RubyMethod missingMethod = lookup(callerClass, receiverObject, "const_missing", ignoreVisibility,
                dispatchAction);

        if (missingMethod == null) {
            throw new RaiseException(getContext().getCoreLibrary().runtimeError(
                    receiverObject.toString() + " didn't have a #const_missing", this));
        }

        final RubySymbol methodNameAsSymbol;

        if (methodName instanceof RubySymbol) {
            methodNameAsSymbol = (RubySymbol) methodName;
        } else if (methodName instanceof RubyString) {
            methodNameAsSymbol = getContext().newSymbol(((RubyString) methodName).getBytes());
        } else if (methodName instanceof String) {
            methodNameAsSymbol = getContext().newSymbol((String) methodName);
        } else {
            throw new UnsupportedOperationException();
        }

        return callNode.call(
                frame,
                missingMethod.getCallTarget(),
                RubyArguments.pack(
                        missingMethod,
                        missingMethod.getDeclarationFrame(),
                        receiverObject,
                        null,
                        new Object[]{methodNameAsSymbol}));
    }

    @Specialization(guards = "actionIsCallOrRespondToMethod")
    public Object dispatch(
            VirtualFrame frame,
            Object methodReceiverObject,
            LexicalScope lexicalScope,
            RubyBasicObject receiverObject,
            Object methodName,
            Object blockObject,
            Object argumentsObjects,
            Dispatch.DispatchAction dispatchAction) {
        // Need to be much more fine grained with TruffleBoundary here
        CompilerDirectives.transferToInterpreter();

        final RubyClass callerClass = box.box(RubyArguments.getSelf(frame.getArguments())).getMetaClass();

        final RubyMethod method = lookup(callerClass, receiverObject, methodName.toString(),
                ignoreVisibility, dispatchAction);

        if (method != null) {
            if (dispatchAction == Dispatch.DispatchAction.CALL_METHOD) {
                return callNode.call(
                        frame,
                        method.getCallTarget(),
                        RubyArguments.pack(
                                method,
                                method.getDeclarationFrame(),
                                receiverObject,
                                (RubyProc) blockObject,
                                CompilerDirectives.unsafeCast(argumentsObjects, Object[].class, true)));
            } else if (dispatchAction == Dispatch.DispatchAction.RESPOND_TO_METHOD) {
                return true;
            } else {
                throw new UnsupportedOperationException();
            }
        }

        final RubyMethod missingMethod = lookup(callerClass, receiverObject, "method_missing", true,
                dispatchAction);

        if (missingMethod == null) {
            if (dispatchAction == Dispatch.DispatchAction.RESPOND_TO_METHOD) {
                return false;
            } else {
                throw new RaiseException(getContext().getCoreLibrary().runtimeError(
                        receiverObject.toString() + " didn't have a #method_missing", this));
            }
        }

        if (dispatchAction == Dispatch.DispatchAction.CALL_METHOD) {
            final Object[] argumentsObjectsArray = CompilerDirectives.unsafeCast(argumentsObjects, Object[].class, true);

            final Object[] modifiedArgumentsObjects = new Object[1 + argumentsObjectsArray.length];

            final RubySymbol methodNameAsSymbol;

            if (methodName instanceof RubySymbol) {
                methodNameAsSymbol = (RubySymbol) methodName;
            } else if (methodName instanceof RubyString) {
                methodNameAsSymbol = getContext().newSymbol(((RubyString) methodName).getBytes());
            } else if (methodName instanceof String) {
                methodNameAsSymbol = getContext().newSymbol((String) methodName);
            } else {
                throw new UnsupportedOperationException();
            }

            modifiedArgumentsObjects[0] = methodNameAsSymbol;

            System.arraycopy(argumentsObjectsArray, 0, modifiedArgumentsObjects, 1, argumentsObjectsArray.length);

            return callNode.call(
                    frame,
                    missingMethod.getCallTarget(),
                    RubyArguments.pack(
                            missingMethod,
                            missingMethod.getDeclarationFrame(),
                            receiverObject,
                            (RubyProc) blockObject,
                            modifiedArgumentsObjects));
        } else if (dispatchAction == Dispatch.DispatchAction.RESPOND_TO_METHOD) {
            return false;
        } else {
            throw new UnsupportedOperationException();
        }
    }

    @Specialization
    public Object dispatch(
            VirtualFrame frame,
            Object methodReceiverObject,
            LexicalScope lexicalScope,
            Object receiverObject,
            Object methodName,
            Object blockObject,
            Object argumentsObjects,
            Dispatch.DispatchAction dispatchAction) {
        return dispatch(
                frame,
                methodReceiverObject,
                lexicalScope,
                box.box(receiverObject),
                methodName,
                CompilerDirectives.unsafeCast(blockObject, RubyProc.class, true, false),
                CompilerDirectives.unsafeCast(argumentsObjects, Object[].class, true, true),
                dispatchAction);
    }


}