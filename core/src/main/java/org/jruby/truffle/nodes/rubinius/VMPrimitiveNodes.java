/*
 * Copyright (c) 2015 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.nodes.rubinius;

import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.source.SourceSection;
import com.oracle.truffle.api.utilities.ConditionProfile;

import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.core.RubyNilClass;
import org.jruby.truffle.runtime.core.RubyString;
import org.jruby.truffle.runtime.core.RubyThread;
import org.jruby.util.StringSupport;

/**
 * Rubinius primitives associated with the VM.
 */
public abstract class VMPrimitiveNodes {

    @RubiniusPrimitive(name = "vm_gc_start", needsSelf = false)
    public static abstract class VMGCStartPrimitiveNode extends RubiniusPrimitiveNode {

        public VMGCStartPrimitiveNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public VMGCStartPrimitiveNode(VMGCStartPrimitiveNode prev) {
            super(prev);
        }

        @Specialization
        public RubyNilClass vmGCStart() {
            final RubyThread runningThread = getContext().getThreadManager().leaveGlobalLock();

            try {
                System.gc();
            } finally {
                getContext().getThreadManager().enterGlobalLock(runningThread);
            }

            return getContext().getCoreLibrary().getNilObject();
        }

    }

}
