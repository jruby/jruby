/*
 * Copyright (c) 2013, 2015 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 *
 * Contains code modified from JRuby's RubyString.java
 *
 * Copyright (C) 2001 Alan Moore <alan_moore@gmx.net>
 * Copyright (C) 2001-2002 Benoit Cerrina <b.cerrina@wanadoo.fr>
 * Copyright (C) 2001-2004 Jan Arne Petersen <jpetersen@uni-bonn.de>
 * Copyright (C) 2002-2004 Anders Bengtsson <ndrsbngtssn@yahoo.se>
 * Copyright (C) 2002-2006 Thomas E Enebo <enebo@acm.org>
 * Copyright (C) 2004 Stefan Matthias Aust <sma@3plus4.de>
 * Copyright (C) 2004 David Corbin <dcorbin@users.sourceforge.net>
 * Copyright (C) 2005 Tim Azzopardi <tim@tigerfive.com>
 * Copyright (C) 2006 Miguel Covarrubias <mlcovarrubias@gmail.com>
 * Copyright (C) 2006 Ola Bini <ola@ologix.com>
 * Copyright (C) 2007 Nick Sieger <nicksieger@gmail.com>
 *
 */
package org.jruby.truffle.runtime.core;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.object.DynamicObject;
import org.jcodings.Encoding;
import org.jcodings.specific.UTF8Encoding;
import org.jruby.RubyString;
import org.jruby.runtime.Helpers;
import org.jruby.truffle.nodes.RubyGuards;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.control.RaiseException;
import org.jruby.truffle.runtime.layouts.Layouts;
import org.jruby.truffle.runtime.rope.LeafRope;
import org.jruby.truffle.runtime.rope.Rope;
import org.jruby.util.ByteList;
import org.jruby.util.CodeRangeable;
import org.jruby.util.StringSupport;
import org.jruby.util.io.EncodingUtils;

public abstract class StringOperations {

    public static final Rope EMPTY_UTF8_ROPE = new LeafRope(new byte[] {}, UTF8Encoding.INSTANCE);

    /** Creates a String from the ByteList, with unknown CR */
    public static DynamicObject createString(RubyContext context, ByteList bytes) {
        return Layouts.STRING.createString(context.getCoreLibrary().getStringFactory(), ropeFromByteList(bytes), StringSupport.CR_UNKNOWN, null);
    }

    /** Creates a String from the ByteList, with 7-bit CR */
    public static DynamicObject create7BitString(RubyContext context, ByteList bytes) {
        return Layouts.STRING.createString(context.getCoreLibrary().getStringFactory(), ropeFromByteList(bytes), StringSupport.CR_7BIT, null);
    }

    // Since ByteList.toString does not decode properly
    @CompilerDirectives.TruffleBoundary
    public static String getString(RubyContext context, DynamicObject string) {
        return Helpers.decodeByteList(context.getRuntime(), StringOperations.getByteList(string));
    }

    public static StringCodeRangeableWrapper getCodeRangeable(DynamicObject string) {
        StringCodeRangeableWrapper wrapper = Layouts.STRING.getCodeRangeableWrapper(string);

        if (wrapper == null) {
            wrapper = new StringCodeRangeableWrapper(string);
            Layouts.STRING.setCodeRangeableWrapper(string, wrapper);
        }

        return wrapper;
    }

    public static int scanForCodeRange(DynamicObject string) {
        int cr = Layouts.STRING.getCodeRange(string);

        if (cr == StringSupport.CR_UNKNOWN) {
            cr = slowCodeRangeScan(string);
            Layouts.STRING.setCodeRange(string, cr);
        }

        return cr;
    }

    public static boolean isCodeRangeValid(DynamicObject string) {
        return Layouts.STRING.getCodeRange(string) == StringSupport.CR_VALID;
    }

    public static void clearCodeRange(DynamicObject string) {
        Layouts.STRING.setCodeRange(string, StringSupport.CR_UNKNOWN);
    }

    public static void keepCodeRange(DynamicObject string) {
        if (Layouts.STRING.getCodeRange(string) == StringSupport.CR_BROKEN) {
            clearCodeRange(string);
        }
    }

    public static void modify(DynamicObject string) {
        // TODO (nirvdrum 16-Feb-15): This should check whether the underlying ByteList is being shared and copy if necessary.
        StringOperations.getByteList(string).invalidate();
    }

    public static void modify(DynamicObject string, int length) {
        // TODO (nirvdrum Jan. 13, 2015): This should check whether the underlying ByteList is being shared and copy if necessary.
        StringOperations.getByteList(string).ensure(length);
        StringOperations.getByteList(string).invalidate();
    }

    public static void modifyAndKeepCodeRange(DynamicObject string) {
        modify(string);
        keepCodeRange(string);
    }

    @CompilerDirectives.TruffleBoundary
    public static Encoding checkEncoding(DynamicObject string, CodeRangeable other) {
        final Encoding encoding = StringSupport.areCompatible(getCodeRangeable(string), other);

        // TODO (nirvdrum 23-Mar-15) We need to raise a proper Truffle+JRuby exception here, rather than a non-Truffle JRuby exception.
        if (encoding == null) {
            throw Layouts.MODULE.getFields(Layouts.BASIC_OBJECT.getLogicalClass(string)).getContext().getRuntime().newEncodingCompatibilityError(
                    String.format("incompatible character encodings: %s and %s",
                            StringOperations.getByteList(string).getEncoding().toString(),
                            other.getByteList().getEncoding().toString()));
        }

        return encoding;
    }

    @CompilerDirectives.TruffleBoundary
    private static int slowCodeRangeScan(DynamicObject string) {
        final ByteList byteList = StringOperations.getByteList(string);
        return StringSupport.codeRangeScan(byteList.getEncoding(), byteList);
    }

    public static void forceEncoding(DynamicObject string, Encoding encoding) {
        modify(string);
        clearCodeRange(string);
        StringSupport.associateEncoding(getCodeRangeable(string), encoding);
        clearCodeRange(string);
    }

    public static int normalizeIndex(int length, int index) {
        return ArrayOperations.normalizeIndex(length, index);
    }

    public static int clampExclusiveIndex(DynamicObject string, int index) {
        assert RubyGuards.isRubyString(string);
        return ArrayOperations.clampExclusiveIndex(StringOperations.getByteList(string).length(), index);
    }

    @CompilerDirectives.TruffleBoundary
    public static Encoding checkEncoding(RubyContext context, DynamicObject string, CodeRangeable other, Node node) {
        final Encoding encoding = StringSupport.areCompatible(getCodeRangeable(string), other);

        if (encoding == null) {
            throw new RaiseException(context.getCoreLibrary().encodingCompatibilityErrorIncompatible(
                    StringOperations.getByteList(string).getEncoding().toString(),
                    other.getByteList().getEncoding().toString(),
                    node));
        }

        return encoding;
    }

    public static boolean singleByteOptimizable(DynamicObject string) {
        return StringSupport.isSingleByteOptimizable(getCodeRangeable(string), EncodingUtils.STR_ENC_GET(getCodeRangeable(string)));
    }

    @TruffleBoundary
    public static ByteList encodeByteList(CharSequence value, Encoding encoding) {
        return RubyString.encodeBytelist(value, encoding);
    }

    public static ByteList getByteList(DynamicObject object) {
        return Layouts.STRING.getRope(object).getByteList();
    }

    public static void setByteList(DynamicObject object, ByteList byteList) {
        Layouts.STRING.setRope(object, ropeFromByteList(byteList));
    }

    // TODO (nirdvrum 07-Jan-16) Either remove this method or Rope#byteLength -- the latter doesn't require materializing the full byte array.
    public static int byteLength(DynamicObject object) {
        return Layouts.STRING.getRope(object).getBytes().length;
    }

    public static int commonCodeRange(int first, int second) {
        if (first == second) {
            return first;
        }

        if ((first == StringSupport.CR_UNKNOWN) || (second == StringSupport.CR_UNKNOWN)) {
            return StringSupport.CR_UNKNOWN;
        }

        if ((first == StringSupport.CR_BROKEN) || (second == StringSupport.CR_BROKEN)) {
            return StringSupport.CR_BROKEN;
        }

        // If we get this far, one must be CR_7BIT and the other must be CR_VALID, so promote to the more general code range.
        return StringSupport.CR_VALID;
    }

    public static Rope ropeFromByteList(ByteList byteList) {
        return new LeafRope(byteList.bytes(), byteList.getEncoding());
    }

    @TruffleBoundary
    public static ByteList createByteList(CharSequence s) {
        return ByteList.create(s);
    }
}
