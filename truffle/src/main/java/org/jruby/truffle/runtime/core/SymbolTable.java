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
import com.oracle.truffle.api.object.DynamicObject;
import org.jruby.truffle.nodes.core.ModuleNodes;
import org.jruby.truffle.nodes.core.SymbolNodes;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.util.ByteList;
import org.jruby.util.StringSupport;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collection;
import java.util.WeakHashMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class SymbolTable {

    private final RubyContext context;

    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    private final WeakHashMap<ByteList, WeakReference<DynamicObject>> symbolsTable = new WeakHashMap<>();

    public SymbolTable(RubyContext context) {
        this.context = context;
    }

    @CompilerDirectives.TruffleBoundary
    public DynamicObject getSymbol(String string) {
        return getSymbol(ByteList.create(string));
    }

    @CompilerDirectives.TruffleBoundary
    public DynamicObject getSymbol(ByteList bytes) {
        lock.readLock().lock();

        try {
            final WeakReference<DynamicObject> symbolReference = symbolsTable.get(bytes);

            if (symbolReference != null) {
                final DynamicObject symbol = symbolReference.get();

                if (symbol != null) {
                    return symbol;
                }
            }
        } finally {
            lock.readLock().unlock();
        }

        lock.writeLock().lock();

        try {
            final WeakReference<DynamicObject> symbolReference = symbolsTable.get(bytes);

            if (symbolReference != null) {
                final DynamicObject symbol = symbolReference.get();

                if (symbol != null) {
                    return symbol;
                }
            }

            final ByteList storedBytes = bytes.dup();

            final DynamicObject symbolClass = context.getCoreLibrary().getSymbolClass();

            final DynamicObject newSymbol = SymbolNodes.SYMBOL_LAYOUT.createSymbol(
                    ModuleNodes.getModel(symbolClass).factory,
                    storedBytes.toString(), storedBytes,
                    storedBytes.toString().hashCode(),
                    StringSupport.CR_UNKNOWN, null);

            symbolsTable.put(storedBytes, new WeakReference<>(newSymbol));
            return newSymbol;
        } finally {
            lock.writeLock().unlock();
        }
    }

    @CompilerDirectives.TruffleBoundary
    public Collection<DynamicObject> allSymbols() {
        final Collection<WeakReference<DynamicObject>> symbolReferences;

        lock.readLock().lock();

        try {
            symbolReferences = symbolsTable.values();
        } finally {
            lock.readLock().unlock();
        }

        final Collection<DynamicObject> symbols = new ArrayList<>(symbolReferences.size());

        for (WeakReference<DynamicObject> reference : symbolReferences) {
            final DynamicObject symbol = reference.get();

            if (symbol != null) {
                symbols.add(symbol);
            }
        }

        return symbols;
    }

}
