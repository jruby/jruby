/*
 * Copyright (c) 2013, 2014 Oracle and/or its affiliates. All rights reserved. This
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
import org.jruby.truffle.nodes.RubyNode;
import org.jruby.truffle.nodes.dispatch.DispatchHeadNode;
import org.jruby.truffle.runtime.*;
import org.jruby.truffle.runtime.core.*;

@CoreClass(name = "main")
public abstract class MainNodes {

    @CoreMethod(names = "include", isSplatted = true, needsSelf = false, minArgs = 1)
    public abstract static class IncludeNode extends CoreMethodNode {

        @Child protected ModuleNodes.IncludeNode includeNode;

        public IncludeNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            includeNode = ModuleNodesFactory.IncludeNodeFactory.create(context, sourceSection, new RubyNode[]{null, null});
        }

        public IncludeNode(IncludeNode prev) {
            super(prev);
            includeNode = prev.includeNode;
        }

        @Specialization
        public RubyNilClass include(VirtualFrame frame, Object[] args) {
            notDesignedForCompilation();
            final RubyClass object = getContext().getCoreLibrary().getObjectClass();
            return includeNode.executeInclude(frame, object, args);
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
