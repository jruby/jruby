/*
 * Copyright (c) 2013, 2017 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.core.cast;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.profiles.BranchProfile;
import org.jruby.truffle.Layouts;
import org.jruby.truffle.core.array.ArrayDupNode;
import org.jruby.truffle.core.array.ArrayDupNodeGen;
import org.jruby.truffle.language.RubyGuards;
import org.jruby.truffle.language.RubyNode;
import org.jruby.truffle.language.SnippetNode;
import org.jruby.truffle.language.control.RaiseException;
import org.jruby.truffle.language.dispatch.CallDispatchHeadNode;
import org.jruby.truffle.language.dispatch.DispatchHeadNodeFactory;
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

    @Child private ArrayDupNode dup = ArrayDupNodeGen.create(null);
    @Child private CallDispatchHeadNode toA = DispatchHeadNodeFactory.createMethodCall(true, MissingBehavior.RETURN_MISSING);

    public SplatCastNode(NilBehavior nilBehavior, boolean useToAry) {
        this.nilBehavior = nilBehavior;
        // Calling private #to_a is allowed for the *splat operator.
        String name = useToAry ? "to_ary" : "to_a";
        conversionMethod = getContext().getSymbolTable().getSymbol(name);
    }

    public abstract RubyNode getChild();

    @Specialization(guards = "isNil(nil)")
    public Object splatNil(VirtualFrame frame, Object nil) {
        switch (nilBehavior) {
            case EMPTY_ARRAY:
            return createArray(null, 0);

            case ARRAY_WITH_NIL:
            return createArray(new Object[] { nil() }, 1);

            case CONVERT:
                return toA.call(frame, nil, "to_a");

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
        // It is needed at least for [*ary] (parsed as just a SplatParseNode) and b = *ary.
        return dup.executeDup(frame, array);
    }

    @Specialization(guards = {"!isNil(object)", "!isRubyArray(object)"})
    public DynamicObject splat(VirtualFrame frame, Object object,
                               @Cached("create()") BranchProfile errorProfile, @Cached("new()") SnippetNode snippetNode) {
        final Object array = snippetNode.execute(frame, "Rubinius::Type.rb_check_convert_type(v, Array, method)",
            "v", object, "method", conversionMethod);
        if (RubyGuards.isRubyArray(array)) {
            return (DynamicObject) array;
        } else if (array == nil()) {
            return createArray(new Object[]{object}, 1);
        } else {
            errorProfile.enter();
            throw new RaiseException(coreExceptions().typeErrorCantConvertTo(object, "Array",
                Layouts.SYMBOL.getString(conversionMethod), array, this));
        }
    }

}
