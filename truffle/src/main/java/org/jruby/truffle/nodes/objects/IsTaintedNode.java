/*
 * Copyright (c) 2015 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.nodes.objects;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.nodes.UnexpectedResultException;
import com.oracle.truffle.api.source.SourceSection;
import org.jruby.truffle.nodes.RubyNode;
import org.jruby.truffle.nodes.objectstorage.ReadHeadObjectFieldNode;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.core.RubyBasicObject;

@NodeChild(value = "child", type = RubyNode.class)
public abstract class IsTaintedNode extends RubyNode {

    @Child private ReadHeadObjectFieldNode readTaintNode;

    public IsTaintedNode(RubyContext context, SourceSection sourceSection) {
        super(context, sourceSection);
    }

    public IsTaintedNode(IsTaintedNode prev) {
        super(prev);
        readTaintNode = prev.readTaintNode;
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
    public boolean isTainted(RubyBasicObject object) {
        if (readTaintNode == null) {
            CompilerDirectives.transferToInterpreter();
            readTaintNode = insert(new ReadHeadObjectFieldNode(RubyBasicObject.TAINTED_IDENTIFIER));
        }

        try {
            return readTaintNode.isSet(object) && readTaintNode.executeBoolean(object);
        } catch (UnexpectedResultException e) {
            throw new UnsupportedOperationException(readTaintNode.execute(object).toString());
        }
    }

}
