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

import com.oracle.truffle.api.source.*;
import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.api.frame.VirtualFrame;
import org.jruby.truffle.nodes.dispatch.DispatchHeadNode;
import org.jruby.truffle.runtime.*;
import org.jruby.truffle.runtime.core.*;

@CoreClass(name = "main")
public abstract class MainNodes {

    @CoreMethod(names = "include", isSplatted = true, minArgs = 1)
    public abstract static class IncludeNode extends CoreMethodNode {

        @Child protected DispatchHeadNode includeNode;

        public IncludeNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            includeNode = new DispatchHeadNode(context);

        }

        public IncludeNode(IncludeNode prev) {
            super(prev);
            includeNode = prev.includeNode;
        }

        @Specialization
        public NilPlaceholder include(VirtualFrame frame, Object[] args) {
            notDesignedForCompilation();

            RubyClass object = getContext().getCoreLibrary().getObjectClass();

            // TODO: MRI does this call statically
            return (NilPlaceholder) includeNode.call(frame, object, "include", null, args);
        }
    }

    @CoreMethod(names = "to_s", needsSelf = false, maxArgs = 0)
    public abstract static class ToSNode extends CoreMethodNode {

        public ToSNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public ToSNode(ToSNode prev) {
            super(prev);
        }

        @Specialization
        public RubyString toS() {
            return getContext().makeString("main");
        }

    }

}
