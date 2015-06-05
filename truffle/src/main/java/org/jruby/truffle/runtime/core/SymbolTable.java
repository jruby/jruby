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
import org.jcodings.specific.ASCIIEncoding;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.util.ByteList;

import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;

public class SymbolTable {

    private final RubyContext context;

    private final ConcurrentHashMap<ByteList, RubySymbol> symbolsTable = new ConcurrentHashMap<>();

    public SymbolTable(RubyContext context) {
        this.context = context;
    }

    @CompilerDirectives.TruffleBoundary
    public RubySymbol getSymbol(String string) {
        return getSymbol(ByteList.create(string));
    }

    @CompilerDirectives.TruffleBoundary
    public RubySymbol getSymbol(ByteList bytes) {
        RubySymbol symbol = symbolsTable.get(bytes);

        if (symbol == null) {
            final ByteList storedBytes = bytes.dup();

            final RubySymbol newSymbol = new RubySymbol(context.getCoreLibrary().getSymbolClass(), bytes.toString(), storedBytes);

            final RubySymbol existingSymbol = symbolsTable.putIfAbsent(storedBytes, newSymbol);

            if (existingSymbol != null) {
                symbol = existingSymbol;
            } else {
                symbol = newSymbol;
            }
        }

        return symbol;

    }

    @CompilerDirectives.TruffleBoundary
    public Collection<RubySymbol> allSymbols() {
        return symbolsTable.values();
    }

}
