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

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.dsl.GenerateNodeFactory;
import com.oracle.truffle.api.dsl.TypeSystemReference;
import com.oracle.truffle.api.frame.MaterializedFrame;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.instrument.ProbeNode;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.UnexpectedResultException;
import com.oracle.truffle.api.source.SourceSection;
import jnr.posix.POSIX;
import org.jruby.truffle.nodes.dispatch.DispatchAction;
import org.jruby.truffle.nodes.instrument.RubyWrapperNode;
import org.jruby.truffle.nodes.yield.YieldDispatchNode;
import org.jruby.truffle.runtime.LexicalScope;
import org.jruby.truffle.runtime.RubyArguments;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.UndefinedPlaceholder;
import org.jruby.truffle.runtime.core.*;
import org.jruby.truffle.runtime.rubinius.RubiniusByteArray;

public abstract class RubyNode extends Node {

    private final RubyContext context;

    // This field is a hack, used to transmit the information
    // supplied by the JRuby parser in the form of a special
    // node in the parse tree. The right thing to do is to
    // add a special information node when the AST is constructed,
    // which can then be removed.
    private boolean atNewline = false;

    public RubyNode(RubyContext context, SourceSection sourceSection) {
        super(sourceSection);
        assert context != null;
        this.context = context;
    }

    // Fundamental execute method

    public abstract Object execute(VirtualFrame frame);

    // Execute without returing the result

    public void executeVoid(VirtualFrame frame) {
        execute(frame);
    }

    // Utility methods to execute and expect a particular type

    public UndefinedPlaceholder executeUndefinedPlaceholder(VirtualFrame frame) throws UnexpectedResultException {
        final Object value = execute(frame);

        if (value instanceof UndefinedPlaceholder) {
            return (UndefinedPlaceholder) value;
        } else {
            throw new UnexpectedResultException(value);
        }
    }

    public boolean executeBoolean(VirtualFrame frame) throws UnexpectedResultException {
        final Object value = execute(frame);

        if (value instanceof Boolean) {
            return (boolean) value;
        } else {
            throw new UnexpectedResultException(value);
        }
    }

    public int executeInteger(VirtualFrame frame) throws UnexpectedResultException {
        final Object value = execute(frame);

        if (value instanceof Integer) {
            return (int) value;
        } else {
            throw new UnexpectedResultException(value);
        }
    }

    public long executeLong(VirtualFrame frame) throws UnexpectedResultException {
        final Object value = execute(frame);

        if (value instanceof Long) {
            return (long) value;
        } else {
            throw new UnexpectedResultException(value);
        }
    }

    public RubyBignum executeRubyBignum(VirtualFrame frame) throws UnexpectedResultException {
        final Object value = execute(frame);

        if (value instanceof RubyBignum) {
            return (RubyBignum) value;
        } else {
            throw new UnexpectedResultException(value);
        }
    }

    public RubyRange.IntegerFixnumRange executeIntegerFixnumRange(VirtualFrame frame) throws UnexpectedResultException {
        final Object value = execute(frame);

        if (value instanceof RubyRange.IntegerFixnumRange) {
            return (RubyRange.IntegerFixnumRange) value;
        } else {
            throw new UnexpectedResultException(value);
        }
    }

    public RubyRange.LongFixnumRange executeLongFixnumRange(VirtualFrame frame) throws UnexpectedResultException {
        final Object value = execute(frame);

        if (value instanceof RubyRange.LongFixnumRange) {
            return (RubyRange.LongFixnumRange) value;
        } else {
            throw new UnexpectedResultException(value);
        }
    }

    public double executeDouble(VirtualFrame frame) throws UnexpectedResultException {
        final Object value = execute(frame);

        if (value instanceof Double) {
            return (double) value;
        } else {
            throw new UnexpectedResultException(value);
        }
    }

    public RubyString executeRubyString(VirtualFrame frame) throws UnexpectedResultException {
        final Object value = execute(frame);

        if (value instanceof RubyString) {
            return (RubyString) value;
        } else {
            throw new UnexpectedResultException(value);
        }
    }

    public RubyArray executeRubyArray(VirtualFrame frame) throws UnexpectedResultException {
        final Object value = execute(frame);

        if (value instanceof RubyArray) {
            return (RubyArray) value;
        } else {
            throw new UnexpectedResultException(value);
        }
    }

    public RubyHash executeRubyHash(VirtualFrame frame) throws UnexpectedResultException {
        final Object value = execute(frame);

        if (value instanceof RubyHash) {
            return (RubyHash) value;
        } else {
            throw new UnexpectedResultException(value);
        }
    }

    public RubyRegexp executeRubyRegexp(VirtualFrame frame) throws UnexpectedResultException {
        final Object value = execute(frame);

        if (value instanceof RubyRegexp) {
            return (RubyRegexp) value;
        } else {
            throw new UnexpectedResultException(value);
        }
    }

    public RubyModule executeRubyModule(VirtualFrame frame) throws UnexpectedResultException {
        final Object value = execute(frame);

        if (value instanceof RubyModule) {
            return (RubyModule) value;
        } else {
            throw new UnexpectedResultException(value);
        }
    }

    public RubyProc executeRubyProc(VirtualFrame frame) throws UnexpectedResultException {
        final Object value = execute(frame);

        if (value instanceof RubyProc) {
            return (RubyProc) value;
        } else {
            throw new UnexpectedResultException(value);
        }
    }

    public RubyBasicObject executeRubyBasicObject(VirtualFrame frame) throws UnexpectedResultException {
        final Object value = execute(frame);

        if (value instanceof RubyBasicObject) {
            return (RubyBasicObject) value;
        } else {
            throw new UnexpectedResultException(value);
        }
    }

    public Object[] executeObjectArray(VirtualFrame frame) throws UnexpectedResultException {
        final Object value = execute(frame);

        if (value instanceof Object[]) {
            return (Object[]) value;
        } else {
            throw new UnexpectedResultException(value);
        }
    }

    @Override
    public boolean isInstrumentable() {
        return true;
    }

    @Override
    public ProbeNode.WrapperNode createWrapperNode() {
        return new RubyWrapperNode(this);
    }

    /**
     * Records that this node was wrapped by the JRuby parser with a "newline" node.
     */
    public void setAtNewline() {
        atNewline = true;
    }

    /**
     * Was this ndoe wrapped by a JRuby parser "newline" node?
     */
    public boolean isAtNewline() {
        return atNewline;
    }

    /**
     * Ruby's parallel semantic path.
     * 
     * @see DefinedNode
     */
    public Object isDefined(VirtualFrame frame) {
        return getContext().makeString("expression");
    }

    public RubyNode getNonProxyNode() {
        return this;
    }

    public RubyContext getContext() {
        return context;
    }

    public static void notDesignedForCompilation() {
        CompilerDirectives.bailout("this code either doesn't implement Ruby semantics properly, or is a basic implementation that will not compile");
    }

    public boolean isTrue(boolean value) {
        return value;
    }

    public RubyNode getNonWrapperNode() {
        return this;
    }

    public boolean isRational(RubyBasicObject o) {
        // TODO(CS, 10-Jan-15) should this be a full is_a? test? We'd need a node for that.
        return o.getLogicalClass() == getContext().getCoreLibrary().getRationalClass();
    }

    public boolean isForeignObject(Object object) {
        return (object instanceof TruffleObject) && !(isRubyBasicObject(object));
    }

    public boolean isComplex(RubyBasicObject o) {
        // TODO(BF, 4-4-15) COPIED from isRational - should this be a full is_a? test? We'd need a node for that.
        return o.getLogicalClass() == getContext().getCoreLibrary().getComplexClass();
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
    public boolean isJavaObjectArray(Object value) {
        return value instanceof Object[];
    }

    public boolean isRubyNilObject(Object value) {
        return value == nil();
    }

    public boolean isRubiniusUndefined(Object value) {
        return value == getContext().getCoreLibrary().getRubiniusUndefined();
    }

    protected Object ruby(VirtualFrame frame, String expression, Object... arguments) {
        return rubyWithSelf(frame, RubyArguments.getSelf(frame.getArguments()), expression, arguments);
    }

    protected Object rubyWithSelf(VirtualFrame frame, Object self, String expression, Object... arguments) {
        final MaterializedFrame evalFrame = setupFrame(RubyArguments.getSelf(frame.getArguments()), arguments);

        final RubyBinding binding = new RubyBinding(
                getContext().getCoreLibrary().getBindingClass(),
                self,
                evalFrame);

        return getContext().eval(expression, binding, true, "inline-ruby", this);
    }

    @CompilerDirectives.TruffleBoundary
    private MaterializedFrame setupFrame(Object self, Object... arguments) {
        final MaterializedFrame evalFrame = Truffle.getRuntime().createMaterializedFrame(
                RubyArguments.pack(null, null, self, null, new Object[]{}));

        if (arguments.length % 2 == 1) {
            throw new UnsupportedOperationException("odd number of name-value pairs for arguments");
        }

        for (int n = 0; n < arguments.length; n += 2) {
            evalFrame.setObject(evalFrame.getFrameDescriptor().findOrAddFrameSlot(arguments[n]), arguments[n + 1]);
        }

        return evalFrame;
    }

    protected RubyNilClass nil() {
        return getContext().getCoreLibrary().getNilObject();
    }

    protected POSIX posix() {
        return getContext().getPosix();
    }
}
