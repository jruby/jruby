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
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.core.RubyArray;
import org.jruby.truffle.runtime.core.RubyProc;
import org.jruby.truffle.runtime.core.RubyString;
import org.jruby.truffle.runtime.core.RubySymbol;

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

        @Specialization
        public boolean equal(RubySymbol a, RubyString b) {
            notDesignedForCompilation();

            return a.toString().equals(b.toString());
        }

        @Specialization
        public boolean equal(RubySymbol a, int b) {
            notDesignedForCompilation();

            return a.toString().equals(Integer.toString(b));
        }

        @Specialization
        public boolean equal(RubySymbol a, long b) {
            notDesignedForCompilation();

            return a.toString().equals(Long.toString(b));
        }

        @Specialization(guards = "notSymbol")
        public boolean equal(RubySymbol a, Object b) {
            return false;
        }

        protected boolean notSymbol(RubySymbol a, Object b) {
            return !(b instanceof RubySymbol);
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

            for (RubySymbol s : getContext().getSymbolTable().getSymbolsTable().values()){
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

    @CoreMethod(names = "to_s")
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

            return getContext().makeString(symbol.getJRubySymbol().to_s().asString().decodeString());
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
        public RubyString toS(RubySymbol symbol) {
            notDesignedForCompilation();

            return getContext().makeString(symbol.getJRubySymbol().inspect(getContext().getRuntime().getCurrentContext()).asString().decodeString());
        }

    }

}
