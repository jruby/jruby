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

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.source.SourceSection;
import org.jruby.truffle.nodes.RubyNode;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.core.RubyArray;

public class ArrayPushNode extends RubyNode {

    @Child private RubyNode array;
    @Child private RubyNode pushed;

    public ArrayPushNode(RubyContext context, SourceSection sourceSection, RubyNode array, RubyNode pushed) {
        super(context, sourceSection);
        this.array = array;
        this.pushed = pushed;
    }

    @Override
    public Object execute(VirtualFrame frame) {
        CompilerDirectives.transferToInterpreter();

        final Object arrayObject = array.execute(frame);
        assert arrayObject instanceof RubyArray : getSourceSection();

        final RubyArray originalArray = (RubyArray) arrayObject;

        final RubyArray newArray = new RubyArray(getContext().getCoreLibrary().getArrayClass(), originalArray.slowToArray(), originalArray.getSize());
        newArray.slowPush(pushed.execute(frame));
        return newArray;
    }

}
