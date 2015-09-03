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

import org.jruby.truffle.nodes.RubyNode;
import org.jruby.truffle.nodes.objectstorage.ReadHeadObjectFieldNode;
import org.jruby.truffle.nodes.objectstorage.ReadHeadObjectFieldNodeGen;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.layouts.Layouts;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.nodes.UnexpectedResultException;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.source.SourceSection;

@NodeChild(value = "child")
public abstract class IsFrozenNode extends RubyNode {

    public IsFrozenNode(RubyContext context, SourceSection sourceSection) {
        super(context, sourceSection);
    }

    public abstract boolean executeIsFrozen(Object object);

    @Specialization
    public boolean isFrozen(boolean object) {
        return true;
    }

    @Specialization
    public boolean isFrozen(int object) {
        return true;
    }

    @Specialization
    public boolean isFrozen(long object) {
        return true;
    }

    @Specialization
    public boolean isFrozen(double object) {
        return true;
    }

    @Specialization(guards = "isNil(nil)")
    public boolean isFrozen(Object nil) {
        return true;
    }

    @Specialization(guards = "isRubyBignum(object)")
    public boolean isFrozenBignum(DynamicObject object) {
        return true;
    }

    @Specialization(guards = "isRubySymbol(symbol)")
    public boolean isFrozenSymbol(DynamicObject symbol) {
        return true;
    }

    @Specialization(guards = { "!isNil(object)", "!isRubyBignum(object)", "!isRubySymbol(object)" })
    protected boolean isFrozen(DynamicObject object,
            @Cached("createReadFrozenNode()") ReadHeadObjectFieldNode readFrozenNode) {
        try {
            return readFrozenNode.executeBoolean(object);
        } catch (UnexpectedResultException e) {
            throw new UnsupportedOperationException(e);
        }
    }

    protected ReadHeadObjectFieldNode createReadFrozenNode() {
        return ReadHeadObjectFieldNodeGen.create(getContext(), getSourceSection(), Layouts.FROZEN_IDENTIFIER, false, null);
    }
}
