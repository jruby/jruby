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
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.util.ByteList;

import java.lang.ref.WeakReference;
import java.util.*;

public class SymbolTable {

    private final RubyContext context;

    private final Map<ByteList, WeakReference<RubyBasicObject>> symbolsTable
            = Collections.synchronizedMap(new WeakHashMap<ByteList, WeakReference<RubyBasicObject>>());

    public SymbolTable(RubyContext context) {
        this.context = context;
    }

    @CompilerDirectives.TruffleBoundary
    public RubyBasicObject getSymbol(String string) {
        return getSymbol(ByteList.create(string));
    }

    @CompilerDirectives.TruffleBoundary
    public RubyBasicObject getSymbol(ByteList bytes) {
        final WeakReference<RubyBasicObject> symbolReference = symbolsTable.get(bytes);

        if (symbolReference != null) {
            final RubyBasicObject symbol = symbolReference.get();

            if (symbol != null) {
                return symbol;
            }
        }

        final ByteList storedBytes = bytes.dup();

        final RubyBasicObject newSymbol = new RubySymbol(context.getCoreLibrary().getSymbolClass(), bytes.toString(), storedBytes);

        final WeakReference<RubyBasicObject> interleavedSymbolReference
                = symbolsTable.putIfAbsent(storedBytes, new WeakReference<>(newSymbol));

        if (interleavedSymbolReference != null) {
            return interleavedSymbolReference.get();
        }

        return newSymbol;
    }

    @CompilerDirectives.TruffleBoundary
    public Collection<RubyBasicObject> allSymbols() {
        final Collection<WeakReference<RubyBasicObject>> symbolReferences = symbolsTable.values();

        final Collection<RubyBasicObject> symbols = new ArrayList<>(symbolReferences.size());

        for (WeakReference<RubyBasicObject> reference : symbolReferences) {
            final RubyBasicObject symbol = reference.get();

            if (symbol != null) {
                symbols.add(symbol);
            }
        }

        return symbols;
    }

}
