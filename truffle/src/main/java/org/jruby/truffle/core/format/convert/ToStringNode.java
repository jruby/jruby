/*
 * Copyright (c) 2015, 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.core.format.convert;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.NodeChildren;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.profiles.ConditionProfile;
import org.jruby.truffle.Layouts;
import org.jruby.truffle.RubyContext;
import org.jruby.truffle.core.format.FormatNode;
import org.jruby.truffle.core.format.exceptions.NoImplicitConversionException;
import org.jruby.truffle.core.kernel.KernelNodes;
import org.jruby.truffle.core.kernel.KernelNodesFactory;
import org.jruby.truffle.language.RubyGuards;
import org.jruby.truffle.language.dispatch.CallDispatchHeadNode;
import org.jruby.truffle.language.dispatch.DispatchHeadNodeFactory;
import org.jruby.truffle.language.dispatch.MissingBehavior;
import org.jruby.truffle.language.objects.IsTaintedNode;
import org.jruby.truffle.language.objects.IsTaintedNodeGen;
import org.jruby.truffle.util.DoubleUtils;

import java.nio.charset.StandardCharsets;

@NodeChildren({
        @NodeChild(value = "value", type = FormatNode.class),
})
public abstract class ToStringNode extends FormatNode {

    protected final boolean convertNumbersToStrings;
    private final String conversionMethod;
    private final boolean inspectOnConversionFailure;
    private final Object valueOnNil;

    @Child private CallDispatchHeadNode toStrNode;
    @Child private CallDispatchHeadNode toSNode;
    @Child private KernelNodes.ToSNode inspectNode;
    @Child private IsTaintedNode isTaintedNode;

    private final ConditionProfile taintedProfile = ConditionProfile.createBinaryProfile();

    public ToStringNode(RubyContext context, boolean convertNumbersToStrings,
                        String conversionMethod, boolean inspectOnConversionFailure,
                        Object valueOnNil) {
        super(context);
        this.convertNumbersToStrings = convertNumbersToStrings;
        this.conversionMethod = conversionMethod;
        this.inspectOnConversionFailure = inspectOnConversionFailure;
        this.valueOnNil = valueOnNil;
        isTaintedNode = IsTaintedNodeGen.create(context, null, null);
    }

    public abstract Object executeToString(VirtualFrame frame, Object object);

    @Specialization(guards = "isNil(nil)")
    public Object toStringNil(Object nil) {
        return valueOnNil;
    }

    @TruffleBoundary
    @Specialization(guards = "convertNumbersToStrings")
    public byte[] toString(int value) {
        return Integer.toString(value).getBytes(StandardCharsets.US_ASCII);
    }

    @TruffleBoundary
    @Specialization(guards = "convertNumbersToStrings")
    public byte[] toString(long value) {
        return Long.toString(value).getBytes(StandardCharsets.US_ASCII);
    }

    @TruffleBoundary
    @Specialization(guards = "convertNumbersToStrings")
    public byte[] toString(double value) {
        return DoubleUtils.toString(value).getBytes(StandardCharsets.US_ASCII);
    }

    @Specialization(guards = "isRubyString(string)")
    public byte[] toStringString(VirtualFrame frame, DynamicObject string) {
        if (taintedProfile.profile(isTaintedNode.executeIsTainted(string))) {
            setTainted(frame);
        }

        return Layouts.STRING.getRope(string).getBytes();
    }

    @Specialization(guards = "isRubyArray(array)")
    public byte[] toString(VirtualFrame frame, DynamicObject array) {
        if (toSNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            toSNode = insert(DispatchHeadNodeFactory.createMethodCall(getContext(), true,
                    MissingBehavior.RETURN_MISSING));
        }

        final Object value = toSNode.call(frame, array, "to_s");

        if (RubyGuards.isRubyString(value)) {
            if (taintedProfile.profile(isTaintedNode.executeIsTainted(value))) {
                setTainted(frame);
            }

            return Layouts.STRING.getRope((DynamicObject) value).getBytes();
        } else {
            throw new NoImplicitConversionException(array, "String");
        }
    }

    @Specialization(guards = {"!isRubyString(object)", "!isRubyArray(object)"})
    public byte[] toString(VirtualFrame frame, Object object) {
        if (toStrNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            toStrNode = insert(DispatchHeadNodeFactory.createMethodCall(getContext(), true,
                    MissingBehavior.RETURN_MISSING));
        }

        final Object value = toStrNode.call(frame, object, conversionMethod);

        if (RubyGuards.isRubyString(value)) {
            if (taintedProfile.profile(isTaintedNode.executeIsTainted(value))) {
                setTainted(frame);
            }

            return Layouts.STRING.getRope((DynamicObject) value).getBytes();
        }

        if (inspectOnConversionFailure) {
            if (inspectNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                inspectNode = insert(KernelNodesFactory.ToSNodeFactory.create(getContext(), null, null));
            }

            return Layouts.STRING.getRope(inspectNode.toS(object)).getBytes();
        } else {
            throw new NoImplicitConversionException(object, "String");
        }
    }

}
