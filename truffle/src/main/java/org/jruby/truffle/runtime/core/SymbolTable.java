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

import java.lang.ref.WeakReference;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class SymbolTable {

    private final RubyContext context;

    private final Map<ByteList, WeakReference<RubySymbol>> symbolsTable
            = Collections.synchronizedMap(new WeakHashMap<ByteList, WeakReference<RubySymbol>>());

    public SymbolTable(RubyContext context) {
        this.context = context;
    }

    @CompilerDirectives.TruffleBoundary
    public RubySymbol getSymbol(String string) {
        return getSymbol(ByteList.create(string));
    }

    @CompilerDirectives.TruffleBoundary
    public RubySymbol getSymbol(ByteList bytes) {
        final WeakReference<RubySymbol> symbolReference = symbolsTable.get(bytes);

        if (symbolReference != null) {
            final RubySymbol symbol = symbolReference.get();

            if (symbol != null) {
                return symbol;
            }
        }

        final ByteList storedBytes = bytes.dup();

        final RubySymbol newSymbol = new RubySymbol(context.getCoreLibrary().getSymbolClass(), bytes.toString(), storedBytes);

        final WeakReference<RubySymbol> interleavedSymbolReference
                = symbolsTable.putIfAbsent(storedBytes, new WeakReference<>(newSymbol));

        if (interleavedSymbolReference != null) {
            return interleavedSymbolReference.get();
        }

        return newSymbol;
    }

    @CompilerDirectives.TruffleBoundary
    public Collection<RubySymbol> allSymbols() {
        final Collection<WeakReference<RubySymbol>> symbolReferences = symbolsTable.values();

        final Collection<RubySymbol> symbols = new ArrayList<>(symbolReferences.size());

        for (WeakReference<RubySymbol> reference : symbolReferences) {
            final RubySymbol symbol = reference.get();

            if (symbol != null) {
                symbols.add(symbol);
            }
        }

        return symbols;
    }

}
