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

import com.oracle.truffle.api.CompilerDirectives;
import org.jcodings.Encoding;
import org.jcodings.specific.ASCIIEncoding;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.util.ByteList;

import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;

public class SymbolTable {

    private final ConcurrentHashMap<ByteList, RubySymbol> symbolsTable = new ConcurrentHashMap<>();
    private final RubyContext context;

    public SymbolTable(RubyContext context) {
        this.context = context;
    }

    @CompilerDirectives.TruffleBoundary
    public RubySymbol getSymbol(String name) {
        return getSymbol(name, ASCIIEncoding.INSTANCE);
    }

    @CompilerDirectives.TruffleBoundary
    public RubySymbol getSymbol(String name, Encoding encoding) {
        final ByteList byteList = org.jruby.RubySymbol.symbolBytesFromString(context.getRuntime(), name);
        byteList.setEncoding(encoding);

        RubySymbol symbol = symbolsTable.get(byteList);

        if (symbol == null) {
            symbol = createSymbol(name, byteList);
        }
        return symbol;
    }

    @CompilerDirectives.TruffleBoundary
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

    @CompilerDirectives.TruffleBoundary
    public Collection<RubySymbol> allSymbols() {
        return symbolsTable.values();
    }
}
