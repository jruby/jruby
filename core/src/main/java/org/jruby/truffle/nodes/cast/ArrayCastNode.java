/*
 * Copyright (c) 2014 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.nodes.cast;

import com.oracle.truffle.api.*;
import com.oracle.truffle.api.source.*;
import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.api.frame.*;
import org.jruby.truffle.nodes.*;
import org.jruby.truffle.nodes.dispatch.Dispatch;
import org.jruby.truffle.nodes.dispatch.DispatchHeadNode;
import org.jruby.truffle.runtime.*;
import org.jruby.truffle.runtime.control.RaiseException;
import org.jruby.truffle.runtime.core.RubyArray;
import org.jruby.truffle.runtime.core.RubyBasicObject;
import org.jruby.truffle.runtime.core.RubyBignum;
import org.jruby.truffle.runtime.core.RubyNilClass;

/*
 * TODO(CS): could probably unify this with SplatCastNode with some final configuration options.
 */
@NodeChild("child")
public abstract class ArrayCastNode extends RubyNode {

    private final SplatCastNode.NilBehavior nilBehavior;

    @Child protected DispatchHeadNode toArrayNode;

    public ArrayCastNode(RubyContext context, SourceSection sourceSection) {
        this(context, sourceSection, SplatCastNode.NilBehavior.NIL);
    }

    public ArrayCastNode(RubyContext context, SourceSection sourceSection, SplatCastNode.NilBehavior nilBehavior) {
        super(context, sourceSection);
        toArrayNode = new DispatchHeadNode(context, Dispatch.MissingBehavior.RETURN_MISSING);
        this.nilBehavior = nilBehavior;
    }

    public ArrayCastNode(ArrayCastNode prev) {
        super(prev);
        toArrayNode = prev.toArrayNode;
        nilBehavior = prev.nilBehavior;
    }

    protected abstract RubyNode getChild();

    @Specialization
    public RubyNilClass cast(boolean value) {
        return getContext().getCoreLibrary().getNilObject();
    }

    @Specialization
    public RubyNilClass cast(int value) {
        return getContext().getCoreLibrary().getNilObject();
    }

    @Specialization
    public RubyNilClass cast(long value) {
        return getContext().getCoreLibrary().getNilObject();
    }

    @Specialization
    public RubyNilClass cast(double value) {
        return getContext().getCoreLibrary().getNilObject();
    }

    @Specialization
    public RubyNilClass cast(RubyBignum value) {
        return getContext().getCoreLibrary().getNilObject();
    }

    @Specialization
    public RubyArray cast(RubyArray array) {
        return array;
    }

    @Specialization
    public Object cast(RubyNilClass nil) {
        switch (nilBehavior) {
            case EMPTY_ARRAY:
                return new RubyArray(getContext().getCoreLibrary().getArrayClass());

            case ARRAY_WITH_NIL:
                return RubyArray.fromObject(getContext().getCoreLibrary().getArrayClass(), getContext().getCoreLibrary().getNilObject());

            case NIL:
                return nil;

            default: {
                CompilerAsserts.neverPartOfCompilation();
                throw new UnsupportedOperationException();
            }
        }
    }

    @Specialization(guards = {"!isRubyNilClass", "!isRubyArray"})
    public Object cast(VirtualFrame frame, RubyBasicObject object) {
        notDesignedForCompilation();

        final Object result = toArrayNode.call(frame, object, "to_ary", null, new Object[]{});

        if (result == Dispatch.MISSING) {
            return getContext().getCoreLibrary().getNilObject();
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
