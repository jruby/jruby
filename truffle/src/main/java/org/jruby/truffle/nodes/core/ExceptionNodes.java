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

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.source.SourceSection;
import org.jruby.truffle.runtime.RubyCallStack;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.UndefinedPlaceholder;
import org.jruby.truffle.runtime.backtrace.Backtrace;
import org.jruby.truffle.runtime.core.RubyBasicObject;
import org.jruby.truffle.runtime.core.RubyException;
import org.jruby.truffle.runtime.core.RubyString;

@CoreClass(name = "Exception")
public abstract class ExceptionNodes {

    @CoreMethod(names = "initialize", optional = 1)
    public abstract static class InitializeNode extends CoreMethodArrayArgumentsNode {

        public InitializeNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public RubyBasicObject initialize(RubyException exception, UndefinedPlaceholder message) {
            CompilerDirectives.transferToInterpreter();

            exception.initialize(getContext().makeString(""));
            return nil();
        }

        @Specialization
        public RubyBasicObject initialize(RubyException exception, RubyString message) {
            CompilerDirectives.transferToInterpreter();

            exception.initialize(message);
            return nil();
        }

    }

    @CoreMethod(names = "backtrace")
    public abstract static class BacktraceNode extends CoreMethodArrayArgumentsNode {

        public BacktraceNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public Object backtrace(RubyException exception) {
            if (exception.getBacktrace() == null) {
                return nil();
            } else {
                return exception.asRubyStringArray();
            }
        }

    }

    @RubiniusOnly
    @CoreMethod(names = "capture_backtrace!", optional = 1)
    public abstract static class CaptureBacktraceNode extends CoreMethodArrayArgumentsNode {

        public CaptureBacktraceNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public RubyBasicObject captureBacktrace(RubyException exception, UndefinedPlaceholder offset) {
            return captureBacktrace(exception, 1);
        }

        @Specialization
        public RubyBasicObject captureBacktrace(RubyException exception, int offset) {
            Backtrace backtrace = RubyCallStack.getBacktrace(this, offset);
            exception.setBacktrace(backtrace);
            return nil();
        }

    }

    @CoreMethod(names = "message")
    public abstract static class MessageNode extends CoreMethodArrayArgumentsNode {

        public MessageNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public RubyString message(RubyException exception) {
            return exception.getMessage();
        }

    }

    @CoreMethod(names = "to_s")
    public abstract static class ToSNode extends CoreMethodArrayArgumentsNode {

        public ToSNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public RubyString toS(RubyException exception) {
            if (exception.getMessage().length() == 0) {
                return getContext().makeString(exception.getLogicalClass().getName());
            } else {
                return getContext().makeString(exception.getMessage().getByteList());
            }
        }

    }

}
