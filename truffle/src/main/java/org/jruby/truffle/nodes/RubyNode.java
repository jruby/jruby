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

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.TypeSystemReference;
import com.oracle.truffle.api.frame.MaterializedFrame;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.instrument.ProbeNode;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.UnexpectedResultException;
import com.oracle.truffle.api.object.DynamicObjectFactory;
import com.oracle.truffle.api.source.SourceSection;
import jnr.ffi.provider.MemoryManager;
import jnr.posix.POSIX;
import org.jcodings.Encoding;
import org.jruby.truffle.nodes.core.BindingNodes;
import org.jruby.truffle.nodes.core.ClassNodes;
import org.jruby.truffle.nodes.core.StringNodes;
import org.jruby.truffle.nodes.core.array.ArrayNodes;
import org.jruby.truffle.nodes.dispatch.DispatchNode;
import org.jruby.truffle.nodes.instrument.RubyWrapperNode;
import org.jruby.truffle.runtime.NotProvided;
import org.jruby.truffle.runtime.RubyArguments;
import org.jruby.truffle.runtime.RubyContext;
import com.oracle.truffle.api.object.DynamicObject;
import org.jruby.truffle.runtime.sockets.NativeSockets;
import org.jruby.util.ByteList;

import java.nio.ByteBuffer;

@TypeSystemReference(RubyTypes.class)
@ImportStatic(RubyGuards.class)
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

    // Fundamental execute methods

    public abstract Object execute(VirtualFrame frame);

    public Object isDefined(VirtualFrame frame) {
        return StringNodes.createString(getContext().getCoreLibrary().getStringClass(), "expression");
    }

    // Execute without returning the result

    public void executeVoid(VirtualFrame frame) {
        execute(frame);
    }

    // Utility methods to execute and expect a particular type

    public NotProvided executeNotProvided(VirtualFrame frame) throws UnexpectedResultException {
        final Object value = execute(frame);

        if (value instanceof NotProvided) {
            return (NotProvided) value;
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

    public double executeDouble(VirtualFrame frame) throws UnexpectedResultException {
        final Object value = execute(frame);

        if (value instanceof Double) {
            return (double) value;
        } else {
            throw new UnexpectedResultException(value);
        }
    }

    public DynamicObject executeDynamicObject(VirtualFrame frame) throws UnexpectedResultException {
        final Object value = execute(frame);

        if (value instanceof DynamicObject) {
            return (DynamicObject) value;
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

    // Guards which use the context and so can't be static

    protected boolean isNil(Object value) {
        return value == nil();
    }

    protected boolean isRubiniusUndefined(Object value) {
        return value == getContext().getCoreLibrary().getRubiniusUndefined();
    }

    // Helpers methods for terseness

    protected DynamicObject nil() {
        return getContext().getCoreLibrary().getNilObject();
    }

    public DynamicObject getSymbol(String name) {
        return getContext().getSymbol(name);
    }

    public DynamicObject getSymbol(ByteList name) {
        return getContext().getSymbol(name);
    }

    protected DynamicObject createEmptyString() {
        return StringNodes.createEmptyString(getContext().getCoreLibrary().getStringClass());
    }

    protected DynamicObject createString(String string) {
        return StringNodes.createString(getContext().getCoreLibrary().getStringClass(), string);
    }

    protected DynamicObject createString(String string, Encoding encoding) {
        return StringNodes.createString(getContext().getCoreLibrary().getStringClass(), string, encoding);
    }

    protected DynamicObject createString(byte[] bytes) {
        return StringNodes.createString(getContext().getCoreLibrary().getStringClass(), bytes);
    }

    protected DynamicObject createString(ByteBuffer bytes) {
        return StringNodes.createString(getContext().getCoreLibrary().getStringClass(), bytes);
    }

    protected DynamicObject createString(ByteList bytes) {
        return StringNodes.createString(getContext().getCoreLibrary().getStringClass(), bytes);
    }

    protected DynamicObject createEmptyArray() {
        return ArrayNodes.createEmptyArray(getContext().getCoreLibrary().getArrayClass());
    }

    protected DynamicObject createArray(Object... store) {
        return createArray(store, store.length);
    }

    protected DynamicObject createArray(int[] store, int size) {
        return ArrayNodes.createArray(getContext().getCoreLibrary().getArrayClass(), store, size);
    }

    protected DynamicObject createArray(long[] store, int size) {
        return ArrayNodes.createArray(getContext().getCoreLibrary().getArrayClass(), store, size);
    }

    protected DynamicObject createArray(double[] store, int size) {
        return ArrayNodes.createArray(getContext().getCoreLibrary().getArrayClass(), store, size);
    }

    protected DynamicObject createArray(Object[] store, int size) {
        return ArrayNodes.createArray(getContext().getCoreLibrary().getArrayClass(), store, size);
    }

    protected POSIX posix() {
        return getContext().getPosix();
    }

    protected NativeSockets nativeSockets() {
        return getContext().getNativeSockets();
    }

    protected static int getCacheLimit() {
        return DispatchNode.DISPATCH_POLYMORPHIC_MAX;
    }

    // Helper methods for caching

    protected DynamicObjectFactory getInstanceFactory(DynamicObject rubyClass) {
        return ClassNodes.CLASS_LAYOUT.getInstanceFactory(rubyClass);
    }

    // Instrumentation

    @Override
    public boolean isInstrumentable() {
        return true;
    }

    @Override
    public ProbeNode.WrapperNode createWrapperNode() {
        return new RubyWrapperNode(this);
    }

    public void setAtNewline() {
        atNewline = true;
    }

    public boolean isAtNewline() {
        return atNewline;
    }

    public RubyNode getNonProxyNode() {
        return this;
    }

    // Accessors

    public RubyContext getContext() {
        return context;
    }

    public MemoryManager getMemoryManager() {
        return jnr.ffi.Runtime.getSystemRuntime().getMemoryManager();
    }

    // ruby() helper

    protected Object ruby(VirtualFrame frame, String expression, Object... arguments) {
        return rubyWithSelf(frame, RubyArguments.getSelf(frame.getArguments()), expression, arguments);
    }

    protected Object rubyWithSelf(VirtualFrame frame, Object self, String expression, Object... arguments) {
        final MaterializedFrame evalFrame = setupFrame(RubyArguments.getSelf(frame.getArguments()), arguments);

        final DynamicObject binding = BindingNodes.createRubyBinding(
                getContext().getCoreLibrary().getBindingClass(),
                self,
                evalFrame);

        return getContext().eval(expression, binding, true, "inline-ruby", this);
    }

    @TruffleBoundary
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

}
