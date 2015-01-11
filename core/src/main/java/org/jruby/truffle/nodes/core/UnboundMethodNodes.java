/*
 * Copyright (c) 2015 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.nodes.core;

import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.source.SourceSection;
import org.jruby.truffle.nodes.yield.YieldDispatchHeadNode;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.UndefinedPlaceholder;
import org.jruby.truffle.runtime.core.RubyBignum;
import org.jruby.truffle.runtime.core.RubyMethod;
import org.jruby.truffle.runtime.core.RubyProc;
import org.jruby.truffle.runtime.core.RubyUnboundMethod;

@CoreClass(name = "UnboundMethod")
public abstract class UnboundMethodNodes {

    @CoreMethod(names = "bind", required = 1)
    public abstract static class BindNode extends CoreMethodNode {

        public BindNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public BindNode(BindNode prev) {
            super(prev);
        }

        @Specialization
        public RubyMethod bind(RubyUnboundMethod unboundMethod, Object object) {
            return new RubyMethod(getContext().getCoreLibrary().getMethodClass(), object, unboundMethod.getMethod());
        }

    }

}
