/*
 * Copyright (c) 2013, 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.language.dispatch;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.source.SourceSection;
import org.jcodings.specific.UTF8Encoding;
import org.jruby.truffle.Layouts;
import org.jruby.truffle.RubyContext;
import org.jruby.truffle.core.array.ArrayUtils;
import org.jruby.truffle.core.cast.BooleanCastNode;
import org.jruby.truffle.core.cast.BooleanCastNodeGen;
import org.jruby.truffle.core.cast.ProcOrNullNode;
import org.jruby.truffle.core.cast.ProcOrNullNodeGen;
import org.jruby.truffle.core.module.ModuleOperations;
import org.jruby.truffle.language.RubyGuards;
import org.jruby.truffle.language.RubyNode;
import org.jruby.truffle.language.arguments.RubyArguments;
import org.jruby.truffle.language.methods.InternalMethod;

public class RubyCallNode extends RubyNode {

    private final String methodName;

    @Child private RubyNode receiver;
    @Child private ProcOrNullNode block;
    @Children private final RubyNode[] arguments;

    private final boolean isSplatted;
    private final boolean isVCall;

    @Child private CallDispatchHeadNode dispatchHead;

    @CompilationFinal private boolean seenNullInUnsplat = false;
    @CompilationFinal private boolean seenIntegerFixnumInUnsplat = false;
    @CompilationFinal private boolean seenLongFixnumInUnsplat = false;
    @CompilationFinal private boolean seenFloatInUnsplat = false;
    @CompilationFinal private boolean seenObjectInUnsplat = false;

    @Child private CallDispatchHeadNode respondToMissing;
    @Child private BooleanCastNode respondToMissingCast;

    private final boolean ignoreVisibility;

    public RubyCallNode(RubyContext context, SourceSection section, String methodName, RubyNode receiver, RubyNode block, boolean isSplatted, RubyNode... arguments) {
        this(context, section, methodName, receiver, block, isSplatted, false, arguments);
    }

    public RubyCallNode(RubyContext context, SourceSection section, String methodName, RubyNode receiver, RubyNode block, boolean isSplatted, boolean ignoreVisibility, RubyNode... arguments) {
        this(context, section, methodName, receiver, block, isSplatted, ignoreVisibility, false, arguments);
    }

    public RubyCallNode(RubyContext context, SourceSection section, String methodName, RubyNode receiver, RubyNode block, boolean isSplatted, boolean ignoreVisibility, boolean isVCall, RubyNode... arguments) {
        super(context, section);

        this.methodName = methodName;
        this.receiver = receiver;
        this.arguments = arguments;
        if (block == null) {
            this.block = null;
        } else {
            this.block = ProcOrNullNodeGen.create(context, section, block);
        }

        this.isSplatted = isSplatted;
        this.isVCall = isVCall;
        this.ignoreVisibility = ignoreVisibility;

        this.dispatchHead = DispatchHeadNodeFactory.createMethodCall(context, ignoreVisibility);
    }

    @Override
    public Object execute(VirtualFrame frame) {
        final Object receiverObject = receiver.execute(frame);
        final Object[] argumentsObjects = executeArguments(frame);
        final DynamicObject blockObject = executeBlock(frame);

        return dispatchHead.call(frame, receiverObject, methodName, blockObject, argumentsObjects);
    }

    private DynamicObject executeBlock(VirtualFrame frame) {
        if (block != null) {
            return (DynamicObject) block.execute(frame);
        } else {
            return null;
        }
    }

    @ExplodeLoop
    private Object[] executeArguments(VirtualFrame frame) {
        final Object[] argumentsObjects = new Object[arguments.length];

        for (int i = 0; i < arguments.length; i++) {
            argumentsObjects[i] = arguments[i].execute(frame);
        }

        if (isSplatted) {
            return splat(argumentsObjects[0]);
        } else {
            return argumentsObjects;
        }
    }

    private Object[] splat(Object argument) {
        // TODO CS 19-May-15 this is a terrible mess and needs to go

        // TODO(CS): what happens if isn't just one argument, or it isn't an Array?

        if (!RubyGuards.isRubyArray(argument)) {
            CompilerDirectives.transferToInterpreter();
            throw new UnsupportedOperationException(argument.getClass().toString());
        }

        final DynamicObject array = (DynamicObject) argument;
        final int size = Layouts.ARRAY.getSize(array);
        final Object store = Layouts.ARRAY.getStore(array);

        if (seenNullInUnsplat && store == null) {
            return new Object[]{};
        } else if (seenIntegerFixnumInUnsplat && store instanceof int[]) {
            return ArrayUtils.boxUntil((int[]) store, size);
        } else if (seenLongFixnumInUnsplat && store instanceof long[]) {
            return ArrayUtils.boxUntil((long[]) store, size);
        } else if (seenFloatInUnsplat && store instanceof double[]) {
            return ArrayUtils.boxUntil((double[]) store, size);
        } else if (seenObjectInUnsplat && store != null && store.getClass() == Object[].class) {
            return ArrayUtils.extractRange((Object[]) store, 0, size);
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
        } else if (store instanceof double[]) {
            seenFloatInUnsplat = true;
            return ArrayUtils.boxUntil((double[]) store, size);
        } else if (store.getClass() == Object[].class) {
            seenObjectInUnsplat = true;
            return ArrayUtils.extractRange((Object[]) store, 0, size);
        }

        throw new UnsupportedOperationException();
    }

    @Override
    public Object isDefined(VirtualFrame frame) {
        CompilerDirectives.transferToInterpreter();

        if (receiver.isDefined(frame) == nil()) {
            return nil();
        }

        for (RubyNode argument : arguments) {
            if (argument.isDefined(frame) == nil()) {
                return nil();
            }
        }

        final RubyContext context = getContext();

        final Object receiverObject;
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

        final Object self = RubyArguments.getSelf(frame);

        if (method == null) {
            final Object r = respondToMissing(frame, receiverObject);
            if (r != DispatchNode.MISSING && !castRespondToMissingToBoolean(frame, r)) {
                return nil();
            }
        } else if (method.isUndefined()) {
            return nil();
        } else if (!ignoreVisibility && !method.isVisibleTo(context.getCoreLibrary().getMetaClass(self))) {
            return nil();
        }

        return create7BitString("method", UTF8Encoding.INSTANCE);
    }

    private Object respondToMissing(VirtualFrame frame, Object receiverObject) {
        if (respondToMissing == null) {
            CompilerDirectives.transferToInterpreter();
            respondToMissing = insert(DispatchHeadNodeFactory.createMethodCall(getContext(), true, MissingBehavior.RETURN_MISSING));
        }
        final DynamicObject method = getContext().getSymbolTable().getSymbol(methodName);
        return respondToMissing.call(frame, receiverObject, "respond_to_missing?", null, method, false);
    }

    private boolean castRespondToMissingToBoolean(VirtualFrame frame, final Object r) {
        if (respondToMissingCast == null) {
            CompilerDirectives.transferToInterpreter();
            respondToMissingCast = insert(BooleanCastNodeGen.create(getContext(), getSourceSection(), null));
        }
        return respondToMissingCast.executeBoolean(frame, r);
    }

    public String getName() {
        return methodName;
    }

    public boolean isVCall() {
        return isVCall;
    }

}
