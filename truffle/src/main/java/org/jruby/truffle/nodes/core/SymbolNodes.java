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
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.source.SourceSection;
import org.jruby.truffle.nodes.RubyRootNode;
import org.jruby.truffle.nodes.arguments.CheckArityNode;
import org.jruby.truffle.nodes.control.SequenceNode;
import org.jruby.truffle.nodes.methods.SymbolProcNode;
import org.jruby.truffle.runtime.RubyArguments;
import org.jruby.truffle.runtime.RubyCallStack;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.control.RaiseException;
import org.jruby.truffle.runtime.core.SymbolCodeRangeableWrapper;
import org.jruby.truffle.runtime.layouts.Layouts;
import org.jruby.truffle.runtime.methods.Arity;
import org.jruby.truffle.runtime.methods.InternalMethod;
import org.jruby.truffle.runtime.methods.SharedMethodInfo;

@CoreClass(name = "Symbol")
public abstract class SymbolNodes {

    public static SymbolCodeRangeableWrapper getCodeRangeable(DynamicObject symbol) {
        SymbolCodeRangeableWrapper wrapper = Layouts.SYMBOL.getCodeRangeableWrapper(symbol);

        if (wrapper != null) {
            return wrapper;
        }

        wrapper = new SymbolCodeRangeableWrapper(symbol);

        Layouts.SYMBOL.setCodeRangeableWrapper(symbol, wrapper);

        return wrapper;
    }

    @CoreMethod(names = "all_symbols", onSingleton = true)
    public abstract static class AllSymbolsNode extends CoreMethodArrayArgumentsNode {

        public AllSymbolsNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @TruffleBoundary
        @Specialization
        public DynamicObject allSymbols() {
            Object[] store = getContext().getSymbolTable().allSymbols().toArray();
            return Layouts.ARRAY.createArray(getContext().getCoreLibrary().getArrayFactory(), store, store.length);
        }

    }

    @CoreMethod(names = { "==", "eql?" }, required = 1)
    public abstract static class EqualNode extends BinaryCoreMethodNode {

        public EqualNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization(guards = "isRubySymbol(b)")
        public boolean equal(DynamicObject a, DynamicObject b) {
            return a == b;
        }

        @Specialization(guards = "!isRubySymbol(b)")
        public boolean equal(VirtualFrame frame, DynamicObject a, Object b) {
            return false;
        }

    }

    @CoreMethod(names = "encoding")
    public abstract static class EncodingNode extends CoreMethodArrayArgumentsNode {

        public EncodingNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public DynamicObject encoding(DynamicObject symbol) {
            return EncodingNodes.getEncoding(Layouts.SYMBOL.getRope(symbol).getEncoding());
        }

    }

    @CoreMethod(names = "hash")
    public abstract static class HashNode extends CoreMethodArrayArgumentsNode {

        public HashNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public int hash(DynamicObject symbol) {
            return Layouts.SYMBOL.getHashCode(symbol);
        }

    }

    @CoreMethod(names = "to_proc")
    public abstract static class ToProcNode extends CoreMethodArrayArgumentsNode {

        public ToProcNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization(guards = "cachedSymbol == symbol", limit = "getCacheLimit()")
        public DynamicObject toProcCached(VirtualFrame frame, DynamicObject symbol,
                                     @Cached("symbol") DynamicObject cachedSymbol,
                                     @Cached("createProc(frame, symbol)") DynamicObject cachedProc) {
            return cachedProc;
        }

        @Specialization
        public DynamicObject toProcUncached(VirtualFrame frame, DynamicObject symbol) {
            return createProc(frame, symbol);
        }

        protected DynamicObject createProc(VirtualFrame frame, DynamicObject symbol) {
            CompilerDirectives.transferToInterpreter();
            final SourceSection sourceSection = RubyCallStack.getCallerFrame(getContext())
                    .getCallNode().getEncapsulatingSourceSection();

            final SharedMethodInfo sharedMethodInfo = new SharedMethodInfo(
                    sourceSection, null, Arity.NO_ARGUMENTS, Layouts.SYMBOL.getString(symbol),
                    true, null, false, false, false);

            final RubyRootNode rootNode = new RubyRootNode(
                    getContext(), sourceSection,
                    new FrameDescriptor(nil()),
                    sharedMethodInfo,
                    SequenceNode.sequence(getContext(), sourceSection,
                            CheckArityNode.create(getContext(), sourceSection, Arity.AT_LEAST_ONE),
                            new SymbolProcNode(getContext(), sourceSection, Layouts.SYMBOL.getString(symbol))));

            final CallTarget callTarget = Truffle.getRuntime().createCallTarget(rootNode);
            final InternalMethod method = RubyArguments.getMethod(frame.getArguments());

            return ProcNodes.createRubyProc(
                    getContext().getCoreLibrary().getProcFactory(),
                    ProcNodes.Type.PROC,
                    sharedMethodInfo,
                    callTarget, callTarget, null,
                    method, getContext().getCoreLibrary().getNilObject(),
                    null);
        }

        protected int getCacheLimit() {
            return getContext().getOptions().SYMBOL_TO_PROC_CACHE;
        }

    }

    @CoreMethod(names = "to_s")
    public abstract static class ToSNode extends CoreMethodArrayArgumentsNode {

        public ToSNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public DynamicObject toS(DynamicObject symbol) {
            return createString(Layouts.SYMBOL.getRope(symbol));
        }

    }

    @CoreMethod(names = "allocate", constructor = true)
    public abstract static class AllocateNode extends UnaryCoreMethodNode {

        public AllocateNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @TruffleBoundary
        @Specialization
        public DynamicObject allocate(DynamicObject rubyClass) {
            throw new RaiseException(getContext().getCoreLibrary().typeErrorAllocatorUndefinedFor(rubyClass, this));
        }

    }

}
