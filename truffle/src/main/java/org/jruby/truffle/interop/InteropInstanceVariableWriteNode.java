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
import org.jruby.truffle.language.objects.WriteInstanceVariableNode;

public class InteropInstanceVariableWriteNode extends RubyNode {

    @Child private WriteInstanceVariableNode write;
    private final String name;
    private final int labelIndex;

    public InteropInstanceVariableWriteNode(RubyContext context, SourceSection sourceSection, String name, int labelIndex, int valueIndex) {
        super(context, sourceSection);
        this.name = name;
        this.labelIndex = labelIndex;
        this.write = new WriteInstanceVariableNode(context, sourceSection, name, new RubyInteropReceiverNode(context, sourceSection), new RubyInteropArgumentNode(context, sourceSection, valueIndex));
    }

    @Override
    public Object execute(VirtualFrame frame) {
        if (name.equals(ForeignAccess.getArguments(frame).get(labelIndex))) {
            return write.execute(frame);
        } else {
            CompilerDirectives.transferToInterpreter();
            throw new IllegalStateException("Not implemented");
        }
    }
}
