/*
 * Copyright (c) 2013, 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 *
 * Version: EPL 1.0/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Eclipse Public
 * License Version 1.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.eclipse.org/legal/epl-v10.html
 *
 * Software distributed under the License is distributed on an "AS
 * IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * rights and limitations under the License.
 *
 * Copyright (C) 2006 Ola Bini <ola@ologix.com>
 * Copyright (C) 2009 Joseph LaFata <joe@quibb.org>
 *
 * Alternatively, the contents of this file may be used under the terms of
 * either of the GNU General Public License Version 2 or later (the "GPL"),
 * or the GNU Lesser General Public License Version 2.1 or later (the "LGPL"),
 * in which case the provisions of the GPL or the LGPL are applicable instead
 * of those above. If you wish to allow use of your version of this file only
 * under the terms of either the GPL or the LGPL, and not to allow others to
 * use your version of this file under the terms of the EPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the EPL, the GPL or the LGPL.
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
import org.jruby.truffle.Layouts;
import org.jruby.truffle.language.RubyGuards;
import org.jruby.truffle.language.RubyNode;
import org.jruby.truffle.language.SnippetNode;
import org.jruby.truffle.language.dispatch.CallDispatchHeadNode;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

@NodeChildren({
        @NodeChild(value = "value", type = RubyNode.class),
        @NodeChild(value = "roundingMode", type = RubyNode.class)
})
@ImportStatic(BigDecimalCoreMethodNode.class)
public abstract class BigDecimalCastNode extends RubyNode {

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
            final Object numerator = numeratorCallNode.call(frame, value, "numerator");
            final Object denominator = denominatorCallNode.call(frame, value, "denominator");

            try {
                return toBigDecimal(numerator, denominator, (RoundingMode) roundingMode);
            } catch (Exception e) {
                throw e;
            }
        } else {
            final Object result = toFCallNode.call(frame, value, "to_f");

            if (result != nil()) {
                return new BigDecimal((double) result);
            } else {
                return result;
            }
        }
    }

    @TruffleBoundary
    private BigDecimal toBigDecimal(Object numerator, Object denominator, RoundingMode roundingMode) {
        BigDecimal numeratorDecimal = toBigDecimal(numerator);
        BigDecimal denominatorDecimal = toBigDecimal(denominator);

        int len = numeratorDecimal.precision() + denominatorDecimal.precision();
        int pow = len / 4;
        MathContext mathContext = new MathContext((pow + 1) * 4, roundingMode);

        return numeratorDecimal.divide(denominatorDecimal, mathContext);
    }

    @TruffleBoundary
    private BigDecimal toBigDecimal(Object object) {
        if (object instanceof Byte) {
            return BigDecimal.valueOf((byte) object);
        } else if (object instanceof Short) {
            return BigDecimal.valueOf((short) object);
        } else if (object instanceof Integer) {
            return BigDecimal.valueOf((int) object);
        } else if (object instanceof Long) {
            return BigDecimal.valueOf((long) object);
        } else if (object instanceof Float) {
            return BigDecimal.valueOf((float) object);
        } else if (object instanceof Double) {
            return BigDecimal.valueOf((double) object);
        } else if (RubyGuards.isRubyBignum(object)) {
            return BigDecimal.valueOf(Layouts.BIGNUM.getValue((DynamicObject) object).doubleValue());
        } else {
            throw new UnsupportedOperationException();
        }
    }

    @Fallback
    public Object doBigDecimalFallback(Object value, Object roundingMode) {
        // TODO (pitr 22-Jun-2015): How to better communicate failure without throwing
        return nil();
    }

}
