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
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.source.SourceSection;
import org.jcodings.Encoding;
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

    public static SymbolCodeRangeableWrapper getCodeRangeable(RubySymbol symbol) {
        if (symbol.codeRangeableWrapper == null) {
            symbol.codeRangeableWrapper = new SymbolCodeRangeableWrapper(symbol);
        }

        return symbol.codeRangeableWrapper;
    }

    public static RubyProc toProc(RubySymbol symbol, SourceSection sourceSection, final Node currentNode) {
        // TODO(CS): cache this?

        final RubyContext context = symbol.getContext();

        final SharedMethodInfo sharedMethodInfo = new SharedMethodInfo(sourceSection, null, Arity.NO_ARGUMENTS, symbol.symbol, true, null, false);

        final RubyRootNode rootNode = new RubyRootNode(context, sourceSection, new FrameDescriptor(), sharedMethodInfo,
                new SymbolProcNode(context, sourceSection, symbol.symbol));

        final CallTarget callTarget = Truffle.getRuntime().createCallTarget(rootNode);

        return new RubyProc(context.getCoreLibrary().getProcClass(), RubyProc.Type.PROC, sharedMethodInfo, callTarget,
                callTarget, callTarget, null, null, symbol.getContext().getCoreLibrary().getNilObject(), null);
    }

    public static int getCodeRange(RubySymbol symbol) {
        return symbol.codeRange;
    }

    @TruffleBoundary
    public static int scanForCodeRange(RubySymbol symbol) {
        int cr = getCodeRange(symbol);

        if (cr == StringSupport.CR_UNKNOWN) {
            cr = slowCodeRangeScan(symbol);
            setCodeRange(symbol, cr);
        }

        return cr;
    }

    public static boolean isCodeRangeValid(RubySymbol symbol) {
        return symbol.codeRange == StringSupport.CR_VALID;
    }

    public static void setCodeRange(RubySymbol symbol, int codeRange) {
        symbol.codeRange = codeRange;
    }

    public static void clearCodeRange(RubySymbol symbol) {
        symbol.codeRange = StringSupport.CR_UNKNOWN;
    }

    public static void keepCodeRange(RubySymbol symbol) {
        if (getCodeRange(symbol) == StringSupport.CR_BROKEN) {
            clearCodeRange(symbol);
        }
    }

    public static void modify() {
        throw new UnsupportedOperationException();
    }

    public static void modify(int length) {
        throw new UnsupportedOperationException();
    }

    public static void modifyAndKeepCodeRange(RubySymbol symbol) {
        modify();
        keepCodeRange(symbol);
    }

    public static Encoding checkEncoding(RubySymbol symbol, CodeRangeable other) {
        // TODO (nirvdrum Jan. 13, 2015): This should check if the encodings are compatible rather than just always succeeding.
        return symbol.bytes.getEncoding();
    }

    public static ByteList getByteList(RubySymbol symbol) {
        return symbol.bytes;
    }

    @TruffleBoundary
    private static int slowCodeRangeScan(RubySymbol symbol) {
        return StringSupport.codeRangeScan(symbol.bytes.getEncoding(), symbol.bytes);
    }

    @CoreMethod(names = {"==", "==="}, required = 1)
    public abstract static class EqualNode extends CoreMethodArrayArgumentsNode {

        public EqualNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public boolean equal(RubySymbol a, RubySymbol b) {
            return a == b;
        }


        @Specialization(guards = "!isRubySymbol(b)")
        public boolean equal(RubySymbol a, Object b) {
            return false;
        }

    }

    @CoreMethod(names = "<=>", required = 1)
    public abstract static class CompareNode extends CoreMethodArrayArgumentsNode {

        public CompareNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @TruffleBoundary
        @Specialization
        public int compare(RubySymbol a, RubySymbol b) {
            return getByteList(a).cmp(getByteList(b));
        }

        @Specialization(guards = "!isRubySymbol(other)")
        public RubyBasicObject compare(RubySymbol symbol,  Object other) {
            return nil();
        }
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

            for (RubySymbol s : getContext().getSymbolTable().allSymbols()) {
                ArrayNodes.slowPush(array, s);
            }
            return array;
        }

    }

    @CoreMethod(names = "capitalize")
    public abstract static class CapitalizeNode extends CoreMethodArrayArgumentsNode {

        public CapitalizeNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public RubySymbol capitalize(RubySymbol symbol) {
            final ByteList byteList = SymbolNodesHelper.capitalize(getByteList(symbol));
            return getContext().getSymbol(byteList);
        }

    }

    @CoreMethod(names = "casecmp", required = 1)
    public abstract static class CaseCompareNode extends CoreMethodArrayArgumentsNode {

        public CaseCompareNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @TruffleBoundary
        @Specialization
        public int caseCompare(RubySymbol symbol, RubySymbol other) {
            return getByteList(symbol).caseInsensitiveCmp(getByteList(other));
        }

        @Specialization(guards = "!isRubySymbol(other)")
        public RubyBasicObject caseCompare(RubySymbol symbol,  Object other) {
            return nil();
        }

    }

    @CoreMethod(names = "downcase")
    public abstract static class DowncaseNode extends CoreMethodArrayArgumentsNode {

        public DowncaseNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @TruffleBoundary
        @Specialization
        public RubySymbol downcase(RubySymbol symbol) {
            final ByteList byteList = SymbolNodesHelper.downcase(getByteList(symbol));
            return getContext().getSymbol(byteList);
        }

    }

    @CoreMethod(names = "empty?")
    public abstract static class EmptyNode extends CoreMethodArrayArgumentsNode {

        public EmptyNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public boolean empty(RubySymbol symbol) {
            return symbol.toString().isEmpty();
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
            return symbol.toString().hashCode();
        }

    }

    @CoreMethod(names = "intern")
    public abstract static class InternNode extends CoreMethodArrayArgumentsNode {

        public InternNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public RubySymbol intern(RubySymbol symbol) {
            return symbol;
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
            // TODO(CS): this should be doing all kinds of caching
            return SymbolNodes.toProc(symbol, Truffle.getRuntime().getCallerFrame().getCallNode().getEncapsulatingSourceSection(), this);
        }
    }

    @CoreMethod(names = "to_sym")
    public abstract static class ToSymNode extends CoreMethodArrayArgumentsNode {

        public ToSymNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public RubySymbol toSym(RubySymbol symbol) {
            return symbol;
        }

    }

    @CoreMethod(names = { "to_s", "id2name" })
    public abstract static class ToSNode extends CoreMethodArrayArgumentsNode {

        public ToSNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public RubyBasicObject toS(RubySymbol symbol) {
            return createString(getByteList(symbol).dup());
        }

    }

    @CoreMethod(names = "inspect")
    public abstract static class InspectNode extends CoreMethodArrayArgumentsNode {

        public InspectNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @TruffleBoundary
        @Specialization
        public RubyBasicObject inspect(RubySymbol symbol) {
            return createString(symbol.getContext().toJRuby(symbol).inspect(getContext().getRuntime().getCurrentContext()).asString().decodeString());
        }

    }

    @CoreMethod(names = {"size", "length"})
    public abstract static class SizeNode extends CoreMethodArrayArgumentsNode {

        public SizeNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public int size(RubySymbol symbol) {
            return getByteList(symbol).lengthEnc();
        }

    }

    @CoreMethod(names = { "swapcase"})
    public abstract static class SwapcaseNode extends CoreMethodArrayArgumentsNode {

        public SwapcaseNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public RubySymbol swapcase(RubySymbol symbol) {
            final ByteList byteList = SymbolNodesHelper.swapcase(getByteList(symbol));
            return getContext().getSymbol(byteList);
        }

    }

    @CoreMethod(names = "upcase")
    public abstract static class UpcaseNode extends CoreMethodArrayArgumentsNode {

        public UpcaseNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public RubySymbol upcase(RubySymbol symbol) {
            final ByteList byteList = SymbolNodesHelper.upcase(getByteList(symbol));
            return getContext().getSymbol(byteList);
        }

    }

    public static class SymbolNodesHelper {

        @TruffleBoundary
        public static ByteList downcase(ByteList originalByteList) {
            final ByteList byteList = originalByteList.dup();
            final int length = byteList.length();
            for (int i = 0; i < length; i++) {
                final char c = byteList.charAt(i);
                if ((c >= 'A') && (c <= 'Z')) {
                    byteList.set(i, c ^ 0x20);
                }
            }
            return byteList;
        }

        @TruffleBoundary
        public static ByteList upcase(ByteList originalByteList) {
            final ByteList byteList = originalByteList.dup();
            final int length = byteList.length();
            for (int i = 0; i < length; i++) {
                final char c = byteList.charAt(i);
                if ((c >= 'a') && (c <= 'z')) {
                    byteList.set(i, c & 0x5f);
                }
            }
            return byteList;
        }



        @TruffleBoundary
        public static ByteList swapcase(ByteList originalByteList) {
            final ByteList byteList = originalByteList.dup();
            final int length = byteList.length();
            for (int i = 0; i < length; i++) {
                final char c = byteList.charAt(i);
                if ((c >= 'a') && (c <= 'z')) {
                    byteList.set(i, c & 0x5f);
                } else if ((c >= 'A') && (c <= 'Z')) {
                    byteList.set(i, c ^ 0x20);
                }
            }
            return byteList;
        }

        @TruffleBoundary
        public static ByteList capitalize(ByteList originalByteList) {
            final ByteList byteList = originalByteList.dup();
            final int length = byteList.length();
            if (length > 0) {
                final char c = byteList.charAt(0);
                if ((c >= 'a') && (c <= 'z')) {
                    byteList.set(0, c & 0x5f);
                }
            }
            for (int i = 1; i < length; i++) {
                final char c = byteList.charAt(i);
                if ((c >= 'A') && (c <= 'Z')) {
                    byteList.set(i, c ^ 0x20);
                }
            }
            return byteList;
        }

    }

}
