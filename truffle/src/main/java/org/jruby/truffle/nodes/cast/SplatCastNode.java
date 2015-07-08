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

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.source.SourceSection;
import org.jruby.truffle.nodes.RubyGuards;
import org.jruby.truffle.nodes.RubyNode;
import org.jruby.truffle.nodes.core.array.ArrayDupNode;
import org.jruby.truffle.nodes.core.array.ArrayDupNodeGen;
import org.jruby.truffle.nodes.core.array.ArrayNodes;
import org.jruby.truffle.nodes.dispatch.CallDispatchHeadNode;
import org.jruby.truffle.nodes.dispatch.DispatchHeadNodeFactory;
import org.jruby.truffle.nodes.dispatch.DispatchNode;
import org.jruby.truffle.nodes.dispatch.MissingBehavior;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.control.RaiseException;
import org.jruby.truffle.runtime.core.RubyBasicObject;

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
        dup = ArrayDupNodeGen.create(context, sourceSection, null);
        respondToToA = DispatchHeadNodeFactory.createMethodCall(context, true, MissingBehavior.RETURN_MISSING);
        respondToCast = BooleanCastNodeGen.create(context, sourceSection, null);
        toA = DispatchHeadNodeFactory.createMethodCall(context, true, MissingBehavior.RETURN_MISSING);
        this.useToAry = useToAry;
    }

    protected abstract RubyNode getChild();

    @Specialization(guards = "isNil(nil)")
    public RubyBasicObject splat(Object nil) {
        switch (nilBehavior) {
            case EMPTY_ARRAY:
                return createEmptyArray();

            case ARRAY_WITH_NIL:
                return ArrayNodes.fromObject(getContext().getCoreLibrary().getArrayClass(), nil());

            default: {
                throw new UnsupportedOperationException();
            }
        }
    }

    @Specialization(guards = "isRubyArray(array)")
    public RubyBasicObject splat(VirtualFrame frame, RubyBasicObject array) {
        // TODO(cs): is it necessary to dup here in all cases?
        // It is needed at least for [*ary] (parsed as just a SplatNode) and b = *ary.
        return dup.executeDup(frame, array);
    }

    @Specialization(guards = {"!isNil(object)", "!isRubyArray(object)"})
    public RubyBasicObject splat(VirtualFrame frame, Object object) {
        final String method;

        if (useToAry) {
            method = "to_ary";
        } else {
            method = "to_a";
        }

        // MRI tries to call dynamic respond_to? here.
        Object respondToResult = respondToToA.call(frame, object, "respond_to?", null, createString(method), true);
        if (respondToResult != DispatchNode.MISSING && respondToCast.executeBoolean(frame, respondToResult)) {
            final Object array = toA.call(frame, object, method, null);

            if (RubyGuards.isRubyArray(array)) {
                return (RubyBasicObject) array;
            } else if (array == nil() || array == DispatchNode.MISSING) {
                CompilerDirectives.transferToInterpreter();
                return ArrayNodes.fromObject(getContext().getCoreLibrary().getArrayClass(), object);
            } else {
                throw new RaiseException(getContext().getCoreLibrary().typeErrorCantConvertTo(
                        object, getContext().getCoreLibrary().getArrayClass(), method, array, this)
                );
            }
        }

        return ArrayNodes.fromObject(getContext().getCoreLibrary().getArrayClass(), object);
    }

}
