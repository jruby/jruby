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
import com.oracle.truffle.api.dsl.*;
import org.jruby.truffle.runtime.*;
import org.jruby.truffle.runtime.core.*;

import java.text.SimpleDateFormat;

@CoreClass(name = "Time")
public abstract class TimeNodes {

    @CoreMethod(names = "-", minArgs = 1, maxArgs = 1)
    public abstract static class SubNode extends CoreMethodNode {

        public SubNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public SubNode(SubNode prev) {
            super(prev);
        }

        @Specialization
        public double sub(RubyTime a, RubyTime b) {
            return RubyTime.nanosecondsToSecond(a.nanoseconds - b.nanoseconds);
        }

    }

    @CoreMethod(names = "now", isModuleMethod = true, needsSelf = false, maxArgs = 0)
    public abstract static class NowNode extends CoreMethodNode {

        public NowNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public NowNode(NowNode prev) {
            super(prev);
        }

        @Specialization
        public RubyTime now() {
            return RubyTime.fromDate(getContext().getCoreLibrary().getTimeClass(), System.currentTimeMillis());
        }

    }

    @CoreMethod(names = {"to_s", "inspect"}, maxArgs = 0)
    public abstract static class ToSNode extends CoreMethodNode {

        public ToSNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public ToSNode(ToSNode prev) {
            super(prev);
        }

        @Specialization
        public RubyString toS(RubyTime time) {
            return getContext().makeString(new SimpleDateFormat("Y-MM-d H:m:ss Z").format(time.toDate()));
        }

    }

}
