package org.jruby.truffle.stdlib.bigdecimal;

/*
 * Copyright (c) 2013, 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.source.SourceSection;
import org.jruby.truffle.RubyContext;
import org.jruby.truffle.core.CoreMethodNode;
import org.jruby.truffle.core.Layouts;
import org.jruby.truffle.core.cast.IntegerCastNode;
import org.jruby.truffle.core.cast.IntegerCastNodeGen;
import org.jruby.truffle.language.RubyGuards;
import org.jruby.truffle.language.dispatch.CallDispatchHeadNode;
import org.jruby.truffle.language.dispatch.DispatchHeadNodeFactory;

import java.math.BigDecimal;
import java.math.RoundingMode;

public abstract class BigDecimalCoreMethodNode extends CoreMethodNode {

    @Child
    private CreateBigDecimalNode createBigDecimal;
    @Child
    private CallDispatchHeadNode limitCall;
    @Child
    private IntegerCastNode limitIntegerCast;
    @Child
    private CallDispatchHeadNode roundModeCall;
    @Child
    private IntegerCastNode roundModeIntegerCast;

    public BigDecimalCoreMethodNode() {
    }

    public BigDecimalCoreMethodNode(RubyContext context, SourceSection sourceSection) {
        super(context, sourceSection);
    }

    public static boolean isNormal(DynamicObject value) {
        return Layouts.BIG_DECIMAL.getType(value) == BigDecimalType.NORMAL;
    }

    public static boolean isNormalRubyBigDecimal(DynamicObject value) {
        return RubyGuards.isRubyBigDecimal(value) && Layouts.BIG_DECIMAL.getType(value) == BigDecimalType.NORMAL;
    }

    public static boolean isSpecialRubyBigDecimal(DynamicObject value) {
        return RubyGuards.isRubyBigDecimal(value) && Layouts.BIG_DECIMAL.getType(value) != BigDecimalType.NORMAL;
    }

    public static boolean isNormalZero(DynamicObject value) {
        return Layouts.BIG_DECIMAL.getValue(value).compareTo(BigDecimal.ZERO) == 0;
    }

    public static boolean isNan(DynamicObject value) {
        return Layouts.BIG_DECIMAL.getType(value) == BigDecimalType.NAN;
    }

    private void setupCreateBigDecimal() {
        if (createBigDecimal == null) {
            CompilerDirectives.transferToInterpreter();
            createBigDecimal = insert(CreateBigDecimalNodeFactory.create(getContext(), getSourceSection(), null, null, null));
        }
    }

    protected DynamicObject createBigDecimal(VirtualFrame frame, Object value) {
        setupCreateBigDecimal();
        return createBigDecimal.executeCreate(frame, value);
    }

    protected DynamicObject initializeBigDecimal(VirtualFrame frame, Object value, DynamicObject self, Object digits) {
        setupCreateBigDecimal();
        return createBigDecimal.executeInitialize(frame, value, self, digits);
    }

    private void setupLimitCall() {
        if (limitCall == null) {
            CompilerDirectives.transferToInterpreter();
            limitCall = insert(DispatchHeadNodeFactory.createMethodCall(getContext()));
        }
    }

    private void setupLimitIntegerCast() {
        if (limitIntegerCast == null) {
            CompilerDirectives.transferToInterpreter();
            limitIntegerCast = insert(IntegerCastNodeGen.create(getContext(), getSourceSection(), null));
        }
    }

    protected int getLimit(VirtualFrame frame) {
        setupLimitCall();
        setupLimitIntegerCast();

        return limitIntegerCast.executeCastInt(limitCall.call(frame, getBigDecimalClass(), "limit", null));
    }

    private void setupRoundModeCall() {
        if (roundModeCall == null) {
            CompilerDirectives.transferToInterpreter();
            roundModeCall = insert(DispatchHeadNodeFactory.createMethodCall(getContext()));
        }
    }

    private void setupRoundModeIntegerCast() {
        if (roundModeIntegerCast == null) {
            CompilerDirectives.transferToInterpreter();
            roundModeIntegerCast = insert(IntegerCastNodeGen.create(getContext(), getSourceSection(), null));
        }
    }

    protected RoundingMode getRoundMode(VirtualFrame frame) {
        setupRoundModeCall();
        setupRoundModeIntegerCast();

        return BigDecimalNodes.toRoundingMode(roundModeIntegerCast.executeCastInt(
                // TODO (pitr 21-Jun-2015): read the actual constant
                roundModeCall.call(frame, getBigDecimalClass(), "mode", null, 256)));
    }

    protected DynamicObject getBigDecimalClass() {
        return coreLibrary().getBigDecimalClass();
    }
}
