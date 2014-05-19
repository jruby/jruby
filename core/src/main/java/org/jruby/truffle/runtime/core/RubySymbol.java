/*
 * Copyright (c) 2013 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.runtime.core;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import com.oracle.truffle.api.*;
import org.jruby.runtime.Visibility;
import org.jruby.truffle.nodes.RubyNode;
import org.jruby.truffle.runtime.*;
import org.jruby.truffle.runtime.methods.*;
import org.jruby.util.ByteList;

/**
 * Represents the Ruby {@code Symbol} class.
 */
public class RubySymbol extends RubyObject {

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

    public RubyProc toProc(SourceSection sourceSection) {
        final RubyContext context = getRubyClass().getContext();

        // TODO(CS): we need a proper method in here
        RubyNode.notDesignedForCompilation();

        final CallTarget callTarget = new CallTarget() {

            @Override
            public Object call(Object... args) {
                RubyNode.notDesignedForCompilation();

                final Object receiver = RubyArguments.getUserArgument(args, 0);
                final Object[] arguments = RubyArguments.extractUserArguments(args);
                final Object[] sendArgs = Arrays.copyOfRange(arguments, 1, arguments.length);
                final RubyBasicObject receiverObject = context.getCoreLibrary().box(receiver);
                return receiverObject.send(symbol, RubyArguments.getBlock(args), sendArgs);
            }

        };

        final SharedMethodInfo sharedMethodInfo = new SharedMethodInfo(sourceSection, symbol, true, null);
        final RubyMethod method = new RubyMethod(sharedMethodInfo, symbol, null, Visibility.PUBLIC, false, callTarget, null, true);
        return new RubyProc(context.getCoreLibrary().getProcClass(), RubyProc.Type.PROC, NilPlaceholder.INSTANCE, null, method);
    }

    public org.jruby.RubySymbol getJRubySymbol() {
        return getRubyClass().getContext().getRuntime().newSymbol(symbolBytes);
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
         return getRubyClass().getContext().makeString(toString());
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

}
