/*
 * Copyright (c) 2013, 2014 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.nodes;

import com.oracle.truffle.api.*;
import com.oracle.truffle.api.source.*;
import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.api.nodes.*;
import com.oracle.truffle.api.utilities.BranchProfile;
import org.jruby.truffle.nodes.cast.ProcOrNullNode;
import org.jruby.truffle.nodes.cast.ProcOrNullNodeFactory;
import org.jruby.truffle.nodes.cast.BooleanCastNode;
import org.jruby.truffle.nodes.cast.BooleanCastNodeFactory;
import org.jruby.truffle.nodes.dispatch.Dispatch;
import org.jruby.truffle.nodes.dispatch.DispatchHeadNode;
import org.jruby.truffle.runtime.*;
import org.jruby.truffle.runtime.core.*;
import org.jruby.truffle.runtime.core.RubyArray;
import org.jruby.truffle.runtime.methods.*;
import org.jruby.truffle.runtime.util.ArrayUtils;

import java.util.Arrays;

public class RubyCallNode extends RubyNode {

    private final String methodName;

    @Child protected RubyNode receiver;
    @Child protected ProcOrNullNode block;
    @Children protected final RubyNode[] arguments;

    private final boolean isSplatted;

    @Child protected DispatchHeadNode dispatchHead;

    private final BranchProfile splatNotArrayProfile = new BranchProfile();

    @CompilerDirectives.CompilationFinal private boolean seenNullInUnsplat = false;
    @CompilerDirectives.CompilationFinal private boolean seenIntegerFixnumInUnsplat = false;
    @CompilerDirectives.CompilationFinal private boolean seenLongFixnumInUnsplat = false;
    @CompilerDirectives.CompilationFinal private boolean seenObjectInUnsplat = false;

    @Child protected DispatchHeadNode respondToMissing;
    @Child protected BooleanCastNode respondToMissingCast;

    public RubyCallNode(RubyContext context, SourceSection section, String methodName, RubyNode receiver, RubyNode block, boolean isSplatted, RubyNode... arguments) {
        this(context, section, methodName, receiver, block, isSplatted, false, arguments);
    }

    public RubyCallNode(RubyContext context, SourceSection section, String methodName, RubyNode receiver, RubyNode block, boolean isSplatted, boolean rubiniusPrimitive, RubyNode... arguments) {
        super(context, section);

        this.methodName = methodName;

        this.receiver = receiver;

        if (block == null) {
            this.block = null;
        } else {
            this.block = ProcOrNullNodeFactory.create(context, section, block);
        }

        this.arguments = arguments;
        this.isSplatted = isSplatted;

        dispatchHead = new DispatchHeadNode(context, false, rubiniusPrimitive, Dispatch.MissingBehavior.CALL_METHOD_MISSING);
        respondToMissing = new DispatchHeadNode(context, false, Dispatch.MissingBehavior.RETURN_MISSING);
        respondToMissingCast = BooleanCastNodeFactory.create(context, section, null);
    }

    @Override
    public Object execute(VirtualFrame frame) {
        final Object receiverObject = receiver.execute(frame);
        final Object[] argumentsObjects = executeArguments(frame);
        final RubyProc blockObject = executeBlock(frame);

        assert RubyContext.shouldObjectBeVisible(receiverObject);
        assert RubyContext.shouldObjectsBeVisible(argumentsObjects);

        return dispatchHead.call(frame, receiverObject, methodName, blockObject, argumentsObjects);
    }

    private RubyProc executeBlock(VirtualFrame frame) {
        if (block != null) {
            return block.executeRubyProc(frame);
        } else {
            return null;
        }
    }

    @ExplodeLoop
    private Object[] executeArguments(VirtualFrame frame) {
        final Object[] argumentsObjects = new Object[arguments.length];

        for (int i = 0; i < arguments.length; i++) {
            argumentsObjects[i] = arguments[i].execute(frame);
            assert RubyContext.shouldObjectBeVisible(argumentsObjects[i]) : argumentsObjects[i].getClass();
        }

        if (isSplatted) {
            return splat(argumentsObjects[0]);
        } else {
            return argumentsObjects;
        }
    }

    private Object[] splat(Object argument) {
        // TODO(CS): what happens if isn't just one argument, or it isn't an Array?

        if (!(argument instanceof RubyArray)) {
            splatNotArrayProfile.enter();
            notDesignedForCompilation();
            throw new UnsupportedOperationException();
        }

        final RubyArray array = (RubyArray) argument;
        final int size = array.getSize();
        final Object store = array.getStore();

        if (seenNullInUnsplat && store == null) {
            return new Object[]{};
        } else if (seenIntegerFixnumInUnsplat && store instanceof int[]) {
            return ArrayUtils.boxUntil((int[]) store, size);
        } else if (seenLongFixnumInUnsplat && store instanceof long[]) {
            return ArrayUtils.boxUntil((long[]) store, size);
        } else if (seenObjectInUnsplat && store instanceof Object[]) {
            return Arrays.copyOfRange((Object[]) store, 0, size);
        }

        CompilerDirectives.transferToInterpreterAndInvalidate();

        if (store == null) {
            seenNullInUnsplat = true;
            return new Object[]{};
        } else if (store instanceof int[]) {
            seenIntegerFixnumInUnsplat = true;
            return ArrayUtils.boxUntil((int[]) store, size);
        } else if (store instanceof long[]) {
            seenLongFixnumInUnsplat = true;
            return ArrayUtils.boxUntil((long[]) store, size);
        } else if (store instanceof Object[]) {
            seenObjectInUnsplat = true;
            return Arrays.copyOfRange((Object[]) store, 0, size);
        }

        throw new UnsupportedOperationException();
    }

    @Override
    public Object isDefined(VirtualFrame frame) {
        notDesignedForCompilation();

        if (receiver.isDefined(frame) == NilPlaceholder.INSTANCE) {
            return NilPlaceholder.INSTANCE;
        }

        for (RubyNode argument : arguments) {
            if (argument.isDefined(frame) == NilPlaceholder.INSTANCE) {
                return NilPlaceholder.INSTANCE;
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
            return NilPlaceholder.INSTANCE;
        }

        final RubyBasicObject receiverBasicObject = context.getCoreLibrary().box(receiverObject);

        // TODO(CS): this lookup should be cached

        final RubyMethod method = ModuleOperations.lookupMethod(receiverBasicObject.getMetaClass(), methodName);

        final RubyBasicObject self = context.getCoreLibrary().box(RubyArguments.getSelf(frame.getArguments()));

        if (method == null) {
            final Object r = respondToMissing.call(frame, receiverBasicObject, "respond_to_missing?", null, context.makeString(methodName));

            if (r != Dispatch.MISSING && !respondToMissingCast.executeBoolean(frame, r)) {
                return NilPlaceholder.INSTANCE;
            }
        } else if (method.isUndefined()) {
            return NilPlaceholder.INSTANCE;
        } else if (!method.isVisibleTo(this, self)) {
            return NilPlaceholder.INSTANCE;
        }

        return context.makeString("method");
    }

    public String getName() {
        return methodName;
    }

}
