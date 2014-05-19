/*
 * Copyright (c) 2013 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.runtime.core;

import org.jcodings.Encoding;
import org.jcodings.specific.UTF8Encoding;
import org.jruby.runtime.Helpers;
import org.jruby.truffle.runtime.core.array.RubyArray;
import org.jruby.util.ByteList;

import java.math.BigInteger;

/**
 * Represents the Ruby {@code String} class.
 */
public class RubyString extends RubyObject {

    /**
     * The class from which we create the object that is {@code String}. A subclass of
     * {@link RubyClass} so that we can override {@link #newInstance} and allocate a
     * {@link RubyString} rather than a normal {@link RubyBasicObject}.
     */
    public static class RubyStringClass extends RubyClass {

        public RubyStringClass(RubyClass objectClass) {
            super(null, objectClass, "String");
        }

        @Override
        public RubyBasicObject newInstance() {
            return new RubyString(this, new ByteList());
        }

    }

    private ByteList bytes;

    public RubyString(RubyClass stringClass, ByteList bytes) {
        super(stringClass);
        this.bytes = bytes;
    }

    public static RubyString fromJavaString(RubyClass stringClass, String string) {
        return new RubyString(stringClass, new ByteList(org.jruby.RubyEncoding.encodeUTF8(string), UTF8Encoding.INSTANCE, false));
    }

    public void set(String string) {
        bytes = new ByteList(org.jruby.RubyEncoding.encodeUTF8(string), UTF8Encoding.INSTANCE, false);
    }

    public void set(RubyString string) {
        bytes = string.getBytes().dup();
    }

    public void forceEncoding(Encoding encoding) {
        this.bytes.setEncoding(encoding);
    }

    public ByteList getBytes() {
        return bytes;
    }

    public static String ljust(String string, int length, String padding) {
        final StringBuilder builder = new StringBuilder();

        builder.append(string);

        int n = 0;

        while (builder.length() < length) {
            builder.append(padding.charAt(n));

            n++;

            if (n == padding.length()) {
                n = 0;
            }
        }

        return builder.toString();
    }

    public static String rjust(String string, int length, String padding) {
        final StringBuilder builder = new StringBuilder();

        int n = 0;

        while (builder.length() + string.length() < length) {
            builder.append(padding.charAt(n));

            n++;

            if (n == padding.length()) {
                n = 0;
            }
        }

        builder.append(string);

        return builder.toString();
    }

    public Object toInteger() {
        if (toString().length() == 0) {
            return 0;
        }

        try {
            final int value = Integer.parseInt(toString());

            if (value >= RubyFixnum.MIN_VALUE && value <= RubyFixnum.MAX_VALUE) {
                return value;
            } else {
                return BigInteger.valueOf(value);
            }
        } catch (NumberFormatException e) {
            return new BigInteger(toString());
        }
    }

    public org.jruby.RubyString toJRubyString() {
        return getRubyClass().getContext().getRuntime().newString(bytes);
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }

        if (other instanceof String || other instanceof RubyString) {
            return toString().equals(other.toString());
        }

        return false;
    }

    @Override
    public Object dup() {
        return new RubyString(getRubyClass(), bytes.dup());
    }

    @Override
    public String toString() {
        return Helpers.decodeByteList(getRubyClass().getContext().getRuntime(), bytes);
    }

    @Override
    public int hashCode() {
        return bytes.hashCode();
    }

    public int normaliseIndex(int index) {
        return RubyArray.normaliseIndex(bytes.length(), index);
    }

    public int normaliseExclusiveIndex(int index) {
        return RubyArray.normaliseExclusiveIndex(bytes.length(), index);
    }

}
