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

import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.source.SourceSection;

import org.jruby.truffle.nodes.core.StringNodes.HashNode;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.core.RubyArray;
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

        @Specialization(guards = "!isRubySymbol(arguments[1])")
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

            return a.toString().compareTo(b.toString());
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

    @CoreMethod(names = "size")
    public abstract static class SizeNode extends CoreMethodNode {

        public SizeNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public SizeNode(SizeNode prev) {
            super(prev);
        }

        @Specialization
        public int size(RubySymbol symbol) {
            return StringSupport.strLengthFromRubyString(symbol);
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

            final ByteList byteList = StringNodes.StringNodesHelper.swapcase(symbol.toRubyString());
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

            final ByteList byteList = StringNodes.StringNodesHelper.upcase(symbol.toRubyString());
            return getContext().newSymbol(byteList);
        }

    }

}
