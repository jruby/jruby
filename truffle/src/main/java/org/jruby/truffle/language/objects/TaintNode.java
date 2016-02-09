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

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.source.SourceSection;
import org.jruby.truffle.language.RubyNode;
import org.jruby.truffle.RubyContext;
import org.jruby.truffle.language.control.RaiseException;
import org.jruby.truffle.core.Layouts;

@NodeChild(value = "child", type = RubyNode.class)
public abstract class TaintNode extends RubyNode {

    @Child private WriteHeadObjectFieldNode writeTaintNode;

    public TaintNode(RubyContext context, SourceSection sourceSection) {
        super(context, sourceSection);
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

    @Specialization(guards = "isRubySymbol(symbol)")
    public Object taintSymbol(DynamicObject symbol) {
        return frozen(symbol);
    }

    @Specialization(guards = "!isRubySymbol(object)")
    public Object taint(DynamicObject object) {
        if (writeTaintNode == null) {
            CompilerDirectives.transferToInterpreter();
            writeTaintNode = insert(WriteHeadObjectFieldNodeGen.create(getContext(), Layouts.TAINTED_IDENTIFIER));
        }
        writeTaintNode.execute(object, true);
        return object;
    }

    private Object frozen(Object object) {
        CompilerDirectives.transferToInterpreter();
        throw new RaiseException(getContext().getCoreLibrary().frozenError(Layouts.MODULE.getFields(getContext().getCoreLibrary().getLogicalClass(object)).getName(), this));
    }

}
