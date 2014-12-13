/*
 * Copyright (c) 2014 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.runtime.core;

import org.jcodings.Encoding;
import org.jcodings.EncodingDB;
import org.jruby.truffle.nodes.RubyNode;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.util.ByteList;

import java.util.HashMap;
import java.util.Map;

/**
 * This is a bridge between JRuby encoding and Truffle encoding
 */
public class RubyEncoding extends RubyBasicObject {

    private static RubyEncoding[] encodingList = new RubyEncoding[EncodingDB.getEncodings().size()];
    private static Map<String, RubyEncoding> lookup = new HashMap<>();

    private final Encoding encoding;
    private final ByteList name;
    private final boolean dummy;

    /**
     * The class from which we create the object that is {@code Encoding}. A subclass of
     * {@link RubyClass} so that we can override {@link RubyClass#newInstance} and allocate a
     * {@link RubyEncoding} rather than a normal {@link RubyBasicObject}.
     */
    public static class RubyEncodingClass extends RubyClass {

        public RubyEncodingClass(RubyContext context, RubyClass objectClass) {
            super(context, objectClass, objectClass, "Encoding");
        }

        @Override
        public RubyBasicObject newInstance(RubyNode currentNode) {
            throw new UnsupportedOperationException();
        }

    }

    public static synchronized RubyEncoding getEncoding(RubyContext context, Encoding encoding) {
        return lookup.get(new String(encoding.getName()).toLowerCase());
    }

    public static RubyEncoding getEncoding(RubyContext context, String name) {
        return lookup.get(name.toLowerCase());
    }

    public static RubyEncoding getEncoding(int index) {
        return encodingList[index];
    }

    public static void storeEncoding(int encodingListIndex, RubyEncoding encoding) {
        encodingList[encodingListIndex] = encoding;
        lookup.put(encoding.getName().toString().toLowerCase(), encoding);
    }

    public static void storeAlias(String aliasName, RubyEncoding encoding) {
        lookup.put(aliasName.toLowerCase(), encoding);
    }

    public static RubyEncoding newEncoding(RubyContext context, Encoding encoding, byte[] name, int p, int end, boolean dummy) {
        return new RubyEncoding(context.getCoreLibrary().getEncodingClass(), encoding, new ByteList(name, p, end), dummy);
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
}