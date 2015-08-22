/*
 * Copyright (c) 2013, 2015 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.nodes.literal;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.source.SourceSection;
import org.jruby.truffle.nodes.RubyNode;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.layouts.Layouts;
import org.jruby.util.ByteList;
import org.jruby.util.StringSupport;

public class StringLiteralNode extends RubyNode {

    private final ByteList bytes;
    private final int codeRange;

    public StringLiteralNode(RubyContext context, SourceSection sourceSection, ByteList bytes, int codeRange) {
        super(context, sourceSection);
        assert bytes != null;
        this.bytes = bytes;
        this.codeRange = codeRange;
    }

    @Override
    public DynamicObject execute(VirtualFrame frame) {
        final DynamicObject string = Layouts.STRING.createString(getContext().getCoreLibrary().getStringFactory(), bytes.dup(), StringSupport.CR_UNKNOWN, null);
        Layouts.STRING.setCodeRange(string, codeRange);
        return string;
    }

}
