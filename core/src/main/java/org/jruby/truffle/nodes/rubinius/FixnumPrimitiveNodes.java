/*
 * Copyright (c) 2015 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.nodes.rubinius;

import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.source.SourceSection;
import com.oracle.truffle.api.utilities.ConditionProfile;
import org.jruby.truffle.nodes.dispatch.DispatchAction;
import org.jruby.truffle.nodes.dispatch.DispatchHeadNode;
import org.jruby.truffle.nodes.dispatch.MissingBehavior;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.control.RaiseException;
import org.jruby.truffle.runtime.core.RubyArray;
import org.jruby.truffle.runtime.core.RubyString;
import org.jruby.util.StringSupport;

/**
 * Rubinius primitives associated with the Ruby {@code Fixnum} class.
 */
public abstract class FixnumPrimitiveNodes {

    @RubiniusPrimitive(name = "fixnum_coerce")
    public static abstract class FixnumCoercePrimitiveNode extends RubiniusPrimitiveNode {

        @Child protected DispatchHeadNode toFRespond;
        @Child protected DispatchHeadNode toF;

        public FixnumCoercePrimitiveNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            toFRespond = new DispatchHeadNode(context, false, false, MissingBehavior.RETURN_MISSING, null, DispatchAction.RESPOND_TO_METHOD);
            toF = new DispatchHeadNode(context);
        }

        public FixnumCoercePrimitiveNode(FixnumCoercePrimitiveNode prev) {
            super(prev);
            toFRespond = prev.toFRespond;
            toF = prev.toF;
        }

        @Specialization
        public RubyArray coerce(int a, int b) {
            return new RubyArray(getContext().getCoreLibrary().getArrayClass(), new int[]{b, a}, 2);
        }

        @Specialization
        public RubyArray coerce(int a, RubyString b) {
            try {
                return new RubyArray(getContext().getCoreLibrary().getArrayClass(), new double[]{Double.parseDouble(b.toString()), a}, 2);
            } catch (NumberFormatException e) {
                throw new RaiseException(getContext().getCoreLibrary().argumentError("invalid value for Float", this));
            }
        }

        @Specialization(guards = {"!isRubyString(arguments[1])", "!isRubyNilObject(arguments[1])"})
        public RubyArray coerce(VirtualFrame frame, int a, Object b) {
            if (toFRespond.doesRespondTo(frame, "to_f", b)) {
                final Object bFloat = toF.call(frame, b, "to_f", null);

                if (bFloat instanceof Double) {
                    return new RubyArray(getContext().getCoreLibrary().getArrayClass(), new double[]{(double) bFloat, a}, 2);
                } else {
                    throw new RaiseException(getContext().getCoreLibrary().typeError("?", this));
                }
            } else {
                throw new RaiseException(getContext().getCoreLibrary().typeError("?", this));
            }
        }

    }

}
