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

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.interop.ForeignAccessFactory;
import com.oracle.truffle.api.nodes.Node;
import org.jcodings.Encoding;
import org.jcodings.specific.USASCIIEncoding;
import org.jruby.runtime.Helpers;
import org.jruby.truffle.nodes.RubyNode;
import org.jruby.truffle.nodes.objects.Allocator;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.control.RaiseException;
import org.jruby.util.ByteList;
import org.jruby.util.CodeRangeable;
import org.jruby.util.StringSupport;
import org.jruby.util.io.EncodingUtils;

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
        modify();
        clearCodeRange();
        StringSupport.associateEncoding(this, encoding);
        clearCodeRange();
    }

    @Override
    @TruffleBoundary
    public String toString() {
        RubyNode.notDesignedForCompilation();

        return Helpers.decodeByteList(getContext().getRuntime(), bytes);
    }

    public int length() {
        if (CompilerDirectives.injectBranchProbability(
                CompilerDirectives.FASTPATH_PROBABILITY,
                StringSupport.isSingleByteOptimizable(this, getByteList().getEncoding()))) {

            return getByteList().getRealSize();

        } else {
            return StringSupport.strLengthFromRubyString(this);
        }
    }

    public int normalizeIndex(int length, int index) {
        return RubyArray.normalizeIndex(length, index);
    }

    public int normalizeIndex(int index) {
        return normalizeIndex(length(), index);
    }

    public int clampExclusiveIndex(int index) {
        return RubyArray.clampExclusiveIndex(bytes.length(), index);
    }

    @Override
    public ForeignAccessFactory getForeignAccessFactory() {
        return new StringForeignAccessFactory(getContext());
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
    public final void modifyAndKeepCodeRange() {
        modify();
        keepCodeRange();
    }

    @Override
    @TruffleBoundary
    public Encoding checkEncoding(CodeRangeable other) {
        final Encoding encoding = StringSupport.areCompatible(this, other);

        // TODO (nirvdrum 23-Mar-15) We need to raise a proper Truffle+JRuby exception here, rather than a non-Truffle JRuby exception.
        if (encoding == null) {
            throw getContext().getRuntime().newEncodingCompatibilityError(
                    String.format("incompatible character encodings: %s and %s",
                            getByteList().getEncoding().toString(),
                            other.getByteList().getEncoding().toString()));
        }

        return encoding;
    }

    @TruffleBoundary
    public Encoding checkEncoding(CodeRangeable other, Node node) {
        final Encoding encoding = StringSupport.areCompatible(this, other);

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

    public boolean singleByteOptimizable() {
        return StringSupport.isSingleByteOptimizable(this, EncodingUtils.STR_ENC_GET(this));
    }
}
