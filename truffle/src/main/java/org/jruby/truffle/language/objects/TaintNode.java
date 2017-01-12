/*
 * Copyright (c) 2015, 2017 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.language.objects;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.object.DynamicObject;
import org.jruby.truffle.Layouts;
import org.jruby.truffle.language.RubyNode;
import org.jruby.truffle.language.control.RaiseException;

@NodeChild(value = "child", type = RubyNode.class)
public abstract class TaintNode extends RubyNode {

    @Child private IsFrozenNode isFrozenNode;
    @Child private IsTaintedNode isTaintedNode;

    public static TaintNode create() {
        return TaintNodeGen.create(null);
    }

    public abstract Object executeTaint(Object object);

    @Specialization
    public Object taint(boolean object) {
        return object;
    }

    @Specialization
    public Object taint(int object) {
        return object;
    }

    @Specialization
    public Object taint(long object) {
        return object;
    }

    @Specialization
    public Object taint(double object) {
        return object;
    }

    @Specialization(guards = "isRubySymbol(object) || isNil(object)")
    public Object taintNilOrSymbol(DynamicObject object) {
        return object;
    }

    @Specialization(guards = {"!isRubySymbol(object)", "!isNil(object)"})
    public Object taint(
        DynamicObject object,
        @Cached("createWriteTaintNode()") WriteObjectFieldNode writeTaintNode) {

        if (isTaintedNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            isTaintedNode = insert(IsTaintedNode.create());
        }

        if (!isTaintedNode.executeIsTainted(object)) {
            if (isFrozenNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                isFrozenNode = insert(IsFrozenNodeGen.create(null));
            }

            if (isFrozenNode.executeIsFrozen(object)) {
                throw new RaiseException(coreExceptions().frozenError(object, this));
            }
        }

        writeTaintNode.execute(object, true);
        return object;
    }

    protected WriteObjectFieldNode createWriteTaintNode() {
        return WriteObjectFieldNodeGen.create(Layouts.TAINTED_IDENTIFIER);
    }

}
