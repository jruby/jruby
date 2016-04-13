/*
 * Copyright (c) 2013, 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.core.cast;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.source.SourceSection;
import org.jruby.truffle.RubyContext;
import org.jruby.truffle.core.Layouts;
import org.jruby.truffle.core.array.ArrayDupNode;
import org.jruby.truffle.core.array.ArrayDupNodeGen;
import org.jruby.truffle.language.RubyGuards;
import org.jruby.truffle.language.RubyNode;
import org.jruby.truffle.language.control.RaiseException;
import org.jruby.truffle.language.dispatch.CallDispatchHeadNode;
import org.jruby.truffle.language.dispatch.DispatchHeadNodeFactory;
import org.jruby.truffle.language.dispatch.DispatchNode;
import org.jruby.truffle.language.dispatch.MissingBehavior;

/**
 * Splat as used to cast a value to an array if it isn't already, as in {@code *value}.
 */
@NodeChild("child")
public abstract class SplatCastNode extends RubyNode {

    public enum NilBehavior {
        EMPTY_ARRAY,
        ARRAY_WITH_NIL,
        NIL,
        CONVERT
    }

    private final NilBehavior nilBehavior;
    private final DynamicObject conversionMethod;

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
        String name = useToAry ? "to_ary" : "to_a";
        conversionMethod = context.getSymbolTable().getSymbol(name);
    }

    public abstract RubyNode getChild();

    @Specialization(guards = "isNil(nil)")
    public Object splatNil(VirtualFrame frame, Object nil) {
        switch (nilBehavior) {
            case EMPTY_ARRAY:
                return Layouts.ARRAY.createArray(coreLibrary().getArrayFactory(), null, 0);

            case ARRAY_WITH_NIL:
                return Layouts.ARRAY.createArray(coreLibrary().getArrayFactory(), new Object[]{nil()}, 1);

            case CONVERT:
                return toA.call(frame, nil, "to_a", null);

            case NIL:
                return nil;

            default: {
                throw new UnsupportedOperationException();
            }
        }
    }

    @Specialization(guards = "isRubyArray(array)")
    public DynamicObject splat(VirtualFrame frame, DynamicObject array) {
        // TODO(cs): is it necessary to dup here in all cases?
        // It is needed at least for [*ary] (parsed as just a SplatNode) and b = *ary.
        return dup.executeDup(frame, array);
    }

    @Specialization(guards = { "!isNil(object)", "!isRubyArray(object)" })
    public DynamicObject splat(VirtualFrame frame, Object object) {
        // MRI tries to call dynamic respond_to? here.
        Object respondToResult = respondToToA.call(frame, object, "respond_to?", null, conversionMethod, true);
        if (respondToResult != DispatchNode.MISSING && respondToCast.executeBoolean(frame, respondToResult)) {
            final Object array = toA.call(frame, object, conversionMethod, null);

            if (RubyGuards.isRubyArray(array)) {
                return (DynamicObject) array;
            } else if (array == nil() || array == DispatchNode.MISSING) {
                CompilerDirectives.transferToInterpreter();
                return Layouts.ARRAY.createArray(coreLibrary().getArrayFactory(), new Object[] { object }, 1);
            } else {
                CompilerDirectives.transferToInterpreter();
                throw new RaiseException(coreLibrary().typeErrorCantConvertTo(object, "Array", Layouts.SYMBOL.getString(conversionMethod), array, this));
            }
        }

        return Layouts.ARRAY.createArray(coreLibrary().getArrayFactory(), new Object[] { object }, 1);
    }

}
