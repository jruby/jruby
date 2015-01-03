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
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.core.RubyString;
import org.jruby.truffle.runtime.core.RubyTime;

import java.text.SimpleDateFormat;

@CoreClass(name = "Time")
public abstract class TimeNodes {

    @CoreMethod(names = "+", required = 1)
    public abstract static class AddNode extends CoreMethodNode {

        public AddNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public AddNode(AddNode prev) {
            super(prev);
        }

        @Specialization
        public double add(RubyTime a, int b) {
            return a.getRealSeconds() + b;
        }

        @Specialization
        public double add(RubyTime a, RubyTime b) {
            return a.getRealSeconds() + b.getRealSeconds();
        }

    }

    @CoreMethod(names = "-", required = 1)
    public abstract static class SubNode extends CoreMethodNode {

        public SubNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public SubNode(SubNode prev) {
            super(prev);
        }

        @Specialization
        public double sub(RubyTime a, double b) {
            return a.getRealSeconds() - b;
        }

        @Specialization
        public double sub(RubyTime a, RubyTime b) {
            return a.getRealSeconds() - b.getRealSeconds();
        }

    }

    @CoreMethod(names = "<", required = 1)
    public abstract static class LessNode extends CoreMethodNode {

        public LessNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public LessNode(LessNode prev) {
            super(prev);
        }

        @Specialization
        public boolean less(RubyTime a, double b) {
            return a.getRealSeconds() < b;
        }

    }

    @CoreMethod(names = "now", onSingleton = true)
    public abstract static class NowNode extends CoreMethodNode {

        public NowNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public NowNode(NowNode prev) {
            super(prev);
        }

        @CompilerDirectives.TruffleBoundary
        @Specialization
        public RubyTime now() {
            return RubyTime.fromDate(getContext().getCoreLibrary().getTimeClass(), System.currentTimeMillis());
        }

    }

    @CoreMethod(names = "from_array", onSingleton = true)
    public abstract static class FromArrayNode extends CoreMethodNode {

        public FromArrayNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public FromArrayNode(FromArrayNode prev) {
            super(prev);
        }

        @CompilerDirectives.TruffleBoundary
        @Specialization
        public RubyTime fromArray(int second,
                                  int minute,
                                  int hour,
                                  int dayOfMonth,
                                  int month,
                                  int year,
                                  int nanoOfSecond,
                                  boolean isdst,
                                  RubyString zone) {
            return RubyTime.fromArray(getContext().getCoreLibrary().getTimeClass(), second, minute, hour, dayOfMonth, month, year, nanoOfSecond, isdst, zone);
        }

    }

    @CoreMethod(names = "to_f")
    public abstract static class ToFNode extends CoreMethodNode {

        public ToFNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public ToFNode(ToFNode prev) {
            super(prev);
        }

        @Specialization
        public double toF(RubyTime time) {
            return time.getRealSeconds();
        }

    }

    @CoreMethod(names = {"to_s", "inspect"})
    public abstract static class ToSNode extends CoreMethodNode {

        public ToSNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public ToSNode(ToSNode prev) {
            super(prev);
        }

        @CompilerDirectives.TruffleBoundary
        @Specialization
        public RubyString toS(RubyTime time) {
            return getContext().makeString(new SimpleDateFormat("Y-MM-d H:m:ss Z").format(time.toDate()));
        }

    }

}
