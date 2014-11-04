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
import org.jruby.truffle.runtime.LexicalScope;
import org.jruby.truffle.runtime.core.*;
import org.jruby.truffle.runtime.RubyArguments;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.core.RubyBasicObject;
import org.jruby.truffle.runtime.core.RubyClass;
import org.jruby.truffle.runtime.core.RubyProc;
import org.jruby.truffle.runtime.methods.RubyMethod;
import org.jruby.util.cli.Options;

public abstract class CachedBoxedMethodMissingDispatchNode extends CachedDispatchNode {

    private final RubyClass expectedClass;
    private final Assumption unmodifiedAssumption;
    private final RubyMethod method;

    @Child protected DirectCallNode callNode;
    @Child protected IndirectCallNode indirectCallNode;

    public CachedBoxedMethodMissingDispatchNode(RubyContext context, Object cachedName, DispatchNode next,
                                                RubyClass expectedClass, RubyMethod method,
                                                boolean indirect) {
        super(context, cachedName, next, indirect);

        this.expectedClass = expectedClass;
        unmodifiedAssumption = expectedClass.getUnmodifiedAssumption();
        this.method = method;

        if (indirect) {
            indirectCallNode = Truffle.getRuntime().createIndirectCallNode();
        } else {
            callNode = Truffle.getRuntime().createDirectCallNode(method.getCallTarget());

            /*
             * The way that #method_missing is used is usually as an indirection to call some other method, and
             * possibly to modify the arguments. In both cases, but especially the latter, it makes a lot of sense
             * to manually clone the call target and to inline it.
             */

            if (callNode.isSplittable() && (Options.TRUFFLE_DISPATCH_METHODMISSING_ALWAYS_CLONED.load() || method.getSharedMethodInfo().shouldAlwaysSplit())) {
                insert(callNode);
                callNode.split();
            }

            if (callNode.isInlinable() && Options.TRUFFLE_DISPATCH_METHODMISSING_ALWAYS_INLINED.load()) {
                insert(callNode);
                callNode.forceInlining();
            }
        }
    }

    public CachedBoxedMethodMissingDispatchNode(CachedBoxedMethodMissingDispatchNode prev) {
        super(prev);
        expectedClass = prev.expectedClass;
        unmodifiedAssumption = prev.unmodifiedAssumption;
        method = prev.method;
        callNode = prev.callNode;
        indirectCallNode = prev.indirectCallNode;
    }

    @Specialization(guards = "guardName")
    public Object dispatch(
            VirtualFrame frame,
            RubyNilClass methodReceiverObject,
            LexicalScope lexicalScope,
            RubyBasicObject receiverObject,
            Object methodName,
            Object blockObject,
            Object argumentsObjects,
            Dispatch.DispatchAction dispatchAction) {
        CompilerAsserts.compilationConstant(dispatchAction);

        // Check the lookup node is what we expect

        if (receiverObject.getMetaClass() != expectedClass) {
            return next.executeDispatch(
                    frame,
                    methodReceiverObject,
                    lexicalScope,
                    receiverObject,
                    methodName,
                    CompilerDirectives.unsafeCast(blockObject, RubyProc.class, true, false),
                    argumentsObjects,
                    dispatchAction);
        }

        // Check the class has not been modified

        try {
            unmodifiedAssumption.check();
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
            // When calling #method_missing we need to prepend the symbol

            final Object[] argumentsObjectsArray = CompilerDirectives.unsafeCast(argumentsObjects, Object[].class, true);
            final Object[] modifiedArgumentsObjects = new Object[1 + argumentsObjectsArray.length];
            modifiedArgumentsObjects[0] = getCachedNameAsSymbol();
            System.arraycopy(argumentsObjects, 0, modifiedArgumentsObjects, 1, argumentsObjectsArray.length);

            if (isIndirect()) {
                return indirectCallNode.call(
                        frame,
                        method.getCallTarget(),
                        RubyArguments.pack(
                                method,
                                method.getDeclarationFrame(),
                                receiverObject,
                                CompilerDirectives.unsafeCast(blockObject, RubyProc.class, true, false),
                                modifiedArgumentsObjects));
            } else {
                return callNode.call(
                        frame,
                        RubyArguments.pack(
                                method,
                                method.getDeclarationFrame(),
                                receiverObject,
                                CompilerDirectives.unsafeCast(blockObject, RubyProc.class, true, false),
                                modifiedArgumentsObjects));
            }
        } else if (dispatchAction == Dispatch.DispatchAction.RESPOND_TO_METHOD) {
            return false;
        } else if (dispatchAction == Dispatch.DispatchAction.READ_CONSTANT) {
            if (isIndirect()) {
                return indirectCallNode.call(
                        frame,
                        method.getCallTarget(),
                        RubyArguments.pack(
                                method,
                                method.getDeclarationFrame(),
                                receiverObject,
                                CompilerDirectives.unsafeCast(blockObject, RubyProc.class, true, false),
                                new Object[]{getCachedNameAsSymbol()}));
            } else {
                return callNode.call(
                        frame,
                        RubyArguments.pack(
                                method,
                                method.getDeclarationFrame(),
                                receiverObject,
                                CompilerDirectives.unsafeCast(blockObject, RubyProc.class, true, false),
                                new Object[]{getCachedNameAsSymbol()}));
            }
        } else {
            throw new UnsupportedOperationException();
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
                CompilerDirectives.unsafeCast(blockObject, RubyProc.class, true, false),
                argumentsObjects, dispatchAction);
    }


}