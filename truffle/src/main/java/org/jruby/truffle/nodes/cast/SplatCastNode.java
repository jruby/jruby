/*
 * Copyright (c) 2013, 2015 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.nodes.cast;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.source.SourceSection;
import org.jruby.truffle.nodes.RubyNode;
import org.jruby.truffle.nodes.core.ArrayDupNode;
import org.jruby.truffle.nodes.core.ArrayDupNodeFactory;
import org.jruby.truffle.nodes.dispatch.*;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.control.RaiseException;
import org.jruby.truffle.runtime.core.RubyArray;
import org.jruby.truffle.runtime.core.RubyNilClass;

/**
 * Splat as used to cast a value to an array if it isn't already, as in {@code *value}.
 */
@NodeChild("child")
public abstract class SplatCastNode extends RubyNode {

    public static enum NilBehavior {
        EMPTY_ARRAY,
        ARRAY_WITH_NIL,
        NIL
    }

    private final NilBehavior nilBehavior;
    private final boolean useToAry;

    @Child private ArrayDupNode dup;
    @Child private CallDispatchHeadNode respondToToA;
    @Child private BooleanCastNode respondToCast;
    @Child private CallDispatchHeadNode toA;

    public SplatCastNode(RubyContext context, SourceSection sourceSection, NilBehavior nilBehavior, boolean useToAry) {
        super(context, sourceSection);
        this.nilBehavior = nilBehavior;
        // Calling private #to_a is allowed for the *splat operator.
        dup = ArrayDupNodeFactory.create(context, sourceSection, null);
        respondToToA = DispatchHeadNodeFactory.createMethodCall(context, true, MissingBehavior.RETURN_MISSING);
        respondToCast = BooleanCastNodeFactory.create(context, sourceSection, null);
        toA = DispatchHeadNodeFactory.createMethodCall(context, true, MissingBehavior.RETURN_MISSING);
        this.useToAry = useToAry;
    }

    protected abstract RubyNode getChild();

    @Specialization
    public RubyArray splat(RubyNilClass nil) {
        switch (nilBehavior) {
            case EMPTY_ARRAY:
                return new RubyArray(getContext().getCoreLibrary().getArrayClass());

            case ARRAY_WITH_NIL:
                return RubyArray.fromObject(getContext().getCoreLibrary().getArrayClass(), nil());

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

    @Specialization(guards = {"!isRubyNilClass(object)", "!isRubyArray(object)"})
    public RubyArray splat(VirtualFrame frame, Object object) {
        notDesignedForCompilation();

        final String method;

        if (useToAry) {
            method = "to_ary";
        } else {
            method = "to_a";
        }

        // MRI tries to call dynamic respond_to? here.
        Object respondToResult = respondToToA.call(frame, object, "respond_to?", null, getContext().makeString(method), true);
        if (respondToResult != DispatchNode.MISSING && respondToCast.executeBoolean(frame, respondToResult)) {
            final Object array = toA.call(frame, object, method, null);

            if (array instanceof RubyArray) {
                return (RubyArray) array;
            } else if (array instanceof RubyNilClass || array == DispatchNode.MISSING) {
                return RubyArray.fromObject(getContext().getCoreLibrary().getArrayClass(), object);
            } else {
                throw new RaiseException(getContext().getCoreLibrary().typeErrorCantConvertTo(
                        object, getContext().getCoreLibrary().getArrayClass(), method, array, this)
                );
            }
        }

        return RubyArray.fromObject(getContext().getCoreLibrary().getArrayClass(), object);
    }

}
