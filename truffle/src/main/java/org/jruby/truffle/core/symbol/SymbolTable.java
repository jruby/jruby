/*
 * Copyright (c) 2013, 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.core.symbol;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.object.DynamicObject;
import org.jcodings.specific.USASCIIEncoding;
import org.jruby.truffle.RubyContext;
import org.jruby.truffle.core.Layouts;
import org.jruby.truffle.core.rope.Rope;
import org.jruby.truffle.core.rope.RopeOperations;
import org.jruby.truffle.core.string.StringOperations;
import org.jruby.truffle.language.control.RaiseException;
import org.jruby.util.ByteList;
import org.jruby.util.IdUtil;

import java.lang.ref.WeakReference;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.WeakHashMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class SymbolTable {

    private final RubyContext context;

    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    private final WeakHashMap<Rope, WeakReference<DynamicObject>> symbolsTable = new WeakHashMap<>();
    private final WeakHashMap<String, WeakReference<DynamicObject>> symbolsTableByString = new WeakHashMap<>();

    public SymbolTable(RubyContext context) {
        this.context = context;
    }

    @CompilerDirectives.TruffleBoundary
    public DynamicObject getSymbol(byte[] bytes) {
        return getSymbol(new String(bytes, StandardCharsets.US_ASCII));
    }

    @CompilerDirectives.TruffleBoundary
    public DynamicObject getSymbol(String string) {
        lock.readLock().lock();

        try {
            final WeakReference<DynamicObject> symbolReference = symbolsTableByString.get(string);

            if (symbolReference != null) {
                final DynamicObject symbol = symbolReference.get();

                if (symbol != null) {
                    return symbol;
                }
            }
        } finally {
            lock.readLock().unlock();
        }

        final Rope rope = StringOperations.createRope(string, USASCIIEncoding.INSTANCE);
        final DynamicObject symbol = getSymbol(rope);

        lock.writeLock().lock();

        try {
            symbolsTableByString.put(string, new WeakReference<>(symbol));

            return symbol;
        } finally {
        lock.writeLock().unlock();
        }
    }

    @CompilerDirectives.TruffleBoundary
    public DynamicObject getSymbol(ByteList bytes) {
        return getSymbol(StringOperations.ropeFromByteList(bytes));
    }

    @CompilerDirectives.TruffleBoundary
    public DynamicObject getSymbol(Rope rope) {
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
                    string.hashCode());

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

    // TODO (eregon, 10/10/2015): this check could be done when a Symbol is created to be much cheaper
    public static String checkInstanceVariableName(RubyContext context, String name, Node currentNode) {
        // if (!IdUtil.isValidInstanceVariableName(name)) {

        // check like Rubinius does for compatibility with their Struct Ruby implementation.
        if (!(name.startsWith("@") && name.length() > 1 && IdUtil.isInitialCharacter(name.charAt(1)))) {
            CompilerDirectives.transferToInterpreter();
            throw new RaiseException(context.getCoreExceptions().nameErrorInstanceNameNotAllowable(name, currentNode));
        }
        return name;
    }

    public static String checkClassVariableName(RubyContext context, String name, Node currentNode) {
        if (!IdUtil.isValidClassVariableName(name)) {
            CompilerDirectives.transferToInterpreter();
            throw new RaiseException(context.getCoreExceptions().nameErrorInstanceNameNotAllowable(name, currentNode));
        }
        return name;
    }

}
