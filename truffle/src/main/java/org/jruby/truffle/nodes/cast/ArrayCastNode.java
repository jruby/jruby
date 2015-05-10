/*
 * Copyright (c) 2014, 2015 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.nodes.cast;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.source.SourceSection;
import org.jruby.truffle.nodes.RubyNode;
import org.jruby.truffle.nodes.dispatch.CallDispatchHeadNode;
import org.jruby.truffle.nodes.dispatch.DispatchHeadNodeFactory;
import org.jruby.truffle.nodes.dispatch.DispatchNode;
import org.jruby.truffle.nodes.dispatch.MissingBehavior;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.control.RaiseException;
import org.jruby.truffle.runtime.core.RubyArray;
import org.jruby.truffle.runtime.core.RubyBasicObject;
import org.jruby.truffle.runtime.core.RubyBignum;

/*
 * TODO(CS): could probably unify this with SplatCastNode with some final configuration options.
 */
@NodeChild("child")
public abstract class ArrayCastNode extends RubyNode {

    private final SplatCastNode.NilBehavior nilBehavior;

    @Child private CallDispatchHeadNode toArrayNode;

    public ArrayCastNode(RubyContext context, SourceSection sourceSection) {
        this(context, sourceSection, SplatCastNode.NilBehavior.NIL);
    }

    public ArrayCastNode(RubyContext context, SourceSection sourceSection, SplatCastNode.NilBehavior nilBehavior) {
        super(context, sourceSection);
        toArrayNode = DispatchHeadNodeFactory.createMethodCall(context, MissingBehavior.RETURN_MISSING);
        this.nilBehavior = nilBehavior;
    }

    protected abstract RubyNode getChild();

    @Specialization
    public RubyBasicObject cast(boolean value) {
        return nil();
    }

    @Specialization
    public RubyBasicObject cast(int value) {
        return nil();
    }

    @Specialization
    public RubyBasicObject cast(long value) {
        return nil();
    }

    @Specialization
    public RubyBasicObject cast(double value) {
        return nil();
    }

    @Specialization(guards = "isRubyBignum(value)")
    public RubyBasicObject cast(RubyBasicObject value) {
        return nil();
    }

    @Specialization
    public RubyArray cast(RubyArray array) {
        return array;
    }

    @Specialization(guards = "isNil(nil)")
    public Object cast(Object nil) {
        switch (nilBehavior) {
            case EMPTY_ARRAY:
                return new RubyArray(getContext().getCoreLibrary().getArrayClass());

            case ARRAY_WITH_NIL:
                return RubyArray.fromObject(getContext().getCoreLibrary().getArrayClass(), nil());

            case NIL:
                return nil;

            default: {
                CompilerAsserts.neverPartOfCompilation();
                throw new UnsupportedOperationException();
            }
        }
    }

    @Specialization(guards = {"!isNil(object)", "!isRubyArray(object)"})
    public Object cast(VirtualFrame frame, RubyBasicObject object) {
        final Object result = toArrayNode.call(frame, object, "to_ary", null, new Object[]{});

        if (result == DispatchNode.MISSING) {
            return nil();
        }

        if (!(result instanceof RubyArray)) {
            CompilerDirectives.transferToInterpreter();
            throw new RaiseException(getContext().getCoreLibrary().typeErrorShouldReturn(object.toString(), "to_ary", "Array", this));
        }

        return result;
    }

    @Override
    public void executeVoid(VirtualFrame frame) {
        getChild().executeVoid(frame);
    }

}
