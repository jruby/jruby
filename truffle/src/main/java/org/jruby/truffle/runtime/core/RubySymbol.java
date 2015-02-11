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
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.source.SourceSection;
import org.jruby.truffle.nodes.RubyNode;
import org.jruby.truffle.nodes.RubyRootNode;
import org.jruby.truffle.nodes.methods.SymbolProcNode;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.methods.SharedMethodInfo;
import org.jruby.util.ByteList;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Represents the Ruby {@code Symbol} class.
 */
public class RubySymbol extends RubyBasicObject {

    private final String symbol;
    private final ByteList symbolBytes;

    private RubySymbol(RubyClass symbolClass, String symbol, ByteList byteList) {
        super(symbolClass);
        this.symbol = symbol;
        this.symbolBytes = byteList;
    }

    public static RubySymbol newSymbol(RubyContext runtime, String name) {
        return runtime.getSymbolTable().getSymbol(name);
    }

    public RubyProc toProc(SourceSection sourceSection, final RubyNode currentNode) {
        // TODO(CS): cache this?

        RubyNode.notDesignedForCompilation();

        final RubyContext context = getContext();

        final SharedMethodInfo sharedMethodInfo = new SharedMethodInfo(sourceSection, null, symbol, true, null, false);

        final RubyRootNode rootNode = new RubyRootNode(context, sourceSection, new FrameDescriptor(), sharedMethodInfo,
                new SymbolProcNode(context, sourceSection, symbol));

        final CallTarget callTarget = Truffle.getRuntime().createCallTarget(rootNode);

        return new RubyProc(context.getCoreLibrary().getProcClass(), RubyProc.Type.PROC, sharedMethodInfo, callTarget,
                callTarget, callTarget, null, null, getContext().getCoreLibrary().getNilObject(), null);
    }

    public ByteList getSymbolBytes() {
        return symbolBytes;
    }

    public org.jruby.RubySymbol getJRubySymbol() {
        RubyNode.notDesignedForCompilation();

        return getContext().getRuntime().newSymbol(symbolBytes);
    }

    @Override
    public int hashCode() {
        return symbol.hashCode();
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        } else if (other instanceof RubySymbol) {
            return symbol == ((RubySymbol) other).symbol;
        } else if (other instanceof RubyString) {
            return other.equals(symbol);
        } else {
            return super.equals(other);
        }
    }

    @Override
    public String toString() {
        return symbol;
    }

    public RubyString toRubyString() {
         return getContext().makeString(toString());
    }

    public static final class SymbolTable {

        private final ConcurrentHashMap<ByteList, RubySymbol> symbolsTable = new ConcurrentHashMap<>();
        private final RubyContext context;

        public SymbolTable(RubyContext context) {
            this.context = context;
        }

        public RubySymbol getSymbol(String name) {
            ByteList byteList = org.jruby.RubySymbol.symbolBytesFromString(context.getRuntime(), name);

            RubySymbol symbol = symbolsTable.get(byteList);

            if (symbol == null) {
                symbol = createSymbol(name);
            }
            return symbol;
        }

        public RubySymbol getSymbol(ByteList byteList) {
            // TODO(CS): is this broken? ByteList is mutable...

            RubySymbol symbol = symbolsTable.get(byteList);

            if (symbol == null) {
                symbol = createSymbol(byteList);
            }
            return symbol;

        }

        private RubySymbol createSymbol(ByteList byteList) {
            RubySymbol symbol = new RubySymbol(context.getCoreLibrary().getSymbolClass(), byteList.toString(), byteList);
            symbolsTable.put(byteList, symbol);
            return symbol;
        }

        private RubySymbol createSymbol(String name) {
            ByteList byteList = org.jruby.RubySymbol.symbolBytesFromString(context.getRuntime(), name);
            RubySymbol symbol = new RubySymbol(context.getCoreLibrary().getSymbolClass(), name, byteList);

            RubySymbol existingSymbol = symbolsTable.putIfAbsent(byteList, symbol);
            return existingSymbol == null ? symbol : existingSymbol;
        }

        public ConcurrentHashMap<ByteList, RubySymbol> getSymbolsTable(){
            return symbolsTable;
        }
    }

    @Override
    public boolean hasNoSingleton() {
        return true;
    }

}
