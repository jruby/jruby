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
import com.oracle.truffle.api.source.SourceSection;
import org.jruby.truffle.nodes.RubyNode;
import org.jruby.truffle.nodes.objectstorage.WriteHeadObjectFieldNode;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.control.RaiseException;
import org.jruby.truffle.runtime.core.RubyBasicObject;
import org.jruby.truffle.runtime.core.RubySymbol;

@NodeChild(value = "child", type = RubyNode.class)
public abstract class TaintNode extends RubyNode {

    @Child private WriteHeadObjectFieldNode writeTaintNode;

    public TaintNode(RubyContext context, SourceSection sourceSection) {
        super(context, sourceSection);
    }

    public TaintNode(TaintNode prev) {
        super(prev);
        writeTaintNode = prev.writeTaintNode;
    }

    public abstract Object executeTaint(Object object);

    @Specialization
    public Object taint(boolean object) {
        return frozen(object);
    }

    @Specialization
    public Object taint(int object) {
        return frozen(object);
    }

    @Specialization
    public Object taint(long object) {
        return frozen(object);
    }

    @Specialization
    public Object taint(double object) {
        return frozen(object);
    }

    @Specialization
    public Object taint(RubySymbol object) {
        return frozen(object);
    }

    @Specialization(guards = "!isRubySymbol(object)")
    public Object taint(RubyBasicObject object) {
        if (writeTaintNode == null) {
            CompilerDirectives.transferToInterpreter();
            writeTaintNode = insert(new WriteHeadObjectFieldNode(RubyBasicObject.TAINTED_IDENTIFIER));
        }
        writeTaintNode.execute(object, true);
        return object;
    }

    private Object frozen(Object object) {
        CompilerDirectives.transferToInterpreter();
        throw new RaiseException(getContext().getCoreLibrary().frozenError(getContext().getCoreLibrary().getLogicalClass(object).getName(), this));
    }

}
