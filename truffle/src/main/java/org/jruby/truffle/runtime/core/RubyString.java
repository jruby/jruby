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

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.interop.ForeignAccessFactory;
import com.oracle.truffle.api.nodes.Node;
import org.jcodings.Encoding;
import org.jcodings.specific.ASCIIEncoding;
import org.jcodings.specific.USASCIIEncoding;
import org.jcodings.specific.UTF8Encoding;
import org.jruby.runtime.Helpers;
import org.jruby.truffle.nodes.RubyNode;
import org.jruby.truffle.nodes.objects.Allocator;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.control.RaiseException;
import org.jruby.util.ByteList;
import org.jruby.util.CodeRangeable;
import org.jruby.util.StringSupport;

/**
 * Represents the Ruby {@code String} class.
 */
public class RubyString extends RubyBasicObject implements CodeRangeable {

    private ByteList bytes;
    private int codeRange = StringSupport.CR_UNKNOWN;

    public RubyString(RubyClass stringClass, ByteList bytes) {
        super(stringClass);
        this.bytes = bytes;
    }

    public static RubyString fromJavaString(RubyClass stringClass, String string) {
        return new RubyString(stringClass, new ByteList(org.jruby.RubyEncoding.encodeUTF8(string), USASCIIEncoding.INSTANCE, false));
    }

    public static RubyString fromJavaString(RubyClass stringClass, String string, Encoding encoding) {
        return new RubyString(stringClass, new ByteList(org.jruby.RubyEncoding.encodeUTF8(string), encoding, false));
    }

    public static RubyString fromByteList(RubyClass stringClass, ByteList bytes) {
        return new RubyString(stringClass, bytes);
    }

    public void set(ByteList bytes) {
        this.bytes = bytes;
    }

    public void forceEncoding(Encoding encoding) {
        this.bytes.setEncoding(encoding);
        clearCodeRange();
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

    public int count(RubyString[] otherStrings) {
        if (bytes.getRealSize() == 0) {
            return 0;
        }

        RubyString otherStr = otherStrings[0];
        Encoding enc = otherStr.getBytes().getEncoding();

        final boolean[]table = new boolean[StringSupport.TRANS_SIZE + 1];
        StringSupport.TrTables tables = StringSupport.trSetupTable(otherStr.getBytes(), getContext().getRuntime(), table, null, true, enc);
        for (int i = 1; i < otherStrings.length; i++) {
            otherStr = otherStrings[i];

            // TODO (nirvdrum Dec. 19, 2014): This method should be encoding aware and check that the strings have compatible encodings.  See non-Truffle JRuby for a more complete solution.
            //enc = checkEncoding(otherStr);
            tables = StringSupport.trSetupTable(otherStr.getBytes(), getContext().getRuntime(), table, tables, false, enc);
        }

        return StringSupport.countCommon19(getBytes(), getContext().getRuntime(), table, tables, enc);
    }

    @Override
    @TruffleBoundary
    public String toString() {
        RubyNode.notDesignedForCompilation();

        return Helpers.decodeByteList(getContext().getRuntime(), bytes);
    }

    public int length() {
        return StringSupport.strLengthFromRubyString(this);
    }

    public int normalizeIndex(int index) {
        return RubyArray.normalizeIndex(bytes.length(), index);
    }

    public int clampExclusiveIndex(int index) {
        return RubyArray.clampExclusiveIndex(bytes.length(), index);
    }

    @Override
    public ForeignAccessFactory getForeignAccessFactory() {
        throw new UnsupportedOperationException();
    }

    @Override
    public int getCodeRange() {
        return codeRange;
    }

    @Override
    @TruffleBoundary
    public int scanForCodeRange() {
        int cr = getCodeRange();

        if (cr == StringSupport.CR_UNKNOWN) {
            cr = slowCodeRangeScan();
            setCodeRange(cr);
        }

        return cr;
    }

    @Override
    public boolean isCodeRangeValid() {
        return codeRange == StringSupport.CR_VALID;
    }

    @Override
    public final void setCodeRange(int codeRange) {
        this.codeRange = codeRange;
    }

    @Override
    public final void clearCodeRange() {
        codeRange = StringSupport.CR_UNKNOWN;
    }

    @Override
    public final void keepCodeRange() {
        if (getCodeRange() == StringSupport.CR_BROKEN) {
            clearCodeRange();
        }
    }

    @Override
    public final void modify() {
        // TODO (nirvdrum 16-Feb-15): This should check whether the underlying ByteList is being shared and copy if necessary.
        bytes.invalidate();
    }

    @Override
    public final void modify(int length) {
        // TODO (nirvdrum Jan. 13, 2015): This should check whether the underlying ByteList is being shared and copy if necessary.
        bytes.ensure(length);
        bytes.invalidate();
    }

    @Override
    public Encoding checkEncoding(CodeRangeable other) {
        // TODO (nirvdrum 16-Mar-15) This will return null for bad cases and we really don't want to propagate that.  Need a way to raise an appropriate exception while adhering to the interface.
        return StringSupport.areCompatible(this, other);
    }

    public Encoding checkEncoding(CodeRangeable other, Node node) {
        final Encoding encoding = checkEncoding(other);

        if (encoding == null) {
            throw new RaiseException(
                    getContext().getCoreLibrary().encodingCompatibilityErrorIncompatible(
                            this.getByteList().getEncoding().toString(),
                            other.getByteList().getEncoding().toString(),
                            node)
            );
        }

        return encoding;
    }

    @Override
    public ByteList getByteList() {
        return bytes;
    }

    public static class StringAllocator implements Allocator {

        @Override
        public RubyBasicObject allocate(RubyContext context, RubyClass rubyClass, Node currentNode) {
            return new RubyString(rubyClass, new ByteList());
        }

    }

    @TruffleBoundary
    private int slowCodeRangeScan() {
        return StringSupport.codeRangeScan(bytes.getEncoding(), bytes);
    }

}
