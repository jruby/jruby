/*
 * Copyright (c) 2015 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.language.objects;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.source.SourceSection;
import org.jruby.truffle.language.RubyNode;
import org.jruby.truffle.RubyContext;
import org.jruby.truffle.runtime.layouts.Layouts;

@NodeChild(value = "child", type = RubyNode.class)
public abstract class IsTaintedNode extends RubyNode {

    @Child private ReadHeadObjectFieldNode readTaintNode;

    public IsTaintedNode(RubyContext context, SourceSection sourceSection) {
        super(context, sourceSection);
    }

    public abstract boolean executeIsTainted(Object object);

    @Specialization
    public boolean isTainted(boolean object) {
        return false;
    }

    @Specialization
    public boolean isTainted(int object) {
        return false;
    }

    @Specialization
    public boolean isTainted(long object) {
        return false;
    }

    @Specialization
    public boolean isTainted(double object) {
        return false;
    }

    @Specialization
    protected boolean isTainted(DynamicObject object,
            @Cached("createReadTaintedNode()") ReadHeadObjectFieldNode readTaintedNode) {
        return (boolean) readTaintedNode.execute(object);
    }

    protected ReadHeadObjectFieldNode createReadTaintedNode() {
        return ReadHeadObjectFieldNodeGen.create(getContext(), Layouts.TAINTED_IDENTIFIER, false);
    }

}
