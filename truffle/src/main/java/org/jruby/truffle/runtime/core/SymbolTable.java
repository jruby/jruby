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
import org.jcodings.specific.USASCIIEncoding;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.layouts.Layouts;
import org.jruby.truffle.runtime.rope.Rope;
import org.jruby.truffle.runtime.rope.LeafRope;
import org.jruby.truffle.runtime.rope.RopeOperations;
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
    private final WeakHashMap<Rope, WeakReference<DynamicObject>> symbolsTable = new WeakHashMap<>();

    public SymbolTable(RubyContext context) {
        this.context = context;
    }

    @CompilerDirectives.TruffleBoundary
    public DynamicObject getSymbol(String string) {
        return getSymbol(RopeOperations.create(ByteList.encode(string, "ISO-8859-1"), USASCIIEncoding.INSTANCE, StringSupport.CR_UNKNOWN));
    }

    @CompilerDirectives.TruffleBoundary
    public DynamicObject getSymbol(ByteList bytes) {
        return getSymbol(StringOperations.ropeFromByteList(bytes));
    }

    @CompilerDirectives.TruffleBoundary
    public synchronized DynamicObject getSymbol(Rope rope) {
        lock.readLock().lock();

        try {
            final WeakReference<DynamicObject> symbolReference = symbolsTable.get(rope);

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
            final WeakReference<DynamicObject> symbolReference = symbolsTable.get(rope);

            if (symbolReference != null) {
                final DynamicObject symbol = symbolReference.get();

                if (symbol != null) {
                    return symbol;
                }
            }

            final DynamicObject symbolClass = context.getCoreLibrary().getSymbolClass();
            final Rope flattenedRope = RopeOperations.flatten(rope);
            final String string = ByteList.decode(flattenedRope.getBytes(), flattenedRope.begin(), flattenedRope.byteLength(), "ISO-8859-1");

            final DynamicObject newSymbol = Layouts.SYMBOL.createSymbol(
                    Layouts.CLASS.getInstanceFactory(symbolClass),
                    string,
                    flattenedRope,
                    string.hashCode(),
                    null);

            symbolsTable.put(flattenedRope, new WeakReference<>(newSymbol));
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
