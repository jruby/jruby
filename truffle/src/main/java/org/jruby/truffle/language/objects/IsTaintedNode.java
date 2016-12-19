/*
 * Copyright (c) 2015, 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.language.objects;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.object.DynamicObject;
import org.jruby.truffle.Layouts;
import org.jruby.truffle.language.RubyNode;

@NodeChild(value = "child", type = RubyNode.class)
public abstract class IsTaintedNode extends RubyNode {

    public static IsTaintedNode create() {
        return IsTaintedNodeGen.create(null);
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

    @Specialization(guards = "isRubySymbol(object) || isNil(object)")
    protected boolean isTaintedNilOrSymbol(DynamicObject object) {
        return false;
    }

    @Specialization(guards = {"!isRubySymbol(object)", "!isNil(object)"})
    protected boolean isTainted(
        DynamicObject object,
        @Cached("createReadTaintedNode()") ReadObjectFieldNode readTaintedNode) {
        return (boolean) readTaintedNode.execute(object);
    }

    @Fallback
    public boolean isTainted(Object object) {
        return false;
    }

    protected ReadObjectFieldNode createReadTaintedNode() {
        return ReadObjectFieldNodeGen.create(Layouts.TAINTED_IDENTIFIER, false);
    }

}
