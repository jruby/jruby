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

import com.oracle.truffle.api.Assumption;
import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.DirectCallNode;
import com.oracle.truffle.api.nodes.IndirectCallNode;
import com.oracle.truffle.api.nodes.InvalidAssumptionException;
import com.oracle.truffle.api.utilities.BranchProfile;
import org.jruby.truffle.runtime.LexicalScope;
import org.jruby.truffle.runtime.core.*;
import org.jruby.truffle.runtime.RubyArguments;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.core.RubyProc;
import org.jruby.truffle.runtime.methods.RubyMethod;

public abstract class CachedBooleanDispatchNode extends CachedDispatchNode {

    private final Assumption falseUnmodifiedAssumption;
    private final RubyMethod falseMethod;
    private final BranchProfile falseProfile = new BranchProfile();

    private final Object falseValue;
    @Child protected DirectCallNode falseCallDirect;

    private final Assumption trueUnmodifiedAssumption;
    private final RubyMethod trueMethod;
    private final BranchProfile trueProfile = new BranchProfile();

    private final Object trueValue;
    @Child protected DirectCallNode trueCallDirect;

    @Child protected IndirectCallNode indirectCallNode;

    public CachedBooleanDispatchNode(
            RubyContext context, Object cachedName, DispatchNode next,
            Assumption falseUnmodifiedAssumption, Object falseValue, RubyMethod falseMethod,
            Assumption trueUnmodifiedAssumption, Object trueValue, RubyMethod trueMethod,
            boolean indirect) {
        super(context, cachedName, next, indirect);

        this.falseUnmodifiedAssumption = falseUnmodifiedAssumption;
        this.falseMethod = falseMethod;
        this.falseValue = falseValue;

        if (falseMethod != null) {
            if (!indirect) {
                falseCallDirect = Truffle.getRuntime().createDirectCallNode(falseMethod.getCallTarget());

                if (falseCallDirect.isSplittable() && falseMethod.getSharedMethodInfo().shouldAlwaysSplit()) {
                    insert(falseCallDirect);
                    falseCallDirect.split();
                }
            }
        }

        this.trueUnmodifiedAssumption = trueUnmodifiedAssumption;
        this.trueMethod = trueMethod;
        this.trueValue = trueValue;

        if (trueMethod != null) {
            if (!indirect) {
                trueCallDirect = Truffle.getRuntime().createDirectCallNode(trueMethod.getCallTarget());

                if (trueCallDirect.isSplittable() && trueMethod.getSharedMethodInfo().shouldAlwaysSplit()) {
                    insert(trueCallDirect);
                    trueCallDirect.split();
                }
            }
        }

        if (indirect) {
            indirectCallNode = Truffle.getRuntime().createIndirectCallNode();
        }
    }

    public CachedBooleanDispatchNode(CachedBooleanDispatchNode prev) {
        super(prev);
        falseUnmodifiedAssumption = prev.falseUnmodifiedAssumption;
        falseMethod = prev.falseMethod;
        falseValue = prev.falseValue;
        falseCallDirect = prev.falseCallDirect;
        trueUnmodifiedAssumption = prev.trueUnmodifiedAssumption;
        trueValue = prev.trueValue;
        trueMethod = prev.trueMethod;
        trueCallDirect = prev.trueCallDirect;
        indirectCallNode = prev.indirectCallNode;
    }

    @Specialization(guards = "guardName")
    public Object dispatch(
            VirtualFrame frame,
            RubyNilClass methodReceiverObject,
            LexicalScope lexicalScope,
            boolean receiverObject,
            Object methodName,
            Object blockObject,
            Object argumentsObjects,
            Dispatch.DispatchAction dispatchAction) {
        CompilerAsserts.compilationConstant(dispatchAction);

        if (receiverObject) {
            trueProfile.enter();

            try {
                trueUnmodifiedAssumption.check();
            } catch (InvalidAssumptionException e) {
                return resetAndDispatch(
                        frame,
                        methodReceiverObject,
                        lexicalScope,
                        receiverObject,
                        methodName,
                        CompilerDirectives.unsafeCast(blockObject, RubyProc.class, true, false),
                        argumentsObjects,
                        dispatchAction,
                        "class modified");
            }

            if (dispatchAction == Dispatch.DispatchAction.CALL_METHOD) {
                if (isIndirect()) {
                    return indirectCallNode.call(
                            frame,
                            trueMethod.getCallTarget(),
                            RubyArguments.pack(
                                    trueMethod,
                                    trueMethod.getDeclarationFrame(),
                                    receiverObject,
                                    CompilerDirectives.unsafeCast(blockObject, RubyProc.class, true, false),
                                    CompilerDirectives.unsafeCast(argumentsObjects, Object[].class, true)));
                } else {
                    return trueCallDirect.call(
                            frame,
                            RubyArguments.pack(
                                    trueMethod,
                                    trueMethod.getDeclarationFrame(),
                                    receiverObject,
                                    CompilerDirectives.unsafeCast(blockObject, RubyProc.class, true, false),
                                    CompilerDirectives.unsafeCast(argumentsObjects, Object[].class, true)));
                }
            } else if (dispatchAction == Dispatch.DispatchAction.RESPOND_TO_METHOD) {
                return true;
            } else if (dispatchAction == Dispatch.DispatchAction.READ_CONSTANT) {
                return trueValue;
            } else {
                throw new UnsupportedOperationException();
            }
        } else {
            falseProfile.enter();

            try {
                falseUnmodifiedAssumption.check();
            } catch (InvalidAssumptionException e) {
                return resetAndDispatch(
                        frame,
                        methodReceiverObject,
                        lexicalScope,
                        receiverObject,
                        methodName,
                        CompilerDirectives.unsafeCast(blockObject, RubyProc.class, true, false),
                        argumentsObjects,
                        dispatchAction,
                        "class modified");
            }

            if (dispatchAction == Dispatch.DispatchAction.CALL_METHOD) {
                if (isIndirect()) {
                    return indirectCallNode.call(
                            frame,
                            falseMethod.getCallTarget(),
                            RubyArguments.pack(
                                    falseMethod,
                                    falseMethod.getDeclarationFrame(),
                                    receiverObject,
                                    CompilerDirectives.unsafeCast(blockObject, RubyProc.class, true, false),
                                    CompilerDirectives.unsafeCast(argumentsObjects, Object[].class, true)));
                } else {
                    return falseCallDirect.call(
                            frame,
                            RubyArguments.pack(
                                    falseMethod,
                                    falseMethod.getDeclarationFrame(),
                                    receiverObject,
                                    CompilerDirectives.unsafeCast(blockObject, RubyProc.class, true, false),
                                    CompilerDirectives.unsafeCast(argumentsObjects, Object[].class, true)));
                }
            } else if (dispatchAction == Dispatch.DispatchAction.RESPOND_TO_METHOD) {
                return true;
            } else if (dispatchAction == Dispatch.DispatchAction.READ_CONSTANT) {
                return falseValue;
            } else {
                throw new UnsupportedOperationException();
            }
        }
    }

    @Fallback
    public Object dispatch(
            VirtualFrame frame,
            Object methodReceiverObject,
            LexicalScope lexicalScope,
            Object receiverObject,
            Object methodName,
            Object blockObject,
            Object argumentsObjects,
            Dispatch.DispatchAction dispatchAction) {
        return next.executeDispatch(
                frame,
                methodReceiverObject,
                lexicalScope,
                receiverObject,
                methodName,
                blockObject,
                argumentsObjects,
                dispatchAction);
    }

}