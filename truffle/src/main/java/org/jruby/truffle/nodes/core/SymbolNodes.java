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
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.source.SourceSection;
import org.jcodings.Encoding;
import org.jruby.truffle.nodes.RubyGuards;
import org.jruby.truffle.nodes.RubyRootNode;
import org.jruby.truffle.nodes.core.array.ArrayNodes;
import org.jruby.truffle.nodes.methods.SymbolProcNode;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.core.*;
import org.jruby.truffle.runtime.methods.Arity;
import org.jruby.truffle.runtime.methods.SharedMethodInfo;
import org.jruby.util.ByteList;
import org.jruby.util.CodeRangeable;
import org.jruby.util.StringSupport;

@CoreClass(name = "Symbol")
public abstract class SymbolNodes {

    public static ByteList getByteList(RubySymbol symbol) {
        return symbol.bytes;
    }

    public static String getString(RubyBasicObject symbol) {
        assert RubyGuards.isRubySymbol(symbol);
        return ((RubySymbol) symbol).symbol;
    }

    public static int getHashCode(RubyBasicObject symbol) {
        assert RubyGuards.isRubySymbol(symbol);
        return ((RubySymbol) symbol).hashCode;
    }

    public static int getCodeRange(RubySymbol symbol) {
        return symbol.codeRange;
    }

    public static void setCodeRange(RubySymbol symbol, int codeRange) {
        symbol.codeRange = codeRange;
    }

    public static SymbolCodeRangeableWrapper getCodeRangeable(RubySymbol symbol) {
        if (symbol.codeRangeableWrapper == null) {
            symbol.codeRangeableWrapper = new SymbolCodeRangeableWrapper(symbol);
        }

        return symbol.codeRangeableWrapper;
    }

    @CoreMethod(names = "all_symbols", onSingleton = true)
    public abstract static class AllSymbolsNode extends CoreMethodArrayArgumentsNode {

        public AllSymbolsNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @TruffleBoundary
        @Specialization
        public RubyBasicObject allSymbols() {
            final RubyBasicObject array = createEmptyArray();

            for (RubyBasicObject s : getContext().getSymbolTable().allSymbols()) {
                ArrayNodes.slowPush(array, s);
            }
            return array;
        }

    }

    @CoreMethod(names = "encoding")
    public abstract static class EncodingNode extends CoreMethodArrayArgumentsNode {

        public EncodingNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public RubyEncoding encoding(RubySymbol symbol) {
            return RubyEncoding.getEncoding(getByteList(symbol).getEncoding());
        }

    }

    @CoreMethod(names = "hash")
    public abstract static class HashNode extends CoreMethodArrayArgumentsNode {

        public HashNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public int hash(RubySymbol symbol) {
            return getHashCode(symbol);
        }

    }

    @CoreMethod(names = "to_proc")
    public abstract static class ToProcNode extends CoreMethodArrayArgumentsNode {

        public ToProcNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @TruffleBoundary
        @Specialization
        public RubyProc toProc(RubySymbol symbol) {
            // TODO(CS): this should be doing all kinds of caching - or just write in Ruby?

            SourceSection sourceSection = Truffle.getRuntime().getCallerFrame().getCallNode().getEncapsulatingSourceSection();

            final RubyContext context = symbol.getContext();

            final SharedMethodInfo sharedMethodInfo = new SharedMethodInfo(sourceSection, null, Arity.NO_ARGUMENTS, symbol.symbol, true, null, false);

            final RubyRootNode rootNode = new RubyRootNode(context, sourceSection, new FrameDescriptor(), sharedMethodInfo,
                    new SymbolProcNode(context, sourceSection, symbol.symbol));

            final CallTarget callTarget = Truffle.getRuntime().createCallTarget(rootNode);

            return new RubyProc(context.getCoreLibrary().getProcClass(), RubyProc.Type.PROC, sharedMethodInfo, callTarget,
                    callTarget, callTarget, null, null, symbol.getContext().getCoreLibrary().getNilObject(), null);
        }

    }

    @CoreMethod(names = "to_s")
    public abstract static class ToSNode extends CoreMethodArrayArgumentsNode {

        public ToSNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public RubyBasicObject toS(RubySymbol symbol) {
            return createString(getByteList(symbol).dup());
        }

    }

}
