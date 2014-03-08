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
import com.oracle.truffle.api.frame.*;
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

    public RubyProc toProc() {
        final RubyContext context = getRubyClass().getContext();

        final CallTarget callTarget = new CallTarget() {

            @Override
            public Object call(PackedFrame frame, Arguments args) {
                final RubyArguments rubyArgs = (RubyArguments) args;
                final Object receiver = rubyArgs.getArguments()[0];
                final Object[] sendArgs = Arrays.copyOfRange(rubyArgs.getArguments(), 1, rubyArgs.getArguments().length);
                final RubyBasicObject receiverObject = context.getCoreLibrary().box(receiver);
                return receiverObject.send(symbol, rubyArgs.getBlock(), sendArgs);
            }

        };

        final CallTargetMethodImplementation methodImplementation = new CallTargetMethodImplementation(callTarget, null);
        final RubyMethod method = new RubyMethod(null, null, new UniqueMethodIdentifier(), symbol, null, Visibility.PUBLIC, false, methodImplementation);

        return new RubyProc(context.getCoreLibrary().getProcClass(), RubyProc.Type.PROC, NilPlaceholder.INSTANCE, null, method);
    }

    @Override
    public String toString() {
        return org.jruby.RubyString.newStringShared(getRubyClass().getContext().getRuntime(),
                symbolBytes).decodeString();
    }


    public org.jruby.RubySymbol getJRubySymbol() {
        return getRubyClass().getContext().getRuntime().newSymbol(symbolBytes);
    }

    @Override
    public String inspect() {
        return getJRubySymbol().inspect(getRubyClass().getContext().getRuntime().getCurrentContext()).asString().decodeString();
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
