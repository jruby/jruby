/*
 * Copyright (c) 2013, 2015 Oracle and/or its affiliates. All rights reserved. This
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
import org.jruby.truffle.runtime.RubyCallStack;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.UndefinedPlaceholder;
import org.jruby.truffle.runtime.core.RubyArray;
import org.jruby.truffle.runtime.core.RubyException;
import org.jruby.truffle.runtime.core.RubyNilClass;
import org.jruby.truffle.runtime.core.RubyString;

@CoreClass(name = "Exception")
public abstract class ExceptionNodes {

    @CoreMethod(names = "initialize", optional = 1)
    public abstract static class InitializeNode extends CoreMethodNode {

        public InitializeNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public InitializeNode(InitializeNode prev) {
            super(prev);
        }

        @Specialization
        public RubyNilClass initialize(RubyException exception, UndefinedPlaceholder message) {
            notDesignedForCompilation();

            exception.initialize(getContext().makeString(" "), RubyCallStack.getBacktrace(this));
            return nil();
        }

        @Specialization
        public RubyNilClass initialize(RubyException exception, RubyString message) {
            notDesignedForCompilation();

            exception.initialize(message, RubyCallStack.getBacktrace(this));
            return nil();
        }

    }

    @CoreMethod(names = "backtrace")
    public abstract static class BacktraceNode extends CoreMethodNode {

        public BacktraceNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public BacktraceNode(BacktraceNode prev) {
            super(prev);
        }

        @Specialization
        public RubyArray backtrace(RubyException exception) {
            return exception.asRubyStringArray();
        }

    }

    @CoreMethod(names = "message")
    public abstract static class MessageNode extends CoreMethodNode {

        public MessageNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public MessageNode(MessageNode prev) {
            super(prev);
        }

        @Specialization
        public RubyString message(RubyException exception) {
            return exception.getMessage();
        }

    }

    @CoreMethod(names = "to_s")
    public abstract static class ToSNode extends CoreMethodNode {

        public ToSNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public ToSNode(ToSNode prev) {
            super(prev);
        }

        @Specialization
        public RubyString toS(RubyException exception) {
            return getContext().makeString(exception.getLogicalClass().getName());
        }

    }

}
