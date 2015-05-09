/*
 * Copyright (c) 2015 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.pack.nodes.type;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.NodeChildren;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.utilities.ConditionProfile;
import org.jruby.truffle.nodes.RubyNode;
import org.jruby.truffle.nodes.core.KernelNodes;
import org.jruby.truffle.nodes.core.KernelNodesFactory;
import org.jruby.truffle.nodes.dispatch.CallDispatchHeadNode;
import org.jruby.truffle.nodes.dispatch.DispatchHeadNodeFactory;
import org.jruby.truffle.nodes.dispatch.DispatchNode;
import org.jruby.truffle.nodes.dispatch.MissingBehavior;
import org.jruby.truffle.nodes.objects.IsTaintedNode;
import org.jruby.truffle.nodes.objects.IsTaintedNodeGen;
import org.jruby.truffle.pack.nodes.PackNode;
import org.jruby.truffle.pack.runtime.exceptions.NoImplicitConversionException;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.core.RubyArray;
import org.jruby.truffle.runtime.core.RubyNilClass;
import org.jruby.truffle.runtime.core.RubyString;
import org.jruby.util.ByteList;

import java.nio.charset.StandardCharsets;

/**
 * Convert a value to a string.
 */
@NodeChildren({
        @NodeChild(value = "value", type = PackNode.class),
})
public abstract class ToStringNode extends PackNode {

    private final RubyContext context;
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
        this.context = context;
        this.convertNumbersToStrings = convertNumbersToStrings;
        this.conversionMethod = conversionMethod;
        this.inspectOnConversionFailure = inspectOnConversionFailure;
        this.valueOnNil = valueOnNil;
        isTaintedNode = IsTaintedNodeGen.create(context, getEncapsulatingSourceSection(), null);
    }

    public abstract Object executeToString(VirtualFrame frame, Object object);

    @Specialization
    public Object toString(VirtualFrame frame, RubyNilClass nil) {
        return valueOnNil;
    }

    // TODO CS 31-Mar-15 these boundaries and slow versions are not ideal

    @CompilerDirectives.TruffleBoundary
    @Specialization(guards = "convertNumbersToStrings")
    public ByteList toString(int value) {
        return new ByteList(Integer.toString(value).getBytes(StandardCharsets.US_ASCII));
    }

    @CompilerDirectives.TruffleBoundary
    @Specialization(guards = "convertNumbersToStrings")
    public ByteList toString(long value) {
        return new ByteList(Long.toString(value).getBytes(StandardCharsets.US_ASCII));
    }

    @CompilerDirectives.TruffleBoundary
    @Specialization(guards = "convertNumbersToStrings")
    public ByteList toString(double value) {
        return new ByteList(Double.toString(value).getBytes(StandardCharsets.US_ASCII));
    }

    @Specialization
    public ByteList toString(VirtualFrame frame, RubyString string) {
        if (taintedProfile.profile(isTaintedNode.executeIsTainted(string))) {
            setTainted(frame);
        }

        return string.getByteList();
    }

    @Specialization
    public ByteList toString(VirtualFrame frame, RubyArray array) {
        if (toSNode == null) {
            CompilerDirectives.transferToInterpreter();
            toSNode = insert(DispatchHeadNodeFactory.createMethodCall(context, true, MissingBehavior.RETURN_MISSING));
        }

        final Object value = toSNode.call(frame, array, "to_s", null);

        if (value instanceof RubyString) {
            if (taintedProfile.profile(isTaintedNode.executeIsTainted(value))) {
                setTainted(frame);
            }

            return ((RubyString) value).getByteList();
        }

        CompilerDirectives.transferToInterpreter();

        if (value == DispatchNode.MISSING) {
            throw new NoImplicitConversionException(array, "String");
        }

        throw new NoImplicitConversionException(array, "String");
    }

    @Specialization(guards = {"!isRubyString(object)", "!isRubyArray(object)"})
    public ByteList toString(VirtualFrame frame, Object object) {
        if (toStrNode == null) {
            CompilerDirectives.transferToInterpreter();
            toStrNode = insert(DispatchHeadNodeFactory.createMethodCall(context, true, MissingBehavior.RETURN_MISSING));
        }

        final Object value = toStrNode.call(frame, object, conversionMethod, null);

        if (value instanceof RubyString) {
            if (taintedProfile.profile(isTaintedNode.executeIsTainted(value))) {
                setTainted(frame);
            }

            return ((RubyString) value).getByteList();
        }

        if (inspectOnConversionFailure) {
            if (inspectNode == null) {
                CompilerDirectives.transferToInterpreter();
                inspectNode = insert(KernelNodesFactory.ToSNodeFactory.create(context,
                        getEncapsulatingSourceSection(), new RubyNode[]{null}));
            }

            return inspectNode.toS(frame, object).getByteList();
        }

        CompilerDirectives.transferToInterpreter();

        if (value == DispatchNode.MISSING) {
            throw new NoImplicitConversionException(object, "String");
        }

        throw new NoImplicitConversionException(object, "String");
    }

}
