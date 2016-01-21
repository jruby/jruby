/*
 * Copyright (c) 2013, 2015, 2016 Oracle and/or its affiliates. All rights reserved. This
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
import org.jruby.RubyString;
import org.jruby.runtime.Helpers;
import org.jruby.truffle.nodes.RubyGuards;
import org.jruby.truffle.nodes.core.EncodingNodes;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.control.RaiseException;
import org.jruby.truffle.runtime.layouts.Layouts;
import org.jruby.truffle.runtime.rope.Rope;
import org.jruby.truffle.runtime.rope.RopeOperations;
import org.jruby.util.ByteList;
import org.jruby.util.CodeRangeable;
import org.jruby.util.StringSupport;

public abstract class StringOperations {

    /** Creates a String from the ByteList, with unknown CR */
    public static DynamicObject createString(RubyContext context, ByteList bytes) {
        return Layouts.STRING.createString(context.getCoreLibrary().getStringFactory(), ropeFromByteList(bytes, StringSupport.CR_UNKNOWN), null);
    }

    /** Creates a String from the ByteList, with 7-bit CR */
    public static DynamicObject create7BitString(RubyContext context, ByteList bytes) {
        return Layouts.STRING.createString(context.getCoreLibrary().getStringFactory(), ropeFromByteList(bytes, StringSupport.CR_7BIT), null);
    }

    public static DynamicObject createString(RubyContext context, Rope rope) {
        return Layouts.STRING.createString(context.getCoreLibrary().getStringFactory(), rope, null);
    }

    // Since ByteList.toString does not decode properly
    @CompilerDirectives.TruffleBoundary
    public static String getString(RubyContext context, DynamicObject string) {
        return RopeOperations.decodeRope(context.getRuntime(), StringOperations.rope(string));
    }

    public static StringCodeRangeableWrapper getCodeRangeable(DynamicObject string) {
        StringCodeRangeableWrapper wrapper = Layouts.STRING.getCodeRangeableWrapper(string);

        if (wrapper == null) {
            wrapper = new StringCodeRangeableWrapper(string);
            Layouts.STRING.setCodeRangeableWrapper(string, wrapper);
        }

        return wrapper;
    }

    public static StringCodeRangeableWrapper getCodeRangeableReadWrite(final DynamicObject string) {
        return new StringCodeRangeableWrapper(string) {
            private final ByteList byteList = StringOperations.rope(string).toByteListCopy();
            int codeRange = StringOperations.getCodeRange(string);

            @Override
            public void setCodeRange(int newCodeRange) {
                this.codeRange = newCodeRange;
            }

            @Override
            public int getCodeRange() {
                return codeRange;
            }

            @Override
            public ByteList getByteList() {
                return byteList;
            }
        };
    }

    public static StringCodeRangeableWrapper getCodeRangeableReadOnly(final DynamicObject string) {
        return new StringCodeRangeableWrapper(string) {
            @Override
            public ByteList getByteList() {
                return StringOperations.getByteListReadOnly(string);
            }
        };
    }

    public static int getCodeRange(DynamicObject string) {
        return Layouts.STRING.getRope(string).getCodeRange();
    }

    public static void setCodeRange(DynamicObject string, int codeRange) {
        // TODO (nirvdrum 07-Jan-16) Code range is now stored in the rope and ropes are immutable -- all calls to this method are suspect.
        final int existingCodeRange = StringOperations.getCodeRange(string);

        if (existingCodeRange != codeRange) {
            CompilerDirectives.transferToInterpreter();
            throw new RuntimeException(String.format("Tried changing the code range value for a rope from %d to %d", existingCodeRange, codeRange));
        }
    }

    public static int scanForCodeRange(DynamicObject string) {
        int cr = StringOperations.getCodeRange(string);

        if (cr == StringSupport.CR_UNKNOWN) {
            cr = slowCodeRangeScan(string);
            StringOperations.setCodeRange(string, cr);
        }

        return cr;
    }

    public static boolean isCodeRangeValid(DynamicObject string) {
        return StringOperations.getCodeRange(string) == StringSupport.CR_VALID;
    }

    public static void clearCodeRange(DynamicObject string) {
        StringOperations.setCodeRange(string, StringSupport.CR_UNKNOWN);
    }

    public static void keepCodeRange(DynamicObject string) {
        if (StringOperations.getCodeRange(string) == StringSupport.CR_BROKEN) {
            clearCodeRange(string);
        }
    }

    public static void modify(DynamicObject string) {
        // No-op. Ropes are immutable so any modifications must've been handled elsewhere.
        // TODO (nirvdrum 07-Jan-16) Remove this method once we've inspected each caller for correctness.
    }

    public static void modify(DynamicObject string, int length) {
        // No-op. Ropes are immutable so any modifications must've been handled elsewhere.
        // TODO (nirvdrum 07-Jan-16) Remove this method once we've inspected each caller for correctness.
    }

    public static void modifyAndKeepCodeRange(DynamicObject string) {
        modify(string);
        keepCodeRange(string);
    }

    @CompilerDirectives.TruffleBoundary
    public static Encoding checkEncoding(DynamicObject string, CodeRangeable other) {
        final Encoding encoding = EncodingNodes.CompatibleQueryNode.compatibleEncodingForStrings(string, ((StringCodeRangeableWrapper) other).getString());

        // TODO (nirvdrum 23-Mar-15) We need to raise a proper Truffle+JRuby exception here, rather than a non-Truffle JRuby exception.
        if (encoding == null) {
            CompilerDirectives.transferToInterpreter();
            throw Layouts.MODULE.getFields(Layouts.BASIC_OBJECT.getLogicalClass(string)).getContext().getRuntime().newEncodingCompatibilityError(
                    String.format("incompatible character encodings: %s and %s",
                            Layouts.STRING.getRope(string).getEncoding().toString(),
                            other.getByteList().getEncoding().toString()));
        }

        return encoding;
    }

    @CompilerDirectives.TruffleBoundary
    private static int slowCodeRangeScan(DynamicObject string) {
        final ByteList byteList = StringOperations.getByteListReadOnly(string);
        return StringSupport.codeRangeScan(byteList.getEncoding(), byteList);
    }

    public static void forceEncoding(DynamicObject string, Encoding encoding) {
        modify(string);
        final Rope oldRope = Layouts.STRING.getRope(string);
        Layouts.STRING.setRope(string, RopeOperations.withEncoding(oldRope, encoding, StringSupport.CR_UNKNOWN));
    }

    public static int normalizeIndex(int length, int index) {
        return ArrayOperations.normalizeIndex(length, index);
    }

    public static int clampExclusiveIndex(DynamicObject string, int index) {
        assert RubyGuards.isRubyString(string);

        // TODO (nirvdrum 21-Jan-16): Verify this is supposed to be the byteLength and not the characterLength.
        return ArrayOperations.clampExclusiveIndex(StringOperations.rope(string).byteLength(), index);
    }

    @CompilerDirectives.TruffleBoundary
    public static Encoding checkEncoding(RubyContext context, DynamicObject string, CodeRangeable other, Node node) {
        final Encoding encoding = StringSupport.areCompatible(getCodeRangeableReadOnly(string), other);

        if (encoding == null) {
            throw new RaiseException(context.getCoreLibrary().encodingCompatibilityErrorIncompatible(
                    Layouts.STRING.getRope(string).getEncoding().toString(),
                    other.getByteList().getEncoding().toString(),
                    node));
        }

        return encoding;
    }

    @TruffleBoundary
    public static ByteList encodeByteList(CharSequence value, Encoding encoding) {
        return RubyString.encodeBytelist(value, encoding);
    }

    public static ByteList getByteList(DynamicObject object) {
        throw new RuntimeException("Replace with read-only call or rope update for String.");
    }

    public static ByteList getByteListReadOnly(DynamicObject object) {
        return Layouts.STRING.getRope(object).getUnsafeByteList();
    }

    // TODO (nirdvrum 07-Jan-16) Either remove this method or Rope#byteLength -- the latter doesn't require materializing the full byte array.
    public static int byteLength(DynamicObject object) {
        return Layouts.STRING.getRope(object).byteLength();
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
        return RopeOperations.create(byteList.bytes(), byteList.getEncoding(), StringSupport.CR_UNKNOWN);
    }

    public static Rope ropeFromByteList(ByteList byteList, int codeRange) {
        // TODO (nirvdrum 08-Jan-16) We need to make a copy of the ByteList's bytes for now to be safe, but we should be able to use the unsafe bytes as we move forward.
        return RopeOperations.create(byteList.bytes(), byteList.getEncoding(), codeRange);
    }

    @TruffleBoundary
    public static ByteList createByteList(CharSequence s) {
        return ByteList.create(s);
    }

    public static Rope rope(DynamicObject string) {
        assert RubyGuards.isRubyString(string);

        return Layouts.STRING.getRope(string);
    }

    public static Encoding encoding(DynamicObject string) {
        assert RubyGuards.isRubyString(string);

        return rope(string).getEncoding();
    }

    public static int codeRange(DynamicObject string) {
        assert RubyGuards.isRubyString(string);

        return rope(string).getCodeRange();
    }

    public static String decodeUTF8(DynamicObject string) {
        assert RubyGuards.isRubyString(string);

        return RopeOperations.decodeUTF8(Layouts.STRING.getRope(string));
    }
}
