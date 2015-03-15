/*
 * Copyright (c) 2013, 2015 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.nodes;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.source.SourceSection;
import org.jruby.truffle.nodes.cast.BooleanCastNode;
import org.jruby.truffle.nodes.cast.BooleanCastNodeFactory;
import org.jruby.truffle.nodes.cast.ProcOrNullNode;
import org.jruby.truffle.nodes.cast.ProcOrNullNodeFactory;
import org.jruby.truffle.nodes.dispatch.*;
import org.jruby.truffle.runtime.ModuleOperations;
import org.jruby.truffle.runtime.RubyArguments;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.methods.InternalMethod;

public class RubyCallNode extends RubyNode {

    private final String methodName;

    @Child private RubyNode receiver;

    private final boolean isVCall;

    @Child private CallDispatchHeadNode dispatchHead;

    @Child private CallDispatchHeadNode respondToMissing;
    @Child private BooleanCastNode respondToMissingCast;

    private final boolean ignoreVisibility;

    public RubyCallNode(RubyContext context, SourceSection section, String methodName, RubyNode receiver, RubyNode block, boolean isSplatted, RubyNode... arguments) {
        this(context, section, methodName, receiver, block, isSplatted, false, false, arguments);
    }

    public RubyCallNode(RubyContext context, SourceSection section, String methodName, RubyNode receiver, RubyNode block, boolean isSplatted, boolean ignoreVisibility, boolean rubiniusPrimitive, RubyNode... arguments) {
        this(context, section, methodName, receiver, block, isSplatted, false, ignoreVisibility, rubiniusPrimitive, arguments);
    }

    public RubyCallNode(RubyContext context, SourceSection section, String methodName, RubyNode receiver, RubyNode block, boolean isSplatted, boolean isVCall, boolean ignoreVisibility, boolean rubiniusPrimitive, RubyNode... arguments) {
        super(context, section);

        this.methodName = methodName;
        this.receiver = receiver;

        ProcOrNullNode blockNode = null;
        if (block != null) {
            blockNode = ProcOrNullNodeFactory.create(context, section, block);
        }

        this.isVCall = isVCall;

        dispatchHead = DispatchHeadNodeFactory.createMethodCall(context, ignoreVisibility, false, MissingBehavior.CALL_METHOD_MISSING, arguments, blockNode, isSplatted);
        respondToMissing = DispatchHeadNodeFactory.createMethodCall(context, true, MissingBehavior.RETURN_MISSING);
        respondToMissingCast = BooleanCastNodeFactory.create(context, section, null);

        this.ignoreVisibility = ignoreVisibility;
    }

    @Override
    public Object execute(VirtualFrame frame) {
        final Object receiverObject = receiver.execute(frame);
        return dispatchHead.call(frame, receiverObject, methodName, null, (Object[]) null);
    }
    
    @Override
    public Object isDefined(VirtualFrame frame) {
        notDesignedForCompilation();

        if (receiver.isDefined(frame) == nil()) {
            return nil();
        }

        for (RubyNode argument : dispatchHead.getArgumentNodes()) {
            if (argument.isDefined(frame) == nil()) {
                return nil();
            }
        }

        final RubyContext context = getContext();

        Object receiverObject;

        try {
            /*
             * TODO(CS): Getting a node via an accessor like this doesn't work with Truffle at the
             * moment and will cause frame escape errors, so we don't use it in compilation mode.
             */

            CompilerAsserts.neverPartOfCompilation();

            receiverObject = receiver.execute(frame);
        } catch (Exception e) {
            return nil();
        }

        // TODO(CS): this lookup should be cached

        final InternalMethod method = ModuleOperations.lookupMethod(context.getCoreLibrary().getMetaClass(receiverObject), methodName);

        final Object self = RubyArguments.getSelf(frame.getArguments());

        if (method == null) {
            final Object r = respondToMissing.call(frame, receiverObject, "respond_to_missing?", null, context.makeString(methodName));

            if (r != DispatchNode.MISSING && !respondToMissingCast.executeBoolean(frame, r)) {
                return nil();
            }
        } else if (method.isUndefined()) {
            return nil();
        } else if (!ignoreVisibility && !method.isVisibleTo(this, context.getCoreLibrary().getMetaClass(self))) {
            return nil();
        }

        return context.makeString("method");
    }

    public String getName() {
        return methodName;
    }

    public boolean isVCall() {
        return isVCall;
    }

}
