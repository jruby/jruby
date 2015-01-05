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

import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.source.SourceSection;
import org.jruby.truffle.runtime.RubyContext;

@CoreClass(name = "Process")
public abstract class ProcessNodes {

    @CoreMethod(names = "pid", isModuleFunction = true)
    public abstract static class PidNode extends CoreMethodNode {

        public PidNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public PidNode(PidNode prev) {
            super(prev);
        }

        @Specialization
        public int pid() {
            notDesignedForCompilation();

            return getContext().getRuntime().getPosix().getpid();
        }

    }

}
