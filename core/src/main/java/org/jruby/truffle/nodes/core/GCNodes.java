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

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.source.SourceSection;
import org.jruby.truffle.runtime.core.*;
import org.jruby.truffle.runtime.RubyContext;

@CoreClass(name = "GC")
public abstract class GCNodes {

    @CoreMethod(names = "start", onSingleton = true)
    public abstract static class StartNode extends GarbageCollectNode {
        public StartNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public StartNode(StartNode prev) {
            super(prev);
        }
    }

    @CoreMethod(names = "garbage_collect", needsSelf = false)
    public abstract static class GarbageCollectNode extends CoreMethodNode {

        public GarbageCollectNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public GarbageCollectNode(GarbageCollectNode prev) {
            super(prev);
        }

        public abstract RubyNilClass executeGC();

        @Specialization
        public RubyNilClass garbageCollect() {
            return doGC();
        }

        @CompilerDirectives.TruffleBoundary
        private RubyNilClass doGC() {
            notDesignedForCompilation();

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
