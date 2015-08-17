/*
 * Copyright (c) 2015 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.nodes.cast;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.source.SourceSection;
import org.jruby.truffle.nodes.RubyNode;
import org.jruby.truffle.nodes.core.array.ArrayNodes;
import org.jruby.truffle.runtime.RubyContext;

@NodeChild(value = "child", type = RubyNode.class)
public abstract class SingleValueCastNode extends RubyNode {

    public SingleValueCastNode(RubyContext context, SourceSection sourceSection) {
        super(context, sourceSection);
    }

    public abstract Object executeSingleValue(VirtualFrame frame, Object[] args);

    @Specialization(guards = "noArguments(args)")
    protected DynamicObject castNil(Object[] args) {
        return nil();
    }

    @Specialization(guards = "singleArgument(args)")
    protected Object castSingle(Object[] args) {
        return args[0];
    }

    @TruffleBoundary
    @Specialization(guards = { "!noArguments(args)", "!singleArgument(args)" })
    protected DynamicObject castMany(Object[] args) {
        return ArrayNodes.fromObjects(getContext().getCoreLibrary().getArrayClass(), args);
    }

    protected boolean noArguments(Object[] args) {
        return args.length == 0;
    }

    protected boolean singleArgument(Object[] args) {
        return args.length == 1;
    }

}
