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
import org.jruby.RubyBoolean;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.truffle.runtime.NilPlaceholder;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.core.array.ArrayUtilities;
import org.jruby.truffle.runtime.core.range.FixnumRange;
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
            return new RubyString(getContext().getCoreLibrary().getStringClass(), "");
        }

    }

    private boolean fromJavaString;
    private ByteList bytes;
    private String cachedStringValue;

    /**
     * Construct a string from a Java {@link String}, lazily converting to bytes as needed.
     */
    public RubyString(RubyClass stringClass, String value) {
        super(stringClass);
        fromJavaString = true;
        bytes = org.jruby.RubySymbol.symbolBytesFromString(getRubyClass().getContext().getRuntime(), value);
        cachedStringValue = value;
    }

    public RubyString(RubyClass stringClass, org.jruby.RubyString value) {
        super(stringClass);
        fromJavaString = true;
        bytes = org.jruby.RubySymbol.symbolBytesFromString(getRubyClass().getContext().getRuntime(), value.toString());
        cachedStringValue = value.asJavaString();
    }
    /**
     * Construct a string from bytes representing characters in an encoding, lazily converting to a
     * Java {@link String} as needed.
     */


    public RubyString(RubyString copyOf) {
        super(copyOf.getRubyClass().getContext().getCoreLibrary().getStringClass());
        fromJavaString = copyOf.fromJavaString;
        bytes = copyOf.getBytes();


        cachedStringValue = copyOf.cachedStringValue;
    }

    public void forceEncoding(Encoding encoding) {
        this.bytes.setEncoding(encoding);
    }

    public boolean isFromJavaString() {
        return fromJavaString;
    }

    public ByteList getBytes() {
        return bytes;
    }

    public void replace(String value) {
        fromJavaString = true;
        bytes = null;
        cachedStringValue = value;
    }

    @Override
    public String toString() {
        if (cachedStringValue == null) {
            cachedStringValue = bytes.toString();
        }

        return cachedStringValue;
    }

    @Override
    public boolean equals(Object other) {
        if (other == null) {
            return false;
        }

        // If the other value is a Java string, use our Java string representation to compare
        if (other instanceof String) {
            return toString().equals(other);
        }

        if (other instanceof RubyString) {
            final RubyString otherString = (RubyString) other;
            IRubyObject resp =  this.toJRubyString().op_equal(getRubyClass().getContext().getRuntime().getCurrentContext(),
                    otherString.toJRubyString());

            if (resp instanceof RubyBoolean){
                return resp.isTrue();
            }
        }

        return false;
    }

    @Override
    public int hashCode() {
        return toString().hashCode();
    }

    public static Object getIndex(RubyContext context, String string, Object[] args) {
        if (args.length == 1) {
            final Object index = args[0];

            if (index instanceof Integer) {
                final int stringLength = string.length();
                final int normalisedIndex = ArrayUtilities.normaliseIndex(stringLength, (int) index);

                return context.makeString(string.charAt(normalisedIndex));
            } else if (index instanceof FixnumRange) {
                final FixnumRange range = (FixnumRange) index;

                final int stringLength = string.length();

                if (range.doesExcludeEnd()) {
                    final int begin = ArrayUtilities.normaliseIndex(stringLength, range.getBegin());
                    final int exclusiveEnd = ArrayUtilities.normaliseExclusiveIndex(stringLength, range.getExclusiveEnd());
                    return context.makeString(string.substring(begin, exclusiveEnd));
                } else {
                    final int begin = ArrayUtilities.normaliseIndex(stringLength, range.getBegin());
                    final int inclusiveEnd = ArrayUtilities.normaliseIndex(stringLength, range.getInclusiveEnd());
                    return context.makeString(string.substring(begin, inclusiveEnd + 1));
                }
            } else {
                throw new UnsupportedOperationException("Don't know how to index a string with " + index.getClass());
            }
        } else {
            final int rangeStart = (int) args[0];
            int rangeLength = (int) args[1];

            if (rangeLength > string.length() - rangeStart) {
                rangeLength = string.length() - rangeStart;
            }

            if (rangeStart > string.length()) {
                return NilPlaceholder.INSTANCE;
            }

            return context.makeString(string.substring(rangeStart, rangeStart + rangeLength));
        }
    }

    @Override
    public Object dup() {
        return new RubyString(this);
    }

    public org.jruby.RubyString toJRubyString() {
        return getRubyClass().getContext().getRuntime().newString(bytes);
    }

    public void concat(RubyString other) {
        if (fromJavaString && other.fromJavaString) {
            cachedStringValue += other.cachedStringValue;
            bytes = null;
        } else {
            throw new UnsupportedOperationException("Don't know how to append strings with encodings");
        }
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

    public String getReverseString() {
        return new StringBuilder(cachedStringValue).reverse().toString();
    }

    public void reverseStringValue(){
        checkFrozen();
        this.cachedStringValue = getReverseString();
    }

}
