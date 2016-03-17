/*
 * Copyright (c) 2013, 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.interop;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.interop.ForeignAccess;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.source.SourceSection;
import org.jruby.truffle.RubyContext;
import org.jruby.truffle.core.Layouts;
import org.jruby.truffle.core.string.StringOperations;
import org.jruby.truffle.language.RubyGuards;
import org.jruby.truffle.language.RubyNode;

class InteropReadStringByteNode extends RubyNode {

    private final int labelIndex;

    public InteropReadStringByteNode(RubyContext context, SourceSection sourceSection, int labelIndex) {
        super(context, sourceSection);
        this.labelIndex = labelIndex;
    }

    @Override
    public Object execute(VirtualFrame frame) {
        if (RubyGuards.isRubyString(ForeignAccess.getReceiver(frame))) {
            final DynamicObject string = (DynamicObject) ForeignAccess.getReceiver(frame);
            final int index = (int) ForeignAccess.getArguments(frame).get(labelIndex);
            if (index >= Layouts.STRING.getRope(string).byteLength()) {
                return 0;
            } else {
                return (byte) StringOperations.getByteListReadOnly(string).get(index);
            }
        } else {
            CompilerDirectives.transferToInterpreter();
            throw new IllegalStateException("Not implemented");
        }
    }
}
