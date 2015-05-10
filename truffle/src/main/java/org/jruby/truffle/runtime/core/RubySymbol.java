/*
 * Copyright (c) 2013, 2015 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.runtime.core;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.source.SourceSection;
import org.jcodings.Encoding;
import org.jcodings.specific.ASCIIEncoding;
import org.jruby.truffle.nodes.RubyRootNode;
import org.jruby.truffle.nodes.methods.SymbolProcNode;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.methods.Arity;
import org.jruby.truffle.runtime.methods.SharedMethodInfo;
import org.jruby.util.ByteList;
import org.jruby.util.CodeRangeable;
import org.jruby.util.StringSupport;

import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Represents the Ruby {@code Symbol} class.
 */
public class RubySymbol extends RubyBasicObject implements CodeRangeable {

    private final String symbol;
    private final ByteList bytes;
    private int codeRange = StringSupport.CR_UNKNOWN;

    private RubySymbol(RubyClass symbolClass, String symbol, ByteList bytes) {
        super(symbolClass);
        this.symbol = symbol;
        this.bytes = bytes;
    }

    public static RubySymbol newSymbol(RubyContext runtime, String name) {
        return runtime.getSymbolTable().getSymbol(name, ASCIIEncoding.INSTANCE);
    }

    public RubyProc toProc(SourceSection sourceSection, final Node currentNode) {
        // TODO(CS): cache this?

        final RubyContext context = getContext();

        final SharedMethodInfo sharedMethodInfo = new SharedMethodInfo(sourceSection, null, Arity.NO_ARGUMENTS, symbol, true, null, false);

        final RubyRootNode rootNode = new RubyRootNode(context, sourceSection, new FrameDescriptor(), sharedMethodInfo,
                new SymbolProcNode(context, sourceSection, symbol));

        final CallTarget callTarget = Truffle.getRuntime().createCallTarget(rootNode);

        return new RubyProc(context.getCoreLibrary().getProcClass(), RubyProc.Type.PROC, sharedMethodInfo, callTarget,
                callTarget, callTarget, null, null, getContext().getCoreLibrary().getNilObject(), null);
    }

    public ByteList getSymbolBytes() {
        return bytes;
    }

    public org.jruby.RubySymbol getJRubySymbol() {
        return getContext().getRuntime().newSymbol(bytes);
    }

    @Override
    public String toString() {
        return symbol;
    }

    public RubyString toRubyString() {
         return getContext().makeString(toString());
    }

    @Override
    public int getCodeRange() {
        return codeRange;
    }

    @Override
    @TruffleBoundary
    public int scanForCodeRange() {
        int cr = getCodeRange();

        if (cr == StringSupport.CR_UNKNOWN) {
            cr = slowCodeRangeScan();
            setCodeRange(cr);
        }

        return cr;
    }

    @Override
    public boolean isCodeRangeValid() {
        return codeRange == StringSupport.CR_VALID;
    }

    @Override
    public final void setCodeRange(int codeRange) {
        this.codeRange = codeRange;
    }

    @Override
    public final void clearCodeRange() {
        codeRange = StringSupport.CR_UNKNOWN;
    }

    @Override
    public final void keepCodeRange() {
        if (getCodeRange() == StringSupport.CR_BROKEN) {
            clearCodeRange();
        }
    }

    @Override
    public final void modify() {
        throw new UnsupportedOperationException();
    }

    @Override
    public final void modify(int length) {
        throw new UnsupportedOperationException();
    }

    @Override
    public final void modifyAndKeepCodeRange() {
        modify();
        keepCodeRange();
    }

    @Override
    public Encoding checkEncoding(CodeRangeable other) {
        // TODO (nirvdrum Jan. 13, 2015): This should check if the encodings are compatible rather than just always succeeding.
        return bytes.getEncoding();
    }

    @Override
    public ByteList getByteList() {
        return bytes;
    }

    @TruffleBoundary
    private int slowCodeRangeScan() {
        return StringSupport.codeRangeScan(bytes.getEncoding(), bytes);
    }

    public static final class SymbolTable {

        private final ConcurrentHashMap<ByteList, RubySymbol> symbolsTable = new ConcurrentHashMap<>();
        private final RubyContext context;

        public SymbolTable(RubyContext context) {
            this.context = context;
        }

        @TruffleBoundary
        public RubySymbol getSymbol(String name) {
            return getSymbol(name, ASCIIEncoding.INSTANCE);
        }

        @TruffleBoundary
        public RubySymbol getSymbol(String name, Encoding encoding) {
            final ByteList byteList = org.jruby.RubySymbol.symbolBytesFromString(context.getRuntime(), name);
            byteList.setEncoding(encoding);

            RubySymbol symbol = symbolsTable.get(byteList);

            if (symbol == null) {
                symbol = createSymbol(name, byteList);
            }
            return symbol;
        }

        @TruffleBoundary
        public RubySymbol getSymbol(ByteList byteList) {
            // TODO(CS): is this broken? ByteList is mutable...

            RubySymbol symbol = symbolsTable.get(byteList);

            if (symbol == null) {
                symbol = createSymbol(byteList.toString(), byteList);
            }
            return symbol;

        }

        private RubySymbol createSymbol(String name, ByteList byteList) {
            RubySymbol symbol = new RubySymbol(context.getCoreLibrary().getSymbolClass(), name, byteList);
            RubySymbol existingSymbol = symbolsTable.putIfAbsent(byteList, symbol);
            return existingSymbol == null ? symbol : existingSymbol;
        }

        @TruffleBoundary
        public Collection<RubySymbol> allSymbols() {
            return symbolsTable.values();
        }
    }

    @Override
    public boolean hasNoSingleton() {
        return true;
    }

}
