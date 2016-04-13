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
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.IndirectCallNode;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.profiles.BranchProfile;
import org.jruby.truffle.RubyContext;
import org.jruby.truffle.core.array.ArrayUtils;
import org.jruby.truffle.core.cast.ToSymbolNode;
import org.jruby.truffle.core.cast.ToSymbolNodeGen;
import org.jruby.truffle.interop.ToJavaStringNode;
import org.jruby.truffle.interop.ToJavaStringNodeGen;
import org.jruby.truffle.language.arguments.RubyArguments;
import org.jruby.truffle.language.control.RaiseException;
import org.jruby.truffle.language.methods.DeclarationContext;
import org.jruby.truffle.language.methods.InternalMethod;
import org.jruby.truffle.language.objects.MetaClassNode;
import org.jruby.truffle.language.objects.MetaClassNodeGen;

public class UncachedDispatchNode extends DispatchNode {

    private final boolean ignoreVisibility;
    private final MissingBehavior missingBehavior;

    @Child private IndirectCallNode indirectCallNode;
    @Child private ToSymbolNode toSymbolNode;
    @Child private ToJavaStringNode toJavaStringNode;
    @Child private MetaClassNode metaClassNode;

    private final BranchProfile methodMissingProfile = BranchProfile.create();

    public UncachedDispatchNode(RubyContext context, boolean ignoreVisibility, DispatchAction dispatchAction, MissingBehavior missingBehavior) {
        super(context, dispatchAction);

        this.ignoreVisibility = ignoreVisibility;
        this.missingBehavior = missingBehavior;
        this.indirectCallNode = Truffle.getRuntime().createIndirectCallNode();
        this.toSymbolNode = ToSymbolNodeGen.create(context, null, null);
        this.toJavaStringNode = ToJavaStringNodeGen.create(context, null, null);
        this.metaClassNode = MetaClassNodeGen.create(context, null, null);
    }

    @Override
    protected boolean guard(Object methodName, Object receiver) {
        return true;
    }

    @Override
    public Object executeDispatch(
            VirtualFrame frame,
            Object receiverObject,
            Object name,
            DynamicObject blockObject,
            Object[] argumentsObjects) {
        final DispatchAction dispatchAction = getDispatchAction();

        final DynamicObject callerClass = ignoreVisibility ? null : metaClassNode.executeMetaClass(RubyArguments.getSelf(frame));

        final InternalMethod method = lookup(callerClass, receiverObject, toJavaStringNode.executeToJavaString(frame, name), ignoreVisibility);

        if (method != null) {
            if (dispatchAction == DispatchAction.CALL_METHOD) {
                return call(frame, method, receiverObject, blockObject, argumentsObjects);
            } else if (dispatchAction == DispatchAction.RESPOND_TO_METHOD) {
                return true;
            } else {
                throw new UnsupportedOperationException();
            }
        }

        if (dispatchAction == DispatchAction.CALL_METHOD && missingBehavior == MissingBehavior.RETURN_MISSING) {
            return DispatchNode.MISSING;
        }

        methodMissingProfile.enter();

        final InternalMethod missingMethod = lookup(callerClass, receiverObject, "method_missing", true);

        if (missingMethod == null) {
            if (dispatchAction == DispatchAction.RESPOND_TO_METHOD) {
                return false;
            } else {
                CompilerDirectives.transferToInterpreter();
                throw new RaiseException(coreLibrary().runtimeError(
                        receiverObject.toString() + " didn't have a #method_missing", this));
            }
        }

        if (dispatchAction == DispatchAction.CALL_METHOD) {
            final DynamicObject nameSymbol = toSymbolNode.executeRubySymbol(frame, name);
            final Object[] modifiedArgumentsObjects = ArrayUtils.unshift(argumentsObjects, nameSymbol);

            return call(frame, missingMethod, receiverObject, blockObject, modifiedArgumentsObjects);
        } else if (dispatchAction == DispatchAction.RESPOND_TO_METHOD) {
            return false;
        } else {
            throw new UnsupportedOperationException();
        }
    }

    private Object call(VirtualFrame frame, InternalMethod method, Object receiverObject, DynamicObject blockObject, Object[] argumentsObjects) {
        return indirectCallNode.call(
                frame,
                method.getCallTarget(),
                RubyArguments.pack(null, null, method, DeclarationContext.METHOD, null, receiverObject, blockObject, argumentsObjects));
    }

}