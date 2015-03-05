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
import org.jruby.truffle.runtime.core.RubySymbol;

@NodeChild(value = "child")
public abstract class IsFrozenNode extends RubyNode {

    @Child private ReadHeadObjectFieldNode readFrozenNode;

    public IsFrozenNode(RubyContext context, SourceSection sourceSection) {
        super(context, sourceSection);
    }

    public abstract boolean executeIsFrozen(Object object);

    public IsFrozenNode(IsFrozenNode prev) {
        super(prev);
        readFrozenNode = prev.readFrozenNode;
    }

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

    @Specialization
    public boolean isFrozen(RubySymbol object) {
        return true;
    }

    @Specialization(guards = "!isRubySymbol(object)")
    public boolean isFrozen(RubyBasicObject object) {
        if (readFrozenNode == null) {
            CompilerDirectives.transferToInterpreter();
            readFrozenNode = insert(new ReadHeadObjectFieldNode(RubyBasicObject.FROZEN_IDENTIFIER));
        }

        try {
            return readFrozenNode.isSet(object) && readFrozenNode.executeBoolean(object);
        } catch (UnexpectedResultException e) {
            throw new UnsupportedOperationException(readFrozenNode.execute(object).toString());
        }
    }
}
