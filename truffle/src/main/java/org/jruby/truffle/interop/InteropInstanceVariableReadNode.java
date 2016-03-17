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
import com.oracle.truffle.api.source.SourceSection;
import org.jruby.truffle.RubyContext;
import org.jruby.truffle.language.RubyNode;
import org.jruby.truffle.language.objects.ReadInstanceVariableNode;

class InteropInstanceVariableReadNode extends RubyNode {

    @Child private ReadInstanceVariableNode read;
    private final String name;
    private final int labelIndex;

    public InteropInstanceVariableReadNode(RubyContext context, SourceSection sourceSection, String name, int labelIndex) {
        super(context, sourceSection);
        this.name = name;
        this.read = new ReadInstanceVariableNode(context, sourceSection, name, new RubyInteropReceiverNode(context, sourceSection));
        this.labelIndex = labelIndex;
    }

    @Override
    public Object execute(VirtualFrame frame) {
        if (name.equals(ForeignAccess.getArguments(frame).get(labelIndex))) {
            return read.execute(frame);
        } else {
            CompilerDirectives.transferToInterpreter();
            throw new IllegalStateException("Not implemented");
        }
    }
}
