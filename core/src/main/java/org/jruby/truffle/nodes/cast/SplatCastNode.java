/*
 * Copyright (c) 2013 Oracle and/or its affiliates. All rights reserved. This
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
import com.oracle.truffle.api.nodes.*;
import org.jruby.truffle.nodes.*;
import org.jruby.truffle.runtime.*;
import org.jruby.truffle.runtime.core.RubyArray;
import org.jruby.truffle.runtime.core.RubyBasicObject;
import org.jruby.truffle.runtime.methods.RubyMethod;

/**
 * Splat as used to cast a value to an array if it isn't already, as in {@code *value}.
 */
@NodeInfo(shortName = "cast-splat")
@NodeChild("child")
public abstract class SplatCastNode extends RubyNode {

    public static enum NilBehavior {
        EMPTY_ARRAY,
        ARRAY_WITH_NIL
    }

    private final NilBehavior nilBehavior;

    public SplatCastNode(RubyContext context, SourceSection sourceSection, NilBehavior nilBehavior) {
        super(context, sourceSection);
        this.nilBehavior = nilBehavior;
    }

    public SplatCastNode(SplatCastNode prev) {
        super(prev);
        nilBehavior = prev.nilBehavior;
    }

    protected abstract RubyNode getChild();

    @Specialization
    public RubyArray doArray(RubyArray array) {
        return array;
    }

    @Specialization
    public RubyArray doObject(Object object) {
        notDesignedForCompilation();

        if (object == NilPlaceholder.INSTANCE) {
            switch (nilBehavior) {
                case EMPTY_ARRAY:
                    return new RubyArray(getContext().getCoreLibrary().getArrayClass());

                case ARRAY_WITH_NIL:
                    return RubyArray.fromObject(getContext().getCoreLibrary().getArrayClass(), NilPlaceholder.INSTANCE);

                default: {
                    CompilerAsserts.neverPartOfCompilation();
                    throw new UnsupportedOperationException();
                }
            }
        } else if (object instanceof RubyArray) {
            return (RubyArray) object;
        } else {
            // TODO(CS): need to specialize for this

            final RubyBasicObject boxedObject = getContext().getCoreLibrary().box(object);

            final RubyMethod toA = boxedObject.getLookupNode().lookupMethod("to_a");

            if (toA != null) {
                final Object toAResult = toA.call(boxedObject, null);

                if (toAResult instanceof RubyArray) {
                    return (RubyArray) toAResult;
                }
            }

            return RubyArray.fromObject(getContext().getCoreLibrary().getArrayClass(), object);
        }
    }

    @Override
    public void executeVoid(VirtualFrame frame) {
        getChild().executeVoid(frame);
    }

}
