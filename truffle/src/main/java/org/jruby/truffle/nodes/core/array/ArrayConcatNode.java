/*
 * Copyright (c) 2013, 2015 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.nodes.core.array;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.source.SourceSection;
import com.oracle.truffle.api.utilities.BranchProfile;
import org.jruby.truffle.nodes.RubyGuards;
import org.jruby.truffle.nodes.RubyNode;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.core.RubyBasicObject;

/**
 * Concatenate arrays.
 */
public final class ArrayConcatNode extends RubyNode {

    @Children private final RubyNode[] children;
    @Child private ArrayBuilderNode arrayBuilderNode;

    private final BranchProfile appendArrayProfile = BranchProfile.create();
    private final BranchProfile appendObjectProfile = BranchProfile.create();

    public ArrayConcatNode(RubyContext context, SourceSection sourceSection, RubyNode[] children) {
        super(context, sourceSection);
        assert children.length > 1;
        this.children = children;
        arrayBuilderNode = new ArrayBuilderNode.UninitializedArrayBuilderNode(context);
    }

    @Override
    public RubyBasicObject execute(VirtualFrame frame) {
        Object store = arrayBuilderNode.start();
        int length = 0;
        if (children.length == 1) {
            return executeSingle(frame, store, length);
        } else {
            return executeRubyArray(frame, store, length);
        }
    }

    @ExplodeLoop
    private RubyBasicObject executeSingle(VirtualFrame frame, Object store, int length) {
        final Object childObject = children[0].execute(frame);
        if (RubyGuards.isRubyArray(childObject)) {
            appendArrayProfile.enter();
            final RubyBasicObject childArray = (RubyBasicObject) childObject;
            store = arrayBuilderNode.ensure(store, length + ArrayNodes.getSize(childArray));
            store = arrayBuilderNode.appendArray(store, length, childArray);
            length += ArrayNodes.getSize(childArray);
        } else {
            appendObjectProfile.enter();
            store = arrayBuilderNode.ensure(store, length + 1);
            store = arrayBuilderNode.appendValue(store, length, childObject);
            length++;
        }
        return ArrayNodes.createGeneralArray(getContext().getCoreLibrary().getArrayClass(), arrayBuilderNode.finish(store, length), length);
    }

    @ExplodeLoop
    private RubyBasicObject executeRubyArray(VirtualFrame frame, Object store, int length) {
        for (int n = 0; n < children.length; n++) {
            final Object childObject = children[n].execute(frame);

            if (RubyGuards.isRubyArray(childObject)) {
                appendArrayProfile.enter();
                final RubyBasicObject childArray = (RubyBasicObject) childObject;
                store = arrayBuilderNode.ensure(store, length + ArrayNodes.getSize(childArray));
                store = arrayBuilderNode.appendArray(store, length, childArray);
                length += ArrayNodes.getSize(childArray);
            } else {
                appendObjectProfile.enter();
                store = arrayBuilderNode.ensure(store, length + 1);
                store = arrayBuilderNode.appendValue(store, length, childObject);
                length++;
            }
        }

        return ArrayNodes.createGeneralArray(getContext().getCoreLibrary().getArrayClass(), arrayBuilderNode.finish(store, length), length);
    }

    @ExplodeLoop
    @Override
    public void executeVoid(VirtualFrame frame) {
        for (int n = 0; n < children.length; n++) {
            children[n].executeVoid(frame);
        }
    }

}
