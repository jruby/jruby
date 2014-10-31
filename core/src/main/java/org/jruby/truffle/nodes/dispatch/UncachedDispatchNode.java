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

import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.IndirectCallNode;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.utilities.BranchProfile;
import org.jruby.truffle.nodes.cast.BoxingNode;
import org.jruby.truffle.nodes.conversion.ToJavaStringNode;
import org.jruby.truffle.nodes.conversion.ToJavaStringNodeFactory;
import org.jruby.truffle.nodes.conversion.ToSymbolNode;
import org.jruby.truffle.nodes.conversion.ToSymbolNodeFactory;
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
    @Child protected ToSymbolNode toSymbolNode;
    @Child protected ToJavaStringNode toJavaStringNode;

    private final BranchProfile constantMissingProfile = BranchProfile.create();
    private final BranchProfile methodMissingProfile = BranchProfile.create();

    public UncachedDispatchNode(RubyContext context, boolean ignoreVisibility) {
        super(context);
        this.ignoreVisibility = ignoreVisibility;
        callNode = Truffle.getRuntime().createIndirectCallNode();
        box = new BoxingNode(context, null, null);
        toSymbolNode = ToSymbolNodeFactory.create(context, null, null);
        toJavaStringNode = ToJavaStringNodeFactory.create(context, null, null);
    }

    public UncachedDispatchNode(UncachedDispatchNode prev) {
        super(prev);
        ignoreVisibility = prev.ignoreVisibility;
        callNode = prev.callNode;
        box = prev.box;
        toSymbolNode = prev.toSymbolNode;
        toJavaStringNode = prev.toJavaStringNode;
    }

    @Specialization(guards = "actionIsReadConstant")
    public Object dispatchReadConstant(
            VirtualFrame frame,
            Object methodReceiverObject,
            LexicalScope lexicalScope,
            RubyModule receiverObject,
            Object constantName,
            Object blockObject,
            Object argumentsObjects,
            Dispatch.DispatchAction dispatchAction) {
        final RubyConstant constant = lookupConstant(lexicalScope, receiverObject,
                toJavaStringNode.executeJavaString(frame, constantName), ignoreVisibility, dispatchAction);

        if (constant != null) {
            return constant.getValue();
        }

        constantMissingProfile.enter();

        final RubyClass callerClass = ignoreVisibility ? null : box.box(RubyArguments.getSelf(frame.getArguments())).getMetaClass();

        final RubyMethod missingMethod = lookup(callerClass, receiverObject, "const_missing", ignoreVisibility,
                dispatchAction);

        if (missingMethod == null) {
            CompilerDirectives.transferToInterpreter();
            throw new RaiseException(getContext().getCoreLibrary().runtimeError(
                    receiverObject.toString() + " didn't have a #const_missing", this));
        }

        return callNode.call(
                frame,
                missingMethod.getCallTarget(),
                RubyArguments.pack(
                        missingMethod,
                        missingMethod.getDeclarationFrame(),
                        receiverObject,
                        null,
                        new Object[]{toSymbolNode.executeRubySymbol(frame, constantName)}));
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
        final RubyClass callerClass = ignoreVisibility ? null : box.box(RubyArguments.getSelf(frame.getArguments())).getMetaClass();

        final RubyMethod method = lookup(callerClass, receiverObject, toJavaStringNode.executeJavaString(frame, methodName),
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

        methodMissingProfile.enter();

        final RubyMethod missingMethod = lookup(callerClass, receiverObject, "method_missing", true,
                dispatchAction);

        if (missingMethod == null) {
            if (dispatchAction == Dispatch.DispatchAction.RESPOND_TO_METHOD) {
                return false;
            } else {
                CompilerDirectives.transferToInterpreter();
                throw new RaiseException(getContext().getCoreLibrary().runtimeError(
                        receiverObject.toString() + " didn't have a #method_missing", this));
            }
        }

        if (dispatchAction == Dispatch.DispatchAction.CALL_METHOD) {
            final Object[] argumentsObjectsArray = CompilerDirectives.unsafeCast(argumentsObjects, Object[].class, true);

            final Object[] modifiedArgumentsObjects = new Object[1 + argumentsObjectsArray.length];

            modifiedArgumentsObjects[0] = toSymbolNode.executeRubySymbol(frame, methodName);

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