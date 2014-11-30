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
import org.jruby.truffle.nodes.RubyNode;
import org.jruby.truffle.runtime.RubyContext;

import java.util.HashMap;
import java.util.Map;

/**
 * This is a bridge between JRuby encoding and Truffle encoding
 */
public class RubyEncoding extends RubyBasicObject {

    private static Map<Encoding, RubyEncoding> map = new HashMap<>();

    private final Encoding encoding;

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
        RubyEncoding mapped = map.get(encoding);

        if (mapped == null) {
            mapped = new RubyEncoding(context.getCoreLibrary().getEncodingClass(), encoding);
            map.put(encoding, mapped);
        }

        return mapped;
    }

    public static RubyEncoding getEncoding(RubyContext context, String name) {
        return getEncoding(context, context.getRuntime().getEncodingService().getEncodingFromString(name));
    }

    private RubyEncoding(RubyClass encodingClass, Encoding encoding) {
        super(encodingClass);
        this.encoding = encoding;
    }

    public Encoding getEncoding() {
        return encoding;
    }

}