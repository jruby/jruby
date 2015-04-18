/*
 * Copyright (c) 2013, 2015 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.nodes;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.dsl.TypeSystemReference;
import com.oracle.truffle.api.frame.MaterializedFrame;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.instrument.Probe;
import com.oracle.truffle.api.instrument.ProbeNode;
import com.oracle.truffle.api.instrument.TruffleEventReceiver;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.UnexpectedResultException;
import com.oracle.truffle.api.source.SourceSection;

import org.jruby.truffle.nodes.dispatch.DispatchAction;
import org.jruby.truffle.nodes.instrument.RubyWrapperNode;
import org.jruby.truffle.nodes.yield.YieldDispatchNode;
import org.jruby.truffle.runtime.LexicalScope;
import org.jruby.truffle.runtime.RubyArguments;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.UndefinedPlaceholder;
import org.jruby.truffle.runtime.core.*;
import org.jruby.truffle.runtime.rubinius.RubiniusByteArray;
import org.jruby.util.ByteList;

import java.math.BigInteger;

/**
 * Base class for most nodes in Ruby.
 *
 * @see YieldDispatchNode
 */
@TypeSystemReference(RubyTypes.class)
public abstract class RubyNode extends Node implements ProbeNode.Instrumentable {

    private final RubyContext context;

    public RubyNode(RubyContext context, SourceSection sourceSection) {
        super(sourceSection);
        assert context != null;
        this.context = context;
    }

    public RubyNode(RubyNode prev) {
        this(prev.context, prev.getSourceSection());
    }

    public abstract Object execute(VirtualFrame frame);

    /**
     * Ruby's parallel semantic path.
     * 
     * @see DefinedNode
     */
    public Object isDefined(VirtualFrame frame) {
        return getContext().makeString("expression");
    }

    public String executeString(VirtualFrame frame) throws UnexpectedResultException {
        return RubyTypesGen.RUBYTYPES.expectString(execute(frame));
    }

    public RubyArray executeArray(VirtualFrame frame) throws UnexpectedResultException {
        return RubyTypesGen.RUBYTYPES.expectRubyArray(execute(frame));
    }

    public RubyBignum executeBignum(VirtualFrame frame) throws UnexpectedResultException {
        return RubyTypesGen.RUBYTYPES.expectRubyBignum(execute(frame));
    }

    public boolean executeBoolean(VirtualFrame frame) throws UnexpectedResultException {
        return RubyTypesGen.RUBYTYPES.expectBoolean(execute(frame));
    }

    public int executeIntegerFixnum(VirtualFrame frame) throws UnexpectedResultException {
        return RubyTypesGen.RUBYTYPES.expectInteger(execute(frame));
    }

    public long executeLongFixnum(VirtualFrame frame) throws UnexpectedResultException {
        return RubyTypesGen.RUBYTYPES.expectLong(execute(frame));
    }

    public RubyRange.IntegerFixnumRange executeIntegerFixnumRange(VirtualFrame frame) throws UnexpectedResultException {
        return RubyTypesGen.RUBYTYPES.expectIntegerFixnumRange(execute(frame));
    }

    public RubyRange.LongFixnumRange executeLongFixnumRange(VirtualFrame frame) throws UnexpectedResultException {
        return RubyTypesGen.RUBYTYPES.expectLongFixnumRange(execute(frame));
    }

    public double executeFloat(VirtualFrame frame) throws UnexpectedResultException {
        return RubyTypesGen.RUBYTYPES.expectDouble(execute(frame));
    }

    public Object[] executeObjectArray(VirtualFrame frame) throws UnexpectedResultException {
        return RubyTypesGen.RUBYTYPES.expectObjectArray(execute(frame));
    }

    public RubyRange.ObjectRange executeObjectRange(VirtualFrame frame) throws UnexpectedResultException {
        return RubyTypesGen.RUBYTYPES.expectObjectRange(execute(frame));
    }

    public RubyBasicObject executeRubyBasicObject(VirtualFrame frame) throws UnexpectedResultException {
        return RubyTypesGen.RUBYTYPES.expectRubyBasicObject(execute(frame));
    }

    public RubyBinding executeRubyBinding(VirtualFrame frame) throws UnexpectedResultException {
        return RubyTypesGen.RUBYTYPES.expectRubyBinding(execute(frame));
    }

    public RubyClass executeRubyClass(VirtualFrame frame) throws UnexpectedResultException {
        return RubyTypesGen.RUBYTYPES.expectRubyClass(execute(frame));
    }

    public RubyException executeRubyException(VirtualFrame frame) throws UnexpectedResultException {
        return RubyTypesGen.RUBYTYPES.expectRubyException(execute(frame));
    }

    public RubyFiber executeRubyFiber(VirtualFrame frame) throws UnexpectedResultException {
        return RubyTypesGen.RUBYTYPES.expectRubyFiber(execute(frame));
    }

    public RubyHash executeRubyHash(VirtualFrame frame) throws UnexpectedResultException {
        return RubyTypesGen.RUBYTYPES.expectRubyHash(execute(frame));
    }

    public RubyMatchData executeRubyMatchData(VirtualFrame frame) throws UnexpectedResultException {
        return RubyTypesGen.RUBYTYPES.expectRubyMatchData(execute(frame));
    }

    public RubyModule executeRubyModule(VirtualFrame frame) throws UnexpectedResultException {
        return RubyTypesGen.RUBYTYPES.expectRubyModule(execute(frame));
    }

    public RubyNilClass executeRubyNilClass(VirtualFrame frame) throws UnexpectedResultException {
        return RubyTypesGen.RUBYTYPES.expectRubyNilClass(execute(frame));
    }

    public RubyProc executeRubyProc(VirtualFrame frame) throws UnexpectedResultException {
        return RubyTypesGen.RUBYTYPES.expectRubyProc(execute(frame));
    }

    public RubyRange executeRubyRange(VirtualFrame frame) throws UnexpectedResultException {
        return RubyTypesGen.RUBYTYPES.expectRubyRange(execute(frame));
    }

    public RubyRegexp executeRubyRegexp(VirtualFrame frame) throws UnexpectedResultException {
        return RubyTypesGen.RUBYTYPES.expectRubyRegexp(execute(frame));
    }

    public RubySymbol executeRubySymbol(VirtualFrame frame) throws UnexpectedResultException {
        return RubyTypesGen.RUBYTYPES.expectRubySymbol(execute(frame));
    }

    public RubyThread executeRubyThread(VirtualFrame frame) throws UnexpectedResultException {
        return RubyTypesGen.RUBYTYPES.expectRubyThread(execute(frame));
    }

    public RubyTime executeRubyTime(VirtualFrame frame) throws UnexpectedResultException {
        return RubyTypesGen.RUBYTYPES.expectRubyTime(execute(frame));
    }

    public RubyString executeRubyString(VirtualFrame frame) throws UnexpectedResultException {
        return RubyTypesGen.RUBYTYPES.expectRubyString(execute(frame));
    }
    public RubyEncoding executeRubyEncoding(VirtualFrame frame) throws UnexpectedResultException {
        return RubyTypesGen.RUBYTYPES.expectRubyEncoding(execute(frame));
    }

    public UndefinedPlaceholder executeUndefinedPlaceholder(VirtualFrame frame) throws UnexpectedResultException {
        return RubyTypesGen.RUBYTYPES.expectUndefinedPlaceholder(execute(frame));
    }

    public RubyEncodingConverter executeRubyEncodingConverter(VirtualFrame frame) throws UnexpectedResultException {
        return RubyTypesGen.RUBYTYPES.expectRubyEncodingConverter(execute(frame));
    }

    public RubyMethod executeRubyMethod(VirtualFrame frame) throws UnexpectedResultException {
        return RubyTypesGen.RUBYTYPES.expectRubyMethod(execute(frame));
    }

    public RubyUnboundMethod executeRubyUnboundMethod(VirtualFrame frame) throws UnexpectedResultException {
        return RubyTypesGen.RUBYTYPES.expectRubyUnboundMethod(execute(frame));
    }

    public RubiniusByteArray executeRubiniusByteArray(VirtualFrame frame) throws UnexpectedResultException {
        return RubyTypesGen.RUBYTYPES.expectRubiniusByteArray(execute(frame));
    }

    public void executeVoid(VirtualFrame frame) {
        execute(frame);
    }

    public RubyNode getNonProxyNode() {
        return this;
    }

    public RubyContext getContext() {
        return context;
    }

    public static void notDesignedForCompilation() {
        CompilerAsserts.neverPartOfCompilation();
    }

    public boolean isTrue(boolean value) {
        return value;
    }

    public RubyNode getNonWrapperNode() {
        return this;
    }

    public Probe probe() {
        final Node parent = getParent();

        if (parent == null) {
            throw new IllegalStateException("Cannot call probe() on a node without a parent.");
        }

        if (parent instanceof RubyWrapperNode) {
            return ((RubyWrapperNode) parent).getProbe();
        }

        // Create a new wrapper/probe with this node as its child.
        final RubyWrapperNode wrapper = new RubyWrapperNode(this);

        // Connect it to a Probe
        final Probe probe = ProbeNode.insertProbe(wrapper);

        // Replace this node in the AST with the wrapper
        this.replace(wrapper);

        return probe;
    }

    public void probeLite(TruffleEventReceiver eventReceiver) {
        final Node parent = getParent();

        if (parent == null) {
            throw new IllegalStateException("Cannot call probeLite() on a node without a parent");
        }

        if (parent instanceof RubyWrapperNode) {
            throw new IllegalStateException("Cannot call probeLite() on a node that already has a wrapper.");
        }

        final RubyWrapperNode wrapper = new RubyWrapperNode(this);
        ProbeNode.insertProbeLite(wrapper, eventReceiver);

        this.replace(wrapper);
    }

    public boolean isNaN(double value) {
        return Double.isNaN(value);
    }

    public boolean isInfinity(double value) {
        return Double.isInfinite(value);
    }

    // Copied from RubyTypesGen

    @SuppressWarnings("static-method")
    public boolean isDispatchAction(Object value) {
        return value instanceof DispatchAction;
    }

    @SuppressWarnings("static-method")
    public boolean isLexicalScope(Object value) {
        return value instanceof LexicalScope;
    }

    @SuppressWarnings("static-method")
    public boolean isUndefinedPlaceholder(Object value) {
        return value instanceof UndefinedPlaceholder;
    }

    @SuppressWarnings("static-method")
    public boolean isBoolean(Object value) {
        return value instanceof Boolean;
    }

    @SuppressWarnings("static-method")
    public boolean isInteger(Object value) {
        return value instanceof Integer;
    }

    @SuppressWarnings("static-method")
    public boolean isLong(Object value) {
        return value instanceof Long;
    }

    @SuppressWarnings("static-method")
    public boolean isDouble(Object value) {
        return value instanceof Double;
    }

    @SuppressWarnings("static-method")
    public boolean isString(Object value) {
        return value instanceof String;
    }

    @SuppressWarnings("static-method")
    public boolean isRubyBignum(Object value) {
        return value instanceof RubyBignum;
    }

    @SuppressWarnings("static-method")
    public boolean isIntegerFixnumRange(Object value) {
        return value instanceof RubyRange.IntegerFixnumRange;
    }

    @SuppressWarnings("static-method")
    public boolean isLongFixnumRange(Object value) {
        return value instanceof RubyRange.LongFixnumRange;
    }

    @SuppressWarnings("static-method")
    public boolean isObjectRange(Object value) {
        return value instanceof RubyRange.ObjectRange;
    }

    @SuppressWarnings("static-method")
    public boolean isRubyArray(Object value) {
        return value instanceof RubyArray;
    }

    @SuppressWarnings("static-method")
    public boolean isRubyBinding(Object value) {
        return value instanceof RubyBinding;
    }

    @SuppressWarnings("static-method")
    public boolean isRubyClass(Object value) {
        return value instanceof RubyClass;
    }

    @SuppressWarnings("static-method")
    public boolean isRubyException(Object value) {
        return value instanceof RubyException;
    }

    @SuppressWarnings("static-method")
    public boolean isRubyFiber(Object value) {
        return value instanceof RubyFiber;
    }

    @SuppressWarnings("static-method")
    public boolean isRubyHash(Object value) {
        return value instanceof RubyHash;
    }

    @SuppressWarnings("static-method")
    public boolean isRubyMatchData(Object value) {
        return value instanceof RubyMatchData;
    }

    @SuppressWarnings("static-method")
    public boolean isRubyModule(Object value) {
        return value instanceof RubyModule;
    }

    @SuppressWarnings("static-method")
    public boolean isRubyNilClass(Object value) {
        return value instanceof RubyNilClass;
    }

    @SuppressWarnings("static-method")
    public boolean isRubyProc(Object value) {
        return value instanceof RubyProc;
    }

    @SuppressWarnings("static-method")
    public boolean isRubyRange(Object value) {
        return value instanceof RubyRange;
    }

    @SuppressWarnings("static-method")
    public boolean isRubyRegexp(Object value) {
        return value instanceof RubyRegexp;
    }

    @SuppressWarnings("static-method")
    public boolean isRubyString(Object value) {
        return value instanceof RubyString;
    }

    @SuppressWarnings("static-method")
    public boolean isRubyEncoding(Object value) {
        return value instanceof RubyEncoding;
    }

    @SuppressWarnings("static-method")
    public boolean isRubySymbol(Object value) {
        return value instanceof RubySymbol;
    }

    @SuppressWarnings("static-method")
    public boolean isRubyThread(Object value) {
        return value instanceof RubyThread;
    }

    @SuppressWarnings("static-method")
    public boolean isRubyTime(Object value) {
        return value instanceof RubyTime;
    }

    @SuppressWarnings("static-method")
    public boolean isRubyEncodingConverter(Object value) {
        return value instanceof RubyEncodingConverter;
    }

    @SuppressWarnings("static-method")
    public boolean isRubyMethod(Object value) {
        return value instanceof RubyMethod;
    }

    @SuppressWarnings("static-method")
    public boolean isRubyUnboundMethod(Object value) {
        return value instanceof RubyUnboundMethod;
    }

    @SuppressWarnings("static-method")
    public boolean isRubyBasicObject(Object value) {
        return value instanceof RubyBasicObject;
    }

    @SuppressWarnings("static-method")
    public boolean isThreadLocal(Object value) {
        return value instanceof ThreadLocal;
    }

    @SuppressWarnings("static-method")
    public boolean isObjectArray(Object value) {
        return value instanceof Object[];
    }

    public boolean isRubyNilObject(Object value) {
        return value == nil();
    }

    public boolean isRubiniusUndefined(Object value) {
        return value == getContext().getCoreLibrary().getRubiniusUndefined();
    }

    protected Object ruby(VirtualFrame frame, String expression, Object... arguments) {
        notDesignedForCompilation();
        
        final MaterializedFrame evalFrame = Truffle.getRuntime().createMaterializedFrame(
                RubyArguments.pack(null, null, RubyArguments.getSelf(frame.getArguments()), null, new Object[]{}));

        if (arguments.length % 2 == 1) {
            throw new UnsupportedOperationException("odd number of name-value pairs for arguments");
        }

        for (int n = 0; n < arguments.length; n += 2) {
            evalFrame.setObject(evalFrame.getFrameDescriptor().findOrAddFrameSlot(arguments[n]), arguments[n + 1]);
        }

        final RubyBinding binding = new RubyBinding(
                getContext().getCoreLibrary().getBindingClass(),
                RubyArguments.getSelf(frame.getArguments()),
                evalFrame);

        return getContext().eval(ByteList.create(expression), binding, true, "inline-ruby", this);
    }

    protected RubyNilClass nil() {
        return getContext().getCoreLibrary().getNilObject();
    }

}
