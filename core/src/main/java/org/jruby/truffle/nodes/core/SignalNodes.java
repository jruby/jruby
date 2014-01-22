/*
 * Copyright (c) 2013 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.nodes.core;

import com.oracle.truffle.api.*;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.dsl.*;
import org.jruby.common.IRubyWarnings;
import org.jruby.truffle.runtime.*;

@CoreClass(name = "Signal")
public abstract class SignalNodes {

    @CoreMethod(names = "trap", isModuleMethod = true, needsSelf = false, appendCallNode = true, minArgs = 2, maxArgs = 2)
    public abstract static class SignalNode extends CoreMethodNode {

        public SignalNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public SignalNode(SignalNode prev) {
            super(prev);
        }

        @Specialization
        public NilPlaceholder trap(@SuppressWarnings("unused") Object signal, Node callNode) {
            getContext().getRuntime().getWarnings().warn(IRubyWarnings.ID.TRUFFLE, callNode.getSourceSection().getSource().getName(), callNode.getSourceSection().getStartLine(), "Signal#trap doesn't do anything");
            return NilPlaceholder.INSTANCE;
        }

    }

}
