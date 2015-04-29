/*
 * Copyright (c) 2014, 2015 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.runtime.core;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.nodes.Node;
import org.jcodings.Encoding;
import org.jcodings.EncodingDB;
import org.jruby.truffle.nodes.objects.Allocator;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.util.ByteList;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * This is a bridge between JRuby encoding and Truffle encoding
 */
public class RubyEncoding extends RubyBasicObject {

    // Both are mutated only in CoreLibrary.initializeEncodingConstants().
    private static RubyEncoding[] encodingList = new RubyEncoding[EncodingDB.getEncodings().size()];
    private static Map<String, RubyEncoding> lookup = new HashMap<>();

    private final Encoding encoding;
    private final ByteList name;
    private final boolean dummy;

    @TruffleBoundary
    public static synchronized RubyEncoding getEncoding(Encoding encoding) {
        return lookup.get(new String(encoding.getName(), StandardCharsets.UTF_8).toLowerCase(Locale.ENGLISH));
    }

    @TruffleBoundary
    public static RubyEncoding getEncoding(String name) {
        return lookup.get(name.toLowerCase(Locale.ENGLISH));
    }

    public static RubyEncoding getEncoding(int index) {
        return encodingList[index];
    }

    @TruffleBoundary
    public static void storeEncoding(int encodingListIndex, RubyEncoding encoding) {
        encodingList[encodingListIndex] = encoding;
        lookup.put(encoding.getName().toString().toLowerCase(Locale.ENGLISH), encoding);
    }

    @TruffleBoundary
    public static void storeAlias(String aliasName, RubyEncoding encoding) {
        lookup.put(aliasName.toLowerCase(Locale.ENGLISH), encoding);
    }

    public static RubyEncoding newEncoding(RubyClass encodingClass, Encoding encoding, byte[] name, int p, int end, boolean dummy) {
        return new RubyEncoding(encodingClass, encoding, new ByteList(name, p, end), dummy);
    }

    private RubyEncoding(RubyClass encodingClass, Encoding encoding, ByteList name, boolean dummy) {
        super(encodingClass);
        this.encoding = encoding;
        this.name = name;
        this.dummy = dummy;
    }

    public Encoding getEncoding() {
        return encoding;
    }

    public ByteList getName() {
        return name;
    }

    public boolean isDummy() {
        return dummy;
    }

    public static RubyEncoding[] cloneEncodingList() {
        final RubyEncoding[] clone = new RubyEncoding[encodingList.length];

        System.arraycopy(encodingList, 0, clone, 0, encodingList.length);

        return clone;
    }

    public static class EncodingAllocator implements Allocator {

        @Override
        public RubyBasicObject allocate(RubyContext context, RubyClass rubyClass, Node currentNode) {
            throw new UnsupportedOperationException();
        }

    }

    @Override
    public String toString() {
        return getName().toString();
    }

}
