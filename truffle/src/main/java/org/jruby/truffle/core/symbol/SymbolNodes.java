/*
 * Copyright (c) 2013, 2017 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.core.symbol;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.source.SourceSection;
import org.jruby.truffle.Layouts;
import org.jruby.truffle.builtins.CoreClass;
import org.jruby.truffle.builtins.CoreMethod;
import org.jruby.truffle.builtins.CoreMethodArrayArgumentsNode;
import org.jruby.truffle.builtins.UnaryCoreMethodNode;
import org.jruby.truffle.core.proc.ProcOperations;
import org.jruby.truffle.core.proc.ProcType;
import org.jruby.truffle.language.RubyRootNode;
import org.jruby.truffle.language.SourceIndexLength;
import org.jruby.truffle.language.arguments.RubyArguments;
import org.jruby.truffle.language.control.RaiseException;
import org.jruby.truffle.language.methods.Arity;
import org.jruby.truffle.language.methods.InternalMethod;
import org.jruby.truffle.language.methods.SharedMethodInfo;
import org.jruby.truffle.language.methods.SymbolProcNode;
import org.jruby.truffle.parser.ArgumentDescriptor;
import org.jruby.truffle.parser.Translator;

import java.util.Arrays;

@CoreClass("Symbol")
public abstract class SymbolNodes {

    @CoreMethod(names = "all_symbols", onSingleton = true)
    public abstract static class AllSymbolsNode extends CoreMethodArrayArgumentsNode {

        @TruffleBoundary
        @Specialization
        public DynamicObject allSymbols() {
            Object[] store = getContext().getSymbolTable().allSymbols().toArray();
            return createArray(store, store.length);
        }

    }

    @CoreMethod(names = { "==", "eql?" }, required = 1)
    public abstract static class EqualNode extends CoreMethodArrayArgumentsNode {

        @Specialization(guards = "isRubySymbol(b)")
        public boolean equal(DynamicObject a, DynamicObject b) {
            return a == b;
        }

        @Specialization(guards = "!isRubySymbol(b)")
        public boolean equal(VirtualFrame frame, DynamicObject a, Object b) {
            return false;
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
                                     @Cached("createProc(getMethod(frame), symbol)") DynamicObject cachedProc) {
            return cachedProc;
        }

        @Specialization
        public DynamicObject toProcUncached(VirtualFrame frame, DynamicObject symbol) {
            final InternalMethod method = getMethod(frame);
            return createProc(method, symbol);
        }

        @TruffleBoundary
        protected DynamicObject createProc(InternalMethod method, DynamicObject symbol) {
            final SourceSection sourceSection = getContext().getCallStack().getCallerFrameIgnoringSend()
                    .getCallNode().getEncapsulatingSourceSection();
            final SourceIndexLength sourceIndexLength = new SourceIndexLength(sourceSection.getCharIndex(), sourceSection.getCharLength());

            final SharedMethodInfo sharedMethodInfo = new SharedMethodInfo(
                    sourceSection,
                    method.getLexicalScope(),
                    Arity.AT_LEAST_ONE,
                    null,
                    Layouts.SYMBOL.getString(symbol),
                    "proc",
                    ArgumentDescriptor.ANON_REST,
                    false,
                    false,
                    false);

            final RubyRootNode rootNode = new RubyRootNode(getContext(), sourceSection, new FrameDescriptor(nil()), sharedMethodInfo, Translator.sequence(sourceIndexLength, Arrays.asList(Translator.createCheckArityNode(Arity.AT_LEAST_ONE), new SymbolProcNode(Layouts.SYMBOL.getString(symbol)))), false);

            final CallTarget callTarget = Truffle.getRuntime().createCallTarget(rootNode);

            return ProcOperations.createRubyProc(
                    coreLibrary().getProcFactory(),
                    ProcType.PROC,
                    sharedMethodInfo,
                    callTarget, callTarget, null,
                    method, coreLibrary().getNilObject(),
                    null);
        }

        protected InternalMethod getMethod(VirtualFrame frame) {
            return RubyArguments.getMethod(frame);
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
