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
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.source.SourceSection;
import org.jruby.truffle.nodes.core.array.ArrayNodes;
import org.jruby.truffle.nodes.objects.Allocator;
import org.jruby.truffle.nodes.objectstorage.ReadHeadObjectFieldNode;
import org.jruby.truffle.runtime.NotProvided;
import org.jruby.truffle.runtime.RubyCallStack;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.backtrace.Backtrace;
import org.jruby.truffle.runtime.core.RubyBasicObject;
import org.jruby.truffle.runtime.core.RubyClass;
import org.jruby.truffle.runtime.core.RubyException;

@CoreClass(name = "Exception")
public abstract class ExceptionNodes {

    // TODO (eregon 16 Apr. 2015): MRI does a dynamic calls to "message"
    public static Object getMessage(RubyException exception) {
        return exception.message;
    }

    public static Backtrace getBacktrace(RubyException exception) {
        return exception.backtrace;
    }

    public static void setBacktrace(RubyException exception, Backtrace backtrace) {
        exception.backtrace = backtrace;
    }

    public static RubyBasicObject asRubyStringArray(RubyException exception) {
        assert getBacktrace(exception) != null;
        final String[] lines = Backtrace.EXCEPTION_FORMATTER.format(exception.getContext(), exception, getBacktrace(exception));

        final Object[] array = new Object[lines.length];

        for (int n = 0;n < lines.length; n++) {
            array[n] = StringNodes.createString(exception.getContext().getCoreLibrary().getStringClass(), lines[n]);
        }

        return ArrayNodes.fromObjects(exception.getContext().getCoreLibrary().getArrayClass(), array);
    }

    public static void setMessage(RubyException exception, Object message) {
        exception.message = message;
    }

    public static RubyException createRubyException(RubyClass rubyClass) {
        return new RubyException(rubyClass);
    }

    public static RubyException createRubyException(RubyClass rubyClass, Object message, Backtrace backtrace) {
        return new RubyException(rubyClass, message, backtrace);
    }

    @CoreMethod(names = "initialize", optional = 1)
    public abstract static class InitializeNode extends CoreMethodArrayArgumentsNode {

        public InitializeNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public RubyBasicObject initialize(RubyException exception, NotProvided message) {
            setMessage(exception, nil());
            return exception;
        }

        @Specialization(guards = "wasProvided(message)")
        public RubyBasicObject initialize(RubyException exception, Object message) {
            setMessage(exception, message);
            return exception;
        }

    }

    @CoreMethod(names = "backtrace")
    public abstract static class BacktraceNode extends CoreMethodArrayArgumentsNode {

        @Child ReadHeadObjectFieldNode readCustomBacktrace;

        public BacktraceNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            readCustomBacktrace = new ReadHeadObjectFieldNode("@custom_backtrace");
        }

        @Specialization
        public Object backtrace(RubyException exception) {
            if (readCustomBacktrace.isSet(exception)) {
                return readCustomBacktrace.execute(exception);
            } else if (getBacktrace(exception) != null) {
                return asRubyStringArray(exception);
            } else {
                return nil();
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
        public RubyBasicObject captureBacktrace(RubyException exception, NotProvided offset) {
            return captureBacktrace(exception, 1);
        }

        @Specialization
        public RubyBasicObject captureBacktrace(RubyException exception, int offset) {
            Backtrace backtrace = RubyCallStack.getBacktrace(this, offset);
            setBacktrace(exception, backtrace);
            return nil();
        }

    }

    @CoreMethod(names = "message")
    public abstract static class MessageNode extends CoreMethodArrayArgumentsNode {

        public MessageNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public Object message(RubyException exception) {
            return getMessage(exception);
        }

    }

    public static class ExceptionAllocator implements Allocator {

        @Override
        public RubyBasicObject allocate(RubyContext context, RubyClass rubyClass, Node currentNode) {
            return createRubyException(rubyClass);
        }

    }
}
