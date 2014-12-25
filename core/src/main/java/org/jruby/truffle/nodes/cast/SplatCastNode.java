/*
 * Copyright (c) 2013, 2014 Oracle and/or its affiliates. All rights reserved. This
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
import org.jruby.RubyNil;
import org.jruby.truffle.nodes.*;
import org.jruby.truffle.nodes.core.ArrayDupNode;
import org.jruby.truffle.nodes.core.ArrayDupNodeFactory;
import org.jruby.truffle.nodes.dispatch.Dispatch;
import org.jruby.truffle.nodes.dispatch.DispatchHeadNode;
import org.jruby.truffle.runtime.*;
import org.jruby.truffle.runtime.control.RaiseException;
import org.jruby.truffle.runtime.core.RubyArray;
import org.jruby.truffle.runtime.core.RubyNilClass;
import org.jruby.truffle.runtime.core.RubyString;

/**
 * Splat as used to cast a value to an array if it isn't already, as in {@code *value}.
 */
@NodeChild("child")
public abstract class SplatCastNode extends RubyNode {

    public static enum NilBehavior {
        EMPTY_ARRAY,
        ARRAY_WITH_NIL
    }

    private final NilBehavior nilBehavior;

    @Child protected ArrayDupNode dup;
    @Child protected DispatchHeadNode respondToToA;
    @Child protected BooleanCastNode respondToCast;
    @Child protected DispatchHeadNode toA;

    public SplatCastNode(RubyContext context, SourceSection sourceSection, NilBehavior nilBehavior) {
        super(context, sourceSection);
        this.nilBehavior = nilBehavior;
        // Calling private #to_a is allowed for the *splat operator.
        dup = ArrayDupNodeFactory.create(context, sourceSection, null);
        respondToToA = new DispatchHeadNode(context, true, Dispatch.MissingBehavior.RETURN_MISSING);
        respondToCast = BooleanCastNodeFactory.create(context, sourceSection, null);
        toA = new DispatchHeadNode(context, true, Dispatch.MissingBehavior.RETURN_MISSING);
    }

    public SplatCastNode(SplatCastNode prev) {
        super(prev);
        dup = prev.dup;
        nilBehavior = prev.nilBehavior;
        respondToToA = prev.respondToToA;
        respondToCast = prev.respondToCast;
        toA = prev.toA;
    }

    protected abstract RubyNode getChild();

    @Specialization
    public RubyArray splat(RubyNilClass nil) {
        switch (nilBehavior) {
            case EMPTY_ARRAY:
                return new RubyArray(getContext().getCoreLibrary().getArrayClass());

            case ARRAY_WITH_NIL:
                return RubyArray.fromObject(getContext().getCoreLibrary().getArrayClass(), getContext().getCoreLibrary().getNilObject());

            default: {
                CompilerAsserts.neverPartOfCompilation();
                throw new UnsupportedOperationException();
            }
        }
    }

    @Specialization
    public RubyArray splat(VirtualFrame frame, RubyArray array) {
        // TODO(cs): is it necessary to dup here in all cases?
        // It is needed at least for [*ary] (parsed as just a SplatNode) and b = *ary.
        return dup.executeDup(frame, array);
    }

    @Specialization(guards = {"!isRubyNilClass", "!isRubyArray"})
    public RubyArray splat(VirtualFrame frame, Object object) {
        notDesignedForCompilation();

        if (respondToCast.executeBoolean(frame, respondToToA.call(frame, object, "respond_to?", null, "to_a", true))) {
            final Object array = toA.call(frame, object, "to_a", null);

            if (array instanceof RubyArray) {
                return (RubyArray) array;
            } else if (array instanceof RubyNilClass) {
                return RubyArray.fromObject(getContext().getCoreLibrary().getArrayClass(), object);
            } else {
                throw new RaiseException(getContext().getCoreLibrary().typeErrorCantConvertTo(
                        getContext().getCoreLibrary().getLogicalClass(object).getName(),
                        "Array",
                        "to_a",
                        getContext().getCoreLibrary().getLogicalClass(array).getName(),
                        this)
                );
            }
        }

        return RubyArray.fromObject(getContext().getCoreLibrary().getArrayClass(), object);
    }

}
