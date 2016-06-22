/*
 * Copyright (c) 2013, 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.core.string;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.object.DynamicObject;
import org.jcodings.Encoding;
import org.jruby.truffle.Layouts;
import org.jruby.truffle.RubyContext;
import org.jruby.truffle.core.encoding.EncodingNodes;
import org.jruby.truffle.language.RubyGuards;
import org.jruby.util.ByteList;
import org.jruby.util.CodeRangeable;

public class StringCodeRangeableWrapper implements CodeRangeable {

    private final DynamicObject string;

    public StringCodeRangeableWrapper(DynamicObject string) {
        assert RubyGuards.isRubyString(string);
        this.string = string;
    }

    @Override
    public int getCodeRange() {
        return StringOperations.getCodeRange(string).toInt();
    }

    @Override
    public int scanForCodeRange() {
        return StringOperations.codeRange(string).toInt();
    }

    @Override
    public boolean isCodeRangeValid() {
        return StringOperations.isCodeRangeValid(string);
    }

    @Override
    public void setCodeRange(int newCodeRange) {
        StringOperations.setCodeRange(string, newCodeRange);
    }

    @Override
    public final void clearCodeRange() {
        StringOperations.clearCodeRange(string);
    }

    @Override
    public final void keepCodeRange() {
        StringOperations.keepCodeRange(string);
    }

    @Override
    public final void modify() {
        // No-op. Ropes are immutable so any modifications must've been handled elsewhere.
    }

    @Override
    public final void modify(int length) {
        // No-op. Ropes are immutable so any modifications must've been handled elsewhere.
    }

    @Override
    public final void modifyAndKeepCodeRange() {
        StringOperations.keepCodeRange(string);
    }

    @Override
    @TruffleBoundary(throwsControlFlowException = true)
    public Encoding checkEncoding(CodeRangeable other) {
        final Encoding encoding = EncodingNodes.CompatibleQueryNode.compatibleEncodingForStrings(string, ((StringCodeRangeableWrapper) other).getString());

        // TODO (nirvdrum 23-Mar-15) We need to raise a proper Truffle+JRuby exception here, rather than a non-Truffle JRuby exception.
        if (encoding == null) {
            final RubyContext context = Layouts.MODULE.getFields(Layouts.BASIC_OBJECT.getLogicalClass(string)).getContext();
            throw context.getJRubyRuntime().newEncodingCompatibilityError(
                    String.format("incompatible character encodings: %s and %s",
                            Layouts.STRING.getRope(string).getEncoding().toString(),
                            other.getByteList().getEncoding().toString()));
        }

        return encoding;
    }

    @Override
    public ByteList getByteList() {
        throw new RuntimeException("Replace with read-only call or rope update for String.");
    }


    public DynamicObject getString() {
        return string;
    }
}
