/*
 * Copyright (c) 2014, 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 *
 * Some of the code in this class is modified from org.jruby.runtime.encoding.EncodingService,
 * licensed under the same EPL1.0/GPL 2.0/LGPL 2.1 used throughout.
 */
package org.jruby.truffle.core.encoding;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.object.DynamicObject;
import org.jcodings.Encoding;
import org.jcodings.EncodingDB;
import org.jcodings.specific.ISO8859_16Encoding;
import org.jcodings.specific.USASCIIEncoding;
import org.jruby.truffle.Layouts;
import org.jruby.truffle.RubyContext;
import org.jruby.truffle.core.rope.Rope;
import org.jruby.truffle.core.string.StringOperations;
import org.jruby.util.ByteList;
import org.jruby.util.encoding.ISO_8859_16;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.charset.UnsupportedCharsetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class EncodingManager {

    private final List<DynamicObject> ENCODING_LIST_BY_ENCODING_LIST_INDEX = new ArrayList<DynamicObject>(EncodingDB.getEncodings().size());
    private final Map<Integer, DynamicObject> ENCODING_LIST_BY_ENCODING_INDEX = new HashMap<Integer,DynamicObject>(EncodingDB.getEncodings().size());
    private final Map<String, DynamicObject> LOOKUP = new HashMap<>();

    private final RubyContext context;

    public EncodingManager(RubyContext context) {
        this.context = context;
    }

    @TruffleBoundary
    public static DynamicObject newRubyEncoding(RubyContext context, Encoding encoding, byte[] name, int p, int end, boolean dummy) {
        // TODO (nirvdrum 21-Jun-16): We probably don't need to create a ByteList and two Ropes. Without any guarantees on the code range of the encoding name, however, we must be conservative.
        final Rope rope = StringOperations.ropeFromByteList(new ByteList(name, p, end, USASCIIEncoding.INSTANCE, false));
        final Rope cachedRope = context.getRopeTable().getRope(rope.getBytes(), rope.getEncoding(), rope.getCodeRange());
        final DynamicObject string = context.getFrozenStrings().getFrozenString(cachedRope);

        return Layouts.ENCODING.createEncoding(context.getCoreLibrary().getEncodingFactory(), encoding, string, dummy);
    }

    public List<DynamicObject> getUnsafeEncodingList() {
        return ENCODING_LIST_BY_ENCODING_LIST_INDEX;
    }

    public DynamicObject getRubyEncoding(Encoding encoding) {
        DynamicObject rubyEncoding = ENCODING_LIST_BY_ENCODING_INDEX.get(encoding.getIndex());

        if (rubyEncoding == null) {
            // Bounded by the number of encodings
            CompilerDirectives.transferToInterpreterAndInvalidate();

            rubyEncoding = LOOKUP.get(new String(encoding.getName(), StandardCharsets.UTF_8).toLowerCase(Locale.ENGLISH));
            ENCODING_LIST_BY_ENCODING_INDEX.put(encoding.getIndex(),rubyEncoding);
        }

        return rubyEncoding;
    }

    @TruffleBoundary
    public DynamicObject getRubyEncoding(String name) {
        return LOOKUP.get(name.toLowerCase(Locale.ENGLISH));
    }

    @TruffleBoundary
    public DynamicObject getRubyEncoding(int encodingListIndex) {
        return ENCODING_LIST_BY_ENCODING_LIST_INDEX.get(encodingListIndex);
    }

    @TruffleBoundary
    public void defineEncoding(EncodingDB.Entry encodingEntry, byte[] name, int p, int end) {
        final DynamicObject rubyEncoding = newRubyEncoding(context, null, name, p, end, encodingEntry.isDummy());

        assert ENCODING_LIST_BY_ENCODING_LIST_INDEX.size() == encodingEntry.getIndex();
        ENCODING_LIST_BY_ENCODING_LIST_INDEX.add(encodingEntry.getIndex(), rubyEncoding);
        LOOKUP.put(Layouts.ENCODING.getName(rubyEncoding).toString().toLowerCase(Locale.ENGLISH), rubyEncoding);
    }

    @TruffleBoundary
    public void defineAlias(int encodingListIndex, String name) {
        final DynamicObject rubyEncoding = getRubyEncoding(encodingListIndex);

        LOOKUP.put(name.toLowerCase(Locale.ENGLISH), rubyEncoding);
    }

    @TruffleBoundary
    public Encoding getLocaleEncoding() {
        return context.getJRubyRuntime().getEncodingService().getLocaleEncoding();
    }

    @TruffleBoundary
    public static Charset charsetForEncoding(Encoding encoding) {
        final String encodingName = encoding.toString();

        if (encodingName.equals("ASCII-8BIT")) {
            return Charset.forName("ISO-8859-1");
        }

        if (encoding == ISO8859_16Encoding.INSTANCE) {
            return ISO_8859_16.INSTANCE;
        }

        try {
            return Charset.forName(encodingName);
        } catch (UnsupportedCharsetException uce) {
            throw new UnsupportedOperationException("no java.nio.charset.Charset found for encoding `" + encoding.toString() + "'", uce);
        }
    }

}
