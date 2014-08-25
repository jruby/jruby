/*
 * Copyright (c) 2013 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.nodes.call;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.NodeUtil;
import org.jruby.truffle.runtime.*;
import org.jruby.truffle.runtime.control.RaiseException;
import org.jruby.truffle.runtime.core.*;
import org.jruby.truffle.runtime.methods.RubyMethod;

/**
 * The head of a chain of dispatch nodes. Can be used with {@link RubyCallNode} or on its own.
 */
public class DispatchHeadNode extends Node {

    private final RubyContext context;
    private final boolean ignoreVisibility;
    private final String name;
    private final boolean isSplatted;

    public static enum MissingBehavior {
        RETURN_MISSING,
        CALL_METHOD_MISSING
    }

    public static enum DispatchAction {
        DISPATCH,
        RESPOND
    }

    public static final Object MISSING = new Object();

    @Child protected NewDispatchNode newDispatch;

    public DispatchHeadNode(RubyContext context, String name, boolean isSplatted, MissingBehavior missingBehavior) {
        this(context, false, name, isSplatted, missingBehavior);
    }

    public DispatchHeadNode(RubyContext context, boolean ignoreVisibility, String name, boolean isSplatted, MissingBehavior missingBehavior) {
        this.context = context;
        this.ignoreVisibility = ignoreVisibility;

        this.name = name;
        this.isSplatted = isSplatted;

        newDispatch = new NewUnresolvedDispatchNode(context, name, ignoreVisibility, missingBehavior);
    }

    public Object dispatch(VirtualFrame frame, Object receiverObject, RubyProc blockObject, Object... argumentsObjects) {
        return dispatch(frame, NilPlaceholder.INSTANCE, RubyArguments.getSelf(frame.getArguments()), receiverObject, blockObject, argumentsObjects);
    }

    public Object dispatch(VirtualFrame frame, Object callingSelf, Object receiverObject, RubyProc blockObject, Object... argumentsObjects) {
        return dispatch(frame, NilPlaceholder.INSTANCE, callingSelf, receiverObject, blockObject, argumentsObjects);
    }

    public Object dispatch(VirtualFrame frame, Object methodReceiverObject, Object callingSelf, Object receiverObject, RubyProc blockObject, Object... argumentsObjects) {
        return newDispatch.executeDispatch(frame, methodReceiverObject, callingSelf, receiverObject, blockObject, argumentsObjects, DispatchAction.DISPATCH);
    }

    public boolean doesRespondTo(VirtualFrame frame, Object receiverObject) {
        return (boolean) newDispatch.executeDispatch(frame, NilPlaceholder.INSTANCE, RubyArguments.getSelf(frame.getArguments()), receiverObject, null, null, DispatchAction.RESPOND);
    }

    /**
     * Replace the entire dispatch chain with a fresh chain. Used when the situation has changed in
     * such a significant way that it's best to start again rather than add new specializations to
     * the chain. Used for example when methods appear to have been monkey-patched.
     */
    public Object respecialize(VirtualFrame frame, String reason, Object receiverObject, RubyProc blockObject, Object... argumentObjects) {
        CompilerAsserts.neverPartOfCompilation();

        final DispatchHeadNode newHead = new DispatchHeadNode(getContext(), getIgnoreVisibility(), name, isSplatted, MissingBehavior.CALL_METHOD_MISSING);
        replace(newHead, reason);
        return newHead.dispatch(frame, receiverObject, blockObject, argumentObjects);
    }

    public boolean respecializeAndDoesRespondTo(VirtualFrame frame, String reason, Object receiverObject) {
        CompilerAsserts.neverPartOfCompilation();

        final DispatchHeadNode newHead = new DispatchHeadNode(getContext(), getIgnoreVisibility(), name, isSplatted, MissingBehavior.CALL_METHOD_MISSING);
        replace(newHead, reason);
        return newHead.doesRespondTo(frame, receiverObject);
    }

    public String getName() {
        return name;
    }
    /**
     * Get the depth of this node in the dispatch chain. The first node below
     * {@link DispatchHeadNode} is at depth 1.
     */
    public int getDepth() {
        // TODO: can we use findParent instead?

        int depth = 1;
        Node parent = this.getParent();

        while (!(parent instanceof DispatchHeadNode)) {
            parent = parent.getParent();
            depth++;
        }

        return depth;
    }

    public Object respecialize(String reason, VirtualFrame frame, Object receiverObject, RubyProc blockObject, Object... argumentsObjects) {
        CompilerAsserts.neverPartOfCompilation();

        final int depth = getDepth();
        final DispatchHeadNode head = (DispatchHeadNode) NodeUtil.getNthParent(this, depth);

        return head.respecialize(frame, reason, receiverObject, blockObject, argumentsObjects);
    }

    public boolean respecializeAndDoesRespondTo(String reason, VirtualFrame frame, Object receiverObject) {
        CompilerAsserts.neverPartOfCompilation();

        final int depth = getDepth();
        final DispatchHeadNode head = (DispatchHeadNode) NodeUtil.getNthParent(this, depth);

        return head.respecializeAndDoesRespondTo(frame, reason, receiverObject);
    }

    protected RubyMethod lookup(RubyBasicObject boxedCallingSelf, RubyBasicObject receiverBasicObject, String name) throws UseMethodMissingException {
        CompilerAsserts.neverPartOfCompilation();

        // TODO(CS): why are we using an exception to convey method missing here?

        RubyMethod method = receiverBasicObject.getLookupNode().lookupMethod(name);

        // If no method was found, use #method_missing

        if (method == null) {
            throw new UseMethodMissingException();
        }

        // Check for methods that are explicitly undefined

        if (method.isUndefined()) {
            throw new RaiseException(context.getCoreLibrary().noMethodError(name, receiverBasicObject.toString(), this));
        }

        // Check visibility

        if (boxedCallingSelf == receiverBasicObject.getRubyClass()){
            return method;
        }

        if (!ignoreVisibility && !method.isVisibleTo(this, boxedCallingSelf, receiverBasicObject)) {
            CompilerDirectives.transferToInterpreter();
            throw new RaiseException(context.getCoreLibrary().privateNoMethodError(name, receiverBasicObject.toString(), this));
        }

        return method;
    }

    public RubyContext getContext() {
        return context;
    }

    public boolean getIgnoreVisibility() { return ignoreVisibility; }

    public NewDispatchNode getNewDispatch() {
        return newDispatch;
    }

}
