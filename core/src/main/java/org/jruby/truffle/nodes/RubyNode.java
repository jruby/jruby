/*
 * Copyright (c) 2013, 2014 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.nodes;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.source.SourceSection;
import com.oracle.truffle.api.dsl.TypeSystemReference;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.UnexpectedResultException;
import org.jruby.truffle.nodes.dispatch.Dispatch;
import org.jruby.truffle.nodes.yield.YieldDispatchNode;
import org.jruby.truffle.runtime.*;
import org.jruby.truffle.runtime.core.*;
import org.jruby.truffle.runtime.core.RubyArray;
import org.jruby.truffle.runtime.core.RubyHash;
import org.jruby.truffle.runtime.core.RubyRange;
import org.jruby.truffle.runtime.core.RubyBasicObject;
import org.jruby.truffle.runtime.rubinius.RubiniusByteArray;
import org.jruby.truffle.runtime.rubinius.RubiniusChannel;

import java.math.BigInteger;

/**
 * Base class for most nodes in Ruby.
 *
 * @see YieldDispatchNode
 */
@TypeSystemReference(RubyTypes.class)
public abstract class RubyNode extends Node {

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

    public String executeJavaString(VirtualFrame frame) throws UnexpectedResultException {
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

    public RubyFile executeRubyFile(VirtualFrame frame) throws UnexpectedResultException {
        return RubyTypesGen.RUBYTYPES.expectRubyFile(execute(frame));
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

    public RubyString executeString(VirtualFrame frame) throws UnexpectedResultException {
        return RubyTypesGen.RUBYTYPES.expectRubyString(execute(frame));
    }
    public RubyEncoding executeRubyEncoding(VirtualFrame frame) throws UnexpectedResultException {
        return RubyTypesGen.RUBYTYPES.expectRubyEncoding(execute(frame));
    }

    public UndefinedPlaceholder executeUndefinedPlaceholder(VirtualFrame frame) throws UnexpectedResultException {
        return RubyTypesGen.RUBYTYPES.expectUndefinedPlaceholder(execute(frame));
    }

    public RubiniusByteArray executeRubiniusByteArray(VirtualFrame frame) throws UnexpectedResultException {
        return RubyTypesGen.RUBYTYPES.expectRubiniusByteArray(execute(frame));
    }

    public RubiniusChannel executeRubiniusChannel(VirtualFrame frame) throws UnexpectedResultException {
        return RubyTypesGen.RUBYTYPES.expectRubiniusChannel(execute(frame));
    }

    public RubyEncodingConverter executeRubyEncodingConverter(VirtualFrame frame) throws UnexpectedResultException {
        return RubyTypesGen.RUBYTYPES.expectRubyEncodingConverter(execute(frame));
    }

    public Dispatch.DispatchAction executeDispatchAction(VirtualFrame frame) {
        throw new UnsupportedOperationException();
    }

    public LexicalScope executeLexicalScope(VirtualFrame frame) {
        throw new UnsupportedOperationException();
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

    public static boolean isNil(Object o) {
        return o instanceof RubyNilClass;
    }

    public static boolean isTrue(boolean b) {
        return b;
    }

    public static boolean isModule(RubyBasicObject o) {
        return o instanceof RubyModule;
    }

    public static boolean isArray(Object o) {
        return o instanceof RubyArray;
    }

    public static boolean isFixnum(Object o) {
        return o instanceof Integer || o instanceof Long;
    }

    public static boolean isBignum(Object o) {
        return o instanceof RubyBignum;
    }

    public static boolean isFloat(Object o) {
        return o instanceof Double;
    }

    public static boolean isFirstFixnum(Object o) {
        return o instanceof Integer || o instanceof Long;
    }

    public static boolean isFirstBignum(Object o) {
        return o instanceof RubyBignum;
    }

    public static boolean isFirstFloat(Object o) {
        return o instanceof Double;
    }

    public static boolean isSecondFixnum(Object a, Object b) {
        return b instanceof Integer || b instanceof Long;
    }

    public static boolean isSecondBignum(Object a, Object b) {
        return b instanceof RubyBignum;
    }

    public static boolean isSecondFloat(Object a, Object b) {
        return b instanceof Double;
    }

    public static boolean isString(Object o) {
        return o instanceof RubyString;
    }

    public RubyBignum bignum(int value) {
        return bignum((long) value);
    }

    public RubyBignum bignum(long value) {
        return bignum(BigInteger.valueOf(value));
    }

    public RubyBignum bignum(BigInteger value) {
        return new RubyBignum(getContext().getCoreLibrary().getBignumClass(), value);
    }

}
