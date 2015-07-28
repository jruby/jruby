/*
 * Copyright (c) 2013, 2015 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.nodes.core;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.object.*;
import com.oracle.truffle.api.source.SourceSection;
import org.jruby.truffle.nodes.RubyRootNode;
import org.jruby.truffle.nodes.arguments.CheckArityNode;
import org.jruby.truffle.nodes.control.SequenceNode;
import org.jruby.truffle.nodes.methods.SymbolProcNode;
import org.jruby.truffle.om.dsl.api.*;
import org.jruby.truffle.runtime.RubyCallStack;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.core.RubyBasicObject;
import org.jruby.truffle.runtime.core.SymbolCodeRangeableWrapper;
import org.jruby.truffle.runtime.methods.Arity;
import org.jruby.truffle.runtime.methods.SharedMethodInfo;
import org.jruby.util.ByteList;

@CoreClass(name = "Symbol")
public abstract class SymbolNodes {

    @org.jruby.truffle.om.dsl.api.Layout
    public interface SymbolLayout extends BasicObjectNodes.BasicObjectLayout {

        DynamicObject createSymbol(RubyBasicObject logicalClass, RubyBasicObject metaClass, String string, ByteList byteList, int hashCode,
                                   int codeRange, @Nullable SymbolCodeRangeableWrapper codeRangeableWrapper);

        boolean isSymbol(DynamicObject object);

        String getString(DynamicObject object);
        ByteList getByteList(DynamicObject object);
        int getHashCode(DynamicObject object);

        int getCodeRange(DynamicObject object);
        void setCodeRange(DynamicObject object, int codeRange);

        @Nullable
        SymbolCodeRangeableWrapper getCodeRangeableWrapper(DynamicObject object);

        @Nullable
        void setCodeRangeableWrapper(DynamicObject object, SymbolCodeRangeableWrapper codeRangeableWrapper);

    }

    public static final SymbolLayout SYMBOL_LAYOUT = SymbolLayoutImpl.INSTANCE;

    public static String getString(RubyBasicObject symbol) {
        return SYMBOL_LAYOUT.getString(BasicObjectNodes.getDynamicObject(symbol));
    }

    public static ByteList getByteList(RubyBasicObject symbol) {
        return SYMBOL_LAYOUT.getByteList(BasicObjectNodes.getDynamicObject(symbol));
    }

    public static int getHashCode(RubyBasicObject symbol) {
        return SYMBOL_LAYOUT.getHashCode(BasicObjectNodes.getDynamicObject(symbol));
    }

    public static int getCodeRange(RubyBasicObject symbol) {
        return SYMBOL_LAYOUT.getCodeRange(BasicObjectNodes.getDynamicObject(symbol));
    }

    public static void setCodeRange(RubyBasicObject symbol, int codeRange) {
        SYMBOL_LAYOUT.setCodeRange(BasicObjectNodes.getDynamicObject(symbol), codeRange);
    }

    public static SymbolCodeRangeableWrapper getCodeRangeable(RubyBasicObject symbol) {
        SymbolCodeRangeableWrapper wrapper = SYMBOL_LAYOUT.getCodeRangeableWrapper(BasicObjectNodes.getDynamicObject(symbol));

        if (wrapper != null) {
            return wrapper;
        }

        wrapper = new SymbolCodeRangeableWrapper(symbol);

        SYMBOL_LAYOUT.setCodeRangeableWrapper(BasicObjectNodes.getDynamicObject(symbol), wrapper);

        return wrapper;
    }

    @CoreMethod(names = "all_symbols", onSingleton = true)
    public abstract static class AllSymbolsNode extends CoreMethodArrayArgumentsNode {

        public AllSymbolsNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @TruffleBoundary
        @Specialization
        public RubyBasicObject allSymbols() {
            return createArray(getContext().getSymbolTable().allSymbols().toArray());
        }

    }

    @CoreMethod(names = { "==", "eql?" }, required = 1)
    public abstract static class EqualNode extends BinaryCoreMethodNode {

        public EqualNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization(guards = "isRubySymbol(b)")
        public boolean equal(RubyBasicObject a, RubyBasicObject b) {
            return a == b;
        }

        @Specialization(guards = "!isRubySymbol(b)")
        public boolean equal(VirtualFrame frame, RubyBasicObject a, Object b) {
            return false;
        }

    }

    @CoreMethod(names = "encoding")
    public abstract static class EncodingNode extends CoreMethodArrayArgumentsNode {

        public EncodingNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public RubyBasicObject encoding(RubyBasicObject symbol) {
            return EncodingNodes.getEncoding(getByteList(symbol).getEncoding());
        }

    }

    @CoreMethod(names = "hash")
    public abstract static class HashNode extends CoreMethodArrayArgumentsNode {

        public HashNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public int hash(RubyBasicObject symbol) {
            return getHashCode(symbol);
        }

    }

    @CoreMethod(names = "to_proc")
    public abstract static class ToProcNode extends CoreMethodArrayArgumentsNode {

        public ToProcNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization(guards = "cachedSymbol == symbol")
        public RubyBasicObject toProcCached(RubyBasicObject symbol,
                                     @Cached("symbol") RubyBasicObject cachedSymbol,
                                     @Cached("createProc(symbol)") RubyBasicObject cachedProc) {
            return cachedProc;
        }

        @TruffleBoundary
        @Specialization
        public RubyBasicObject toProcUncached(RubyBasicObject symbol) {
            return createProc(symbol);
        }

        protected RubyBasicObject createProc(RubyBasicObject symbol) {
            final SourceSection sourceSection = RubyCallStack.getCallerFrame(getContext())
                    .getCallNode().getEncapsulatingSourceSection();

            final SharedMethodInfo sharedMethodInfo = new SharedMethodInfo(
                    sourceSection, null, Arity.NO_ARGUMENTS, getString(symbol),
                    true, null, false);

            final RubyRootNode rootNode = new RubyRootNode(
                    getContext(), sourceSection,
                    new FrameDescriptor(),
                    sharedMethodInfo,
                    SequenceNode.sequence(getContext(), sourceSection,
                            new CheckArityNode(getContext(), sourceSection, Arity.AT_LEAST_ONE),
                            new SymbolProcNode(getContext(), sourceSection, getString(symbol))));

            final CallTarget callTarget = Truffle.getRuntime().createCallTarget(rootNode);

            return ProcNodes.createRubyProc(
                    getContext().getCoreLibrary().getProcClass(),
                    ProcNodes.Type.PROC,
                    sharedMethodInfo,
                    callTarget, callTarget, callTarget,
                    null, null,
                    BasicObjectNodes.getContext(symbol).getCoreLibrary().getNilObject(),
                    null);
        }

    }

    @CoreMethod(names = "to_s")
    public abstract static class ToSNode extends CoreMethodArrayArgumentsNode {

        public ToSNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public RubyBasicObject toS(RubyBasicObject symbol) {
            return createString(getByteList(symbol).dup());
        }

    }

}
