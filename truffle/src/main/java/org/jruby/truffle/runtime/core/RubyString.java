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
import com.oracle.truffle.api.object.DynamicObject;
import org.jcodings.Encoding;
import org.jruby.runtime.Helpers;
import org.jruby.util.ByteList;
import org.jruby.util.CodeRangeable;
import org.jruby.util.StringSupport;

public class RubyString extends RubyBasicObject {

    public ByteList bytes;
    public int codeRange = StringSupport.CR_UNKNOWN;

    private CodeRangeableWrapper codeRangeableWrapper;

    public RubyString(RubyClass stringClass, ByteList bytes, DynamicObject dynamicObject) {
        super(stringClass, dynamicObject);
        this.bytes = bytes;
    }

    @Override
    @TruffleBoundary
    public String toString() {
        return Helpers.decodeByteList(getContext().getRuntime(), bytes);
    }

    public int getCodeRange() {
        return codeRange;
    }

    @TruffleBoundary
    public int scanForCodeRange() {
        int cr = getCodeRange();

        if (cr == StringSupport.CR_UNKNOWN) {
            cr = slowCodeRangeScan();
            setCodeRange(cr);
        }

        return cr;
    }

    public boolean isCodeRangeValid() {
        return codeRange == StringSupport.CR_VALID;
    }

    public final void setCodeRange(int newCodeRange) {
        codeRange = newCodeRange;
    }

    public final void clearCodeRange() {
        codeRange = StringSupport.CR_UNKNOWN;
    }

    public final void keepCodeRange() {
        if (getCodeRange() == StringSupport.CR_BROKEN) {
            clearCodeRange();
        }
    }

    public final void modify() {
        // TODO (nirvdrum 16-Feb-15): This should check whether the underlying ByteList is being shared and copy if necessary.
        bytes.invalidate();
    }

    public final void modify(int length) {
        // TODO (nirvdrum Jan. 13, 2015): This should check whether the underlying ByteList is being shared and copy if necessary.
        bytes.ensure(length);
        bytes.invalidate();
    }

    public final void modifyAndKeepCodeRange() {
        modify();
        keepCodeRange();
    }

    @TruffleBoundary
    public Encoding checkEncoding(CodeRangeable other) {
        final Encoding encoding = StringSupport.areCompatible(getCodeRangeable(), other);

        // TODO (nirvdrum 23-Mar-15) We need to raise a proper Truffle+JRuby exception here, rather than a non-Truffle JRuby exception.
        if (encoding == null) {
            throw getContext().getRuntime().newEncodingCompatibilityError(
                    String.format("incompatible character encodings: %s and %s",
                            getByteList().getEncoding().toString(),
                            other.getByteList().getEncoding().toString()));
        }

        return encoding;
    }

    public ByteList getByteList() {
        return bytes;
    }

    @TruffleBoundary
    private int slowCodeRangeScan() {
        return StringSupport.codeRangeScan(bytes.getEncoding(), bytes);
    }

    public CodeRangeableWrapper getCodeRangeable() {
        if (codeRangeableWrapper == null) {
            codeRangeableWrapper = new CodeRangeableWrapper();
        }

        return codeRangeableWrapper;
    }

    public class CodeRangeableWrapper implements CodeRangeable {

        @Override
        public int getCodeRange() {
            return RubyString.this.getCodeRange();
        }

        @Override
        public int scanForCodeRange() {
            return RubyString.this.scanForCodeRange();
        }

        @Override
        public boolean isCodeRangeValid() {
            return RubyString.this.isCodeRangeValid();
        }

        @Override
        public final void setCodeRange(int newCodeRange) {
            RubyString.this.setCodeRange(newCodeRange);
        }

        @Override
        public final void clearCodeRange() {
            RubyString.this.clearCodeRange();
        }

        @Override
        public final void keepCodeRange() {
            RubyString.this.keepCodeRange();
        }

        @Override
        public final void modify() {
            RubyString.this.modify();
        }

        @Override
        public final void modify(int length) {
            RubyString.this.modify(length);
        }

        @Override
        public final void modifyAndKeepCodeRange() {
            RubyString.this.modifyAndKeepCodeRange();
        }

        @Override
        public Encoding checkEncoding(CodeRangeable other) {
            return RubyString.this.checkEncoding(other);
        }

        @Override
        public ByteList getByteList() {
            return RubyString.this.getByteList();
        }

    }

}
