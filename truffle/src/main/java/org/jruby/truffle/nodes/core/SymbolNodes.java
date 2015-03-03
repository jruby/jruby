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

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.source.SourceSection;
import org.jruby.Ruby;
import org.jruby.RubyObject;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.core.RubyArray;
import org.jruby.truffle.runtime.core.RubyEncoding;
import org.jruby.truffle.runtime.core.RubyNilClass;
import org.jruby.truffle.runtime.core.RubyProc;
import org.jruby.truffle.runtime.core.RubyString;
import org.jruby.truffle.runtime.core.RubySymbol;
import org.jruby.util.ByteList;
import org.jruby.util.StringSupport;

@CoreClass(name = "Symbol")
public abstract class SymbolNodes {

    @CoreMethod(names = {"==", "==="}, required = 1)
    public abstract static class EqualNode extends CoreMethodNode {

        public EqualNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public EqualNode(EqualNode prev) {
            super(prev);
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
    public abstract static class CompareNode extends CoreMethodNode {

        public CompareNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public CompareNode(CompareNode prev) {
            super(prev);
        }

        @Specialization
        public int compare(RubySymbol a, RubySymbol b) {
            notDesignedForCompilation();

            return a.getByteList().cmp(b.getByteList());
        }

        @Specialization(guards = "!isRubySymbol(other)")
        public RubyNilClass compare(RubySymbol symbol,  Object other) {
            notDesignedForCompilation();
            return getContext().getCoreLibrary().getNilObject();
        }
    }

    @CoreMethod(names = "all_symbols", onSingleton = true)
    public abstract static class AllSymbolsNode extends CoreMethodNode {

        public AllSymbolsNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public AllSymbolsNode(AllSymbolsNode prev) {
            super(prev);
        }

        @Specialization
        public RubyArray allSymbols() {
            notDesignedForCompilation();

            final RubyArray array = new RubyArray(getContext().getCoreLibrary().getArrayClass());

            for (RubySymbol s : getContext().getSymbolTable().allSymbols()) {
                array.slowPush(s);
            }
            return array;
        }

    }

    @CoreMethod(names = "capitalize")
    public abstract static class CapitalizeNode extends CoreMethodNode {

        public CapitalizeNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public CapitalizeNode(CapitalizeNode prev) {
            super(prev);
        }

        @Specialization
        public RubySymbol capitalize(RubySymbol symbol) {
            notDesignedForCompilation();
            final ByteList byteList = SymbolNodesHelper.capitalize(symbol.getByteList());
            return getContext().newSymbol(byteList);
        }

    }

    @CoreMethod(names = "casecmp", required = 1)
    public abstract static class CaseCompareNode extends CoreMethodNode {

        public CaseCompareNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public CaseCompareNode(CaseCompareNode prev) {
            super(prev);
        }

        @Specialization
        public int caseCompare(RubySymbol symbol, RubySymbol other) {
            notDesignedForCompilation();

            return symbol.getByteList().caseInsensitiveCmp(other.getByteList());
        }

        @Specialization(guards = "!isRubySymbol(other)")
        public RubyNilClass caseCompare(RubySymbol symbol,  Object other) {
            notDesignedForCompilation();
            return getContext().getCoreLibrary().getNilObject();
        }

    }

    @CoreMethod(names = "downcase")
    public abstract static class DowncaseNode extends CoreMethodNode {

        public DowncaseNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public DowncaseNode(DowncaseNode prev) {
            super(prev);
        }

        @Specialization
        public RubySymbol downcase(RubySymbol symbol) {
            notDesignedForCompilation();

            final ByteList byteList = SymbolNodesHelper.downcase(symbol.getByteList());
            return getContext().newSymbol(byteList);
        }

    }

    @CoreMethod(names = "empty?")
    public abstract static class EmptyNode extends CoreMethodNode {

        public EmptyNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public EmptyNode(EmptyNode prev) {
            super(prev);
        }

        @Specialization
        public boolean empty(RubySymbol symbol) {
            notDesignedForCompilation();

            return symbol.toString().isEmpty();
        }

    }

    @CoreMethod(names = "encoding")
    public abstract static class EncodingNode extends CoreMethodNode {

        public EncodingNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public EncodingNode(EncodingNode prev) {
            super(prev);
        }

        @Specialization
        public RubyEncoding encoding(RubySymbol symbol) {
            notDesignedForCompilation();

            return RubyEncoding.getEncoding(symbol.getByteList().getEncoding());
        }

    }

    @CoreMethod(names = "hash")
    public abstract static class HashNode extends CoreMethodNode {

        public HashNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public HashNode(HashNode prev) {
            super(prev);
        }

        @Specialization
        public int hash(RubySymbol symbol) {
            return symbol.toString().hashCode();
        }

    }

    @CoreMethod(names = "intern")
    public abstract static class InternNode extends CoreMethodNode {

        public InternNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public InternNode(InternNode prev) {
            super(prev);
        }

        @Specialization
        public RubySymbol intern(RubySymbol symbol) {
            return symbol;
        }

    }

    @CoreMethod(names = "to_proc")
    public abstract static class ToProcNode extends CoreMethodNode {

        public ToProcNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public ToProcNode(ToProcNode prev) {
            super(prev);
        }

        @Specialization
        public RubyProc toProc(RubySymbol symbol) {
            notDesignedForCompilation();

            // TODO(CS): this should be doing all kinds of caching
            return symbol.toProc(Truffle.getRuntime().getCallerFrame().getCallNode().getEncapsulatingSourceSection(), this);
        }
    }

    @CoreMethod(names = "to_sym")
    public abstract static class ToSymNode extends CoreMethodNode {

        public ToSymNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public ToSymNode(ToSymNode prev) {
            super(prev);
        }

        @Specialization
        public RubySymbol toSym(RubySymbol symbol) {
            return symbol;
        }

    }

    @CoreMethod(names = { "to_s", "id2name" })
    public abstract static class ToSNode extends CoreMethodNode {

        public ToSNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public ToSNode(ToSNode prev) {
            super(prev);
        }

        @Specialization
        public RubyString toS(RubySymbol symbol) {
            notDesignedForCompilation();

            return getContext().makeString(symbol.getSymbolBytes().dup());
        }

    }

    @CoreMethod(names = "inspect")
    public abstract static class InspectNode extends CoreMethodNode {

        public InspectNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public InspectNode(InspectNode prev) {
            super(prev);
        }

        @Specialization
        public RubyString inspect(RubySymbol symbol) {
            notDesignedForCompilation();

            return getContext().makeString(symbol.getJRubySymbol().inspect(getContext().getRuntime().getCurrentContext()).asString().decodeString());
        }

    }

    @CoreMethod(names = {"size", "length"})
    public abstract static class SizeNode extends CoreMethodNode {

        public SizeNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public SizeNode(SizeNode prev) {
            super(prev);
        }

        @Specialization
        public int size(RubySymbol symbol) {
            return symbol.getByteList().lengthEnc();
        }

    }

    @CoreMethod(names = {"succ"})
    public abstract static class SuccNode extends CoreMethodNode {

        public SuccNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public SuccNode(SuccNode prev) {
            super(prev);
        }

        @Specialization
        public RubySymbol succ(RubySymbol symbol) {
            notDesignedForCompilation();
            return getContext().newSymbol(StringSupport.succCommon(symbol.getByteList()));
        }

    }

    @CoreMethod(names = { "swapcase"})
    public abstract static class SwapcaseNode extends CoreMethodNode {

        public SwapcaseNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public SwapcaseNode(SwapcaseNode prev) {
            super(prev);
        }

        @Specialization
        public RubySymbol swapcase(RubySymbol symbol) {
            notDesignedForCompilation();

            final ByteList byteList = SymbolNodesHelper.swapcase(symbol.getByteList());
            return getContext().newSymbol(byteList);
        }

    }

    @CoreMethod(names = "upcase")
    public abstract static class UpcaseNode extends CoreMethodNode {

        public UpcaseNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public UpcaseNode(UpcaseNode prev) {
            super(prev);
        }

        @Specialization
        public RubySymbol upcase(RubySymbol symbol) {
            notDesignedForCompilation();

            final ByteList byteList = SymbolNodesHelper.upcase(symbol.getByteList());
            return getContext().newSymbol(byteList);
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
