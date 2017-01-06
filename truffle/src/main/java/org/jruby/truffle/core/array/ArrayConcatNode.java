/*
 * Copyright (c) 2013, 2017 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.core.array;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.profiles.ConditionProfile;
import org.jruby.truffle.Layouts;
import org.jruby.truffle.language.RubyGuards;
import org.jruby.truffle.language.RubyNode;

/**
 * Concatenate argument arrays (translating a org.jruby.ast.ArgsCatParseNode).
 */
public final class ArrayConcatNode extends RubyNode {

    @Children private final RubyNode[] children;
    // Use an arrayBuilderNode to stabilize the array type for a given location.
    @Child private ArrayBuilderNode arrayBuilderNode = ArrayBuilderNode.create();

    private final ConditionProfile isArrayProfile = ConditionProfile.createBinaryProfile();

    public ArrayConcatNode(RubyNode[] children) {
        assert children.length > 1;
        this.children = children;
    }

    @Override
    public DynamicObject execute(VirtualFrame frame) {
        if (children.length == 1) {
            return executeSingle(frame);
        } else {
            return executeMultiple(frame);
        }
    }

    private DynamicObject executeSingle(VirtualFrame frame) {
        Object store = arrayBuilderNode.start();
        final Object childObject = children[0].execute(frame);

        final int size;
        if (isArrayProfile.profile(RubyGuards.isRubyArray(childObject))) {
            final DynamicObject childArray = (DynamicObject) childObject;
            size = Layouts.ARRAY.getSize(childArray);
            store = arrayBuilderNode.ensure(store, size);
            store = arrayBuilderNode.appendArray(store, 0, childArray);
        } else {
            size = 1;
            store = arrayBuilderNode.ensure(store, 1);
            store = arrayBuilderNode.appendValue(store, 0, childObject);
        }
        return createArray(arrayBuilderNode.finish(store, size), size);
    }

    @ExplodeLoop
    private DynamicObject executeMultiple(VirtualFrame frame) {
        Object store = arrayBuilderNode.start();
        int length = 0;

        for (int n = 0; n < children.length; n++) {
            final Object childObject = children[n].execute(frame);

            if (isArrayProfile.profile(RubyGuards.isRubyArray(childObject))) {
                final DynamicObject childArray = (DynamicObject) childObject;
                final int size = Layouts.ARRAY.getSize(childArray);
                store = arrayBuilderNode.ensure(store, length + size);
                store = arrayBuilderNode.appendArray(store, length, childArray);
                length += size;
            } else {
                store = arrayBuilderNode.ensure(store, length + 1);
                store = arrayBuilderNode.appendValue(store, length, childObject);
                length++;
            }
        }

        return createArray(arrayBuilderNode.finish(store, length), length);
    }

    @ExplodeLoop
    @Override
    public void executeVoid(VirtualFrame frame) {
        for (int n = 0; n < children.length; n++) {
            children[n].executeVoid(frame);
        }
    }

}
