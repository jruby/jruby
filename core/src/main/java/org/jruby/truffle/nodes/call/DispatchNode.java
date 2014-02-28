/*
 * Copyright (c) 2013, 2014 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.nodes.call;

import com.oracle.truffle.api.*;
import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.api.nodes.*;
import org.jruby.truffle.runtime.*;
import org.jruby.truffle.runtime.control.*;
import org.jruby.truffle.runtime.core.*;
import org.jruby.truffle.runtime.methods.*;

/**
 * Any node in the dispatch chain.
 */
public class DispatchNode extends Node {

    private final RubyContext context;

    public DispatchNode(RubyContext context, SourceSection sourceSection) {
        super(sourceSection);

        assert context != null;
        assert sourceSection != null;

        this.context = context;
    }

    /**
     * Get the depth of this node in the dispatch chain. The first node below
     * {@link DispatchHeadNode} is at depth 1.
     */
    public int getDepth() {
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

    /**
     * The central point for method lookup.
     */
    protected RubyMethod lookup(VirtualFrame frame, RubyBasicObject receiverBasicObject, String name) throws UseMethodMissingException {
        CompilerAsserts.neverPartOfCompilation();

        final RubyBasicObject boxedCallingSelf = context.getCoreLibrary().box(frame.getArguments(RubyArguments.class).getSelf());

        RubyMethod method = receiverBasicObject.getLookupNode().lookupMethod(name);

        // If no method was found, use #method_missing

        if (method == null) {
            throw new UseMethodMissingException();
        }

        // Check for methods that are explicitly undefined

        if (method.isUndefined()) {
            throw new RaiseException(context.getCoreLibrary().nameErrorNoMethod(name, receiverBasicObject.toString()));
        }

        /**
         * If there was still nothing found it's an error. Normally we should at least find BasicObject#method_missing,
         * but it might have been removed or something.
         */

        if (method == null) {
            throw new RaiseException(context.getCoreLibrary().nameErrorNoMethod(name, receiverBasicObject.toString()));
        }

        // Check visibility

        if (boxedCallingSelf == receiverBasicObject.getRubyClass()){
            return method;
        }

        if (!method.isVisibleTo(boxedCallingSelf, receiverBasicObject)) {
            CompilerDirectives.transferToInterpreter();
            throw new RaiseException(context.getCoreLibrary().noMethodError(name, receiverBasicObject.toString()));
        }

        return method;
    }

    public RubyContext getContext() {
        return context;
    }

}
