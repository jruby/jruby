/*
 * Copyright (c) 2013, 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.core.symbol;

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
import org.jruby.runtime.ArgumentDescriptor;
import org.jruby.truffle.core.BinaryCoreMethodNode;
import org.jruby.truffle.core.CoreClass;
import org.jruby.truffle.core.CoreMethod;
import org.jruby.truffle.core.CoreMethodArrayArgumentsNode;
import org.jruby.truffle.core.Layouts;
import org.jruby.truffle.core.UnaryCoreMethodNode;
import org.jruby.truffle.core.encoding.EncodingNodes;
import org.jruby.truffle.core.proc.ProcOperations;
import org.jruby.truffle.core.proc.ProcType;
import org.jruby.truffle.language.RubyRootNode;
import org.jruby.truffle.language.arguments.RubyArguments;
import org.jruby.truffle.language.control.RaiseException;
import org.jruby.truffle.language.methods.Arity;
import org.jruby.truffle.language.methods.InternalMethod;
import org.jruby.truffle.language.methods.SharedMethodInfo;
import org.jruby.truffle.language.methods.SymbolProcNode;
import org.jruby.truffle.language.parser.jruby.Translator;

import java.util.Arrays;

@CoreClass(name = "Symbol")
public abstract class SymbolNodes {

    @CoreMethod(names = "all_symbols", onSingleton = true)
    public abstract static class AllSymbolsNode extends CoreMethodArrayArgumentsNode {

        @TruffleBoundary
        @Specialization
        public DynamicObject allSymbols() {
            Object[] store = getContext().getSymbolTable().allSymbols().toArray();
            return Layouts.ARRAY.createArray(coreLibrary().getArrayFactory(), store, store.length);
        }

    }

    @CoreMethod(names = { "==", "eql?" }, required = 1)
    public abstract static class EqualNode extends BinaryCoreMethodNode {

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

        @Specialization
        public DynamicObject encoding(DynamicObject symbol) {
            return EncodingNodes.getEncoding(Layouts.SYMBOL.getRope(symbol).getEncoding());
        }

    }

    @CoreMethod(names = "hash")
    public abstract static class HashNode extends CoreMethodArrayArgumentsNode {

        @Specialization
        public int hash(DynamicObject symbol) {
            return Layouts.SYMBOL.getHashCode(symbol);
        }

    }

    @CoreMethod(names = "to_proc")
    public abstract static class ToProcNode extends CoreMethodArrayArgumentsNode {

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
            final SourceSection sourceSection = getContext().getCallStack().getCallerFrameIgnoringSend()
                    .getCallNode().getEncapsulatingSourceSection();

            final SharedMethodInfo sharedMethodInfo = new SharedMethodInfo(
                    sourceSection, null, Arity.AT_LEAST_ONE, Layouts.SYMBOL.getString(symbol),
                    true, ArgumentDescriptor.ANON_REST, false, false, false);

            final RubyRootNode rootNode = new RubyRootNode(getContext(), sourceSection, new FrameDescriptor(nil()), sharedMethodInfo, Translator.sequence(getContext(), sourceSection, Arrays.asList(Translator.createCheckArityNode(getContext(), sourceSection, Arity.AT_LEAST_ONE), new SymbolProcNode(getContext(), sourceSection, Layouts.SYMBOL.getString(symbol)))), false);

            final CallTarget callTarget = Truffle.getRuntime().createCallTarget(rootNode);
            final InternalMethod method = RubyArguments.getMethod(frame);

            return ProcOperations.createRubyProc(
                    coreLibrary().getProcFactory(),
                    ProcType.PROC,
                    sharedMethodInfo,
                    callTarget, callTarget, null,
                    method, coreLibrary().getNilObject(),
                    null);
        }

        protected int getCacheLimit() {
            return getContext().getOptions().SYMBOL_TO_PROC_CACHE;
        }

    }

    @CoreMethod(names = "to_s")
    public abstract static class ToSNode extends CoreMethodArrayArgumentsNode {

        @Specialization
        public DynamicObject toS(DynamicObject symbol) {
            return createString(Layouts.SYMBOL.getRope(symbol));
        }

    }

    @CoreMethod(names = "allocate", constructor = true)
    public abstract static class AllocateNode extends UnaryCoreMethodNode {

        @TruffleBoundary
        @Specialization
        public DynamicObject allocate(DynamicObject rubyClass) {
            throw new RaiseException(coreExceptions().typeErrorAllocatorUndefinedFor(rubyClass, this));
        }

    }

}
