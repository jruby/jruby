/*
 * Copyright (c) 2013, 2016 Oracle and/or its affiliates. All rights reserved. This
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
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.api.source.SourceSection;
import org.jruby.truffle.RubyContext;
import org.jruby.truffle.core.Layouts;
import org.jruby.truffle.language.RubyGuards;
import org.jruby.truffle.language.RubyNode;

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
        arrayBuilderNode = ArrayBuilderNode.create(context);
    }

    @Override
    public DynamicObject execute(VirtualFrame frame) {
        Object store = arrayBuilderNode.start();
        int length = 0;
        if (children.length == 1) {
            return executeSingle(frame, store, length);
        } else {
            return executeRubyArray(frame, store, length);
        }
    }

    @ExplodeLoop
    private DynamicObject executeSingle(VirtualFrame frame, Object store, int length) {
        final Object childObject = children[0].execute(frame);
        if (RubyGuards.isRubyArray(childObject)) {
            appendArrayProfile.enter();
            final DynamicObject childArray = (DynamicObject) childObject;
            store = arrayBuilderNode.ensure(store, length + Layouts.ARRAY.getSize(childArray));
            store = arrayBuilderNode.appendArray(store, length, childArray);
            length += Layouts.ARRAY.getSize(childArray);
        } else {
            appendObjectProfile.enter();
            store = arrayBuilderNode.ensure(store, length + 1);
            store = arrayBuilderNode.appendValue(store, length, childObject);
            length++;
        }
        return Layouts.ARRAY.createArray(coreLibrary().getArrayFactory(), arrayBuilderNode.finish(store, length), length);
    }

    @ExplodeLoop
    private DynamicObject executeRubyArray(VirtualFrame frame, Object store, int length) {
        for (int n = 0; n < children.length; n++) {
            final Object childObject = children[n].execute(frame);

            if (RubyGuards.isRubyArray(childObject)) {
                appendArrayProfile.enter();
                final DynamicObject childArray = (DynamicObject) childObject;
                store = arrayBuilderNode.ensure(store, length + Layouts.ARRAY.getSize(childArray));
                store = arrayBuilderNode.appendArray(store, length, childArray);
                length += Layouts.ARRAY.getSize(childArray);
            } else {
                appendObjectProfile.enter();
                store = arrayBuilderNode.ensure(store, length + 1);
                store = arrayBuilderNode.appendValue(store, length, childObject);
                length++;
            }
        }

        return Layouts.ARRAY.createArray(coreLibrary().getArrayFactory(), arrayBuilderNode.finish(store, length), length);
    }

    @ExplodeLoop
    @Override
    public void executeVoid(VirtualFrame frame) {
        for (int n = 0; n < children.length; n++) {
            children[n].executeVoid(frame);
        }
    }

}
