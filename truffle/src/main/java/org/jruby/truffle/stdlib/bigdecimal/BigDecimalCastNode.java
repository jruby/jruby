/*
 * Copyright (c) 2013, 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.stdlib.bigdecimal;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.NodeChildren;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.source.SourceSection;
import org.jruby.RubyBignum;
import org.jruby.RubyFixnum;
import org.jruby.RubyRational;
import org.jruby.ext.bigdecimal.RubyBigDecimal;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.truffle.RubyContext;
import org.jruby.truffle.core.Layouts;
import org.jruby.truffle.language.RubyGuards;
import org.jruby.truffle.language.RubyNode;
import org.jruby.truffle.language.SnippetNode;
import org.jruby.truffle.language.dispatch.CallDispatchHeadNode;

import java.math.BigDecimal;
import java.math.RoundingMode;

@NodeChildren({
        @NodeChild(value = "value", type = RubyNode.class),
        @NodeChild(value = "roundingMode", type = RubyNode.class)
})
@ImportStatic(BigDecimalCoreMethodNode.class)
public abstract class BigDecimalCastNode extends RubyNode {
    public BigDecimalCastNode(RubyContext context, SourceSection sourceSection) {
        super(context, sourceSection);
    }

    public abstract BigDecimal executeBigDecimal(VirtualFrame frame, Object value, RoundingMode roundingMode);

    public abstract Object executeObject(VirtualFrame frame, Object value, RoundingMode roundingMode);

    @Specialization
    public BigDecimal doInt(long value, Object roundingMode) {
        return BigDecimal.valueOf(value);
    }

    @TruffleBoundary
    @Specialization
    public BigDecimal doDouble(double value, Object roundingMode) {
        return BigDecimal.valueOf(value);
    }

    @Specialization(guards = "isRubyBignum(value)")
    public BigDecimal doBignum(DynamicObject value, Object roundingMode) {
        return new BigDecimal(Layouts.BIGNUM.getValue(value));
    }

    @Specialization(guards = "isNormalRubyBigDecimal(value)")
    public BigDecimal doBigDecimal(DynamicObject value, Object roundingMode) {
        return Layouts.BIG_DECIMAL.getValue(value);
    }

    @Specialization(guards = {
            "!isRubyBignum(value)",
            "!isRubyBigDecimal(value)"
    })
    public Object doOther(
            VirtualFrame frame,
            DynamicObject value,
            Object roundingMode,
            @Cached("new()") SnippetNode isRationalSnippet,
            @Cached("createMethodCall()") CallDispatchHeadNode numeratorCallNode,
            @Cached("createMethodCall()") CallDispatchHeadNode denominatorCallNode,
            @Cached("createMethodCall()") CallDispatchHeadNode toFCallNode) {
        if (roundingMode instanceof RoundingMode && (boolean) isRationalSnippet.execute(frame, "value.is_a?(Rational)", "value", value)) {
            final Object numerator = numeratorCallNode.call(frame, value, "numerator", null);
            final Object denominator = denominatorCallNode.call(frame, value, "denominator", null);

            final RubyRational rubyRationalValue = RubyRational.newRationalRaw(
                    getContext().getJRubyRuntime(),
                    toJRubyInteger(numerator),
                    toJRubyInteger(denominator));

            try {
                return RubyBigDecimal
                        .getVpRubyObjectWithPrec19Inner(
                                getContext().getJRubyRuntime().getCurrentContext(),
                                rubyRationalValue,
                                (RoundingMode) roundingMode)
                        .getBigDecimalValue();
            } catch (Exception e) {
                throw e;
            }
        } else {
            final Object result = toFCallNode.call(frame, value, "to_f", null);

            if (result != nil()) {
                return new BigDecimal((double) result);
            } else {
                return result;
            }
        }
    }

    @TruffleBoundary
    private IRubyObject toJRubyInteger(Object value) {
        if (value instanceof Integer) {
            return RubyFixnum.newFixnum(getContext().getJRubyRuntime(), (int) value);
        } else if (value instanceof Long) {
            return RubyFixnum.newFixnum(getContext().getJRubyRuntime(), (long) value);
        } else if (RubyGuards.isRubyBignum(value)) {
            return RubyBignum.newBignum(getContext().getJRubyRuntime(), Layouts.BIGNUM.getValue((DynamicObject) value));
        } else {
            throw new UnsupportedOperationException(value.toString());
        }
    }

    @Fallback
    public Object doBigDecimalFallback(Object value, Object roundingMode) {
        // TODO (pitr 22-Jun-2015): How to better communicate failure without throwing
        return nil();
    }

}
