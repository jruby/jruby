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

import com.oracle.truffle.api.object.DynamicObject;
import org.jcodings.Encoding;
import org.jruby.truffle.nodes.RubyGuards;
import org.jruby.truffle.runtime.layouts.Layouts;
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
        return Layouts.STRING.getCodeRange(string);
    }

    @Override
    public int scanForCodeRange() {
        return StringOperations.scanForCodeRange(string);
    }

    @Override
    public boolean isCodeRangeValid() {
        return StringOperations.isCodeRangeValid(string);
    }

    @Override
    public final void setCodeRange(int newCodeRange) {
        Layouts.STRING.setCodeRange(string, newCodeRange);
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
        StringOperations.modify(string);
    }

    @Override
    public final void modify(int length) {
        StringOperations.modify(string, length);
    }

    @Override
    public final void modifyAndKeepCodeRange() {
        StringOperations.modifyAndKeepCodeRange(string);
    }

    @Override
    public Encoding checkEncoding(CodeRangeable other) {
        return StringOperations.checkEncoding(string, other);
    }

    @Override
    public ByteList getByteList() {
        return Layouts.STRING.getByteList(string);
    }

}
