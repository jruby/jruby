/*
 * Copyright (c) 2013 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.nodes.core;

import com.oracle.truffle.api.SourceSection;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.NodeChildren;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.nodes.NodeInfo;
import com.oracle.truffle.api.nodes.UnexpectedResultException;
import org.jruby.truffle.nodes.RubyNode;
import org.jruby.truffle.runtime.NilPlaceholder;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.core.array.*;

@NodeInfo(shortName = "array-tail")
@NodeChildren({@NodeChild(value = "array", type = RubyNode.class)})
public abstract class ArrayGetTailNode extends RubyNode {

    final int index;

    public ArrayGetTailNode(RubyContext context, SourceSection sourceSection, int index) {
        super(context, sourceSection);
        this.index = index;
    }

    public ArrayGetTailNode(ArrayGetTailNode prev) {
        super(prev);
        index = prev.index;
    }

    @Specialization
    public Object getTail(RubyArray array) {
        return array.getRangeExclusive(index, array.size());
    }

    protected boolean isEmptyStore(RubyArray receiver) {
        return receiver.getArrayStore() instanceof EmptyArrayStore;
    }

    protected boolean isFixnumStore(RubyArray receiver) {
        return receiver.getArrayStore() instanceof FixnumArrayStore;
    }

    protected boolean isFixnumImmutablePairStore(RubyArray receiver) {
        return receiver.getArrayStore() instanceof FixnumImmutablePairArrayStore;
    }

    protected boolean isObjectStore(RubyArray receiver) {
        return receiver.getArrayStore() instanceof ObjectArrayStore;
    }

    protected boolean isObjectImmutablePairStore(RubyArray receiver) {
        return receiver.getArrayStore() instanceof ObjectImmutablePairArrayStore;
    }

}
