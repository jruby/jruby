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
import org.jruby.truffle.nodes.core.StringNodes;
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
        return StringNodes.getCodeRange(string);
    }

    @Override
    public int scanForCodeRange() {
        return StringNodes.scanForCodeRange(string);
    }

    @Override
    public boolean isCodeRangeValid() {
        return StringNodes.isCodeRangeValid(string);
    }

    @Override
    public final void setCodeRange(int newCodeRange) {
        StringNodes.setCodeRange(string, newCodeRange);
    }

    @Override
    public final void clearCodeRange() {
        StringNodes.clearCodeRange(string);
    }

    @Override
    public final void keepCodeRange() {
        StringNodes.keepCodeRange(string);
    }

    @Override
    public final void modify() {
        StringNodes.modify(string);
    }

    @Override
    public final void modify(int length) {
        StringNodes.modify(string, length);
    }

    @Override
    public final void modifyAndKeepCodeRange() {
        StringNodes.modifyAndKeepCodeRange(string);
    }

    @Override
    public Encoding checkEncoding(CodeRangeable other) {
        return StringNodes.checkEncoding(string, other);
    }

    @Override
    public ByteList getByteList() {
        return StringNodes.getByteList(string);
    }

}
