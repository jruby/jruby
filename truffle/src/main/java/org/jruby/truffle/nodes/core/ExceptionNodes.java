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

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.DynamicObjectFactory;
import com.oracle.truffle.api.source.SourceSection;
import org.jruby.truffle.nodes.RubyGuards;
import org.jruby.truffle.nodes.core.array.ArrayNodes;
import org.jruby.truffle.nodes.objects.Allocator;
import org.jruby.truffle.nodes.objectstorage.ReadHeadObjectFieldNode;
import org.jruby.truffle.om.dsl.api.Layout;
import org.jruby.truffle.om.dsl.api.Nullable;
import org.jruby.truffle.runtime.NotProvided;
import org.jruby.truffle.runtime.RubyCallStack;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.backtrace.Activation;
import org.jruby.truffle.runtime.backtrace.Backtrace;
import org.jruby.truffle.runtime.backtrace.MRIBacktraceFormatter;

import java.util.List;

@CoreClass(name = "Exception")
public abstract class ExceptionNodes {

    @Layout
    public interface ExceptionLayout extends BasicObjectNodes.BasicObjectLayout {

        DynamicObjectFactory createExceptionShape(DynamicObject logicalClass, DynamicObject metaClass);

        DynamicObject createException(DynamicObjectFactory factory, @Nullable Object message, @Nullable Backtrace backtrace);

        boolean isException(DynamicObject object);

        @Nullable
        Object getMessage(DynamicObject object);

        @Nullable
        void setMessage(DynamicObject object, Object message);

        @Nullable
        Backtrace getBacktrace(DynamicObject object);

        @Nullable
        void setBacktrace(DynamicObject object, Backtrace backtrace);

    }

    public static final ExceptionLayout EXCEPTION_LAYOUT = ExceptionLayoutImpl.INSTANCE;

    public static class BacktraceFormatter extends MRIBacktraceFormatter {
        @Override
        protected String formatFromLine(List<Activation> activations, int n) {
            return formatCallerLine(activations, n);
        }
    }

    public static final BacktraceFormatter BACKTRACE_FORMATTER = new BacktraceFormatter();

    // TODO (eregon 16 Apr. 2015): MRI does a dynamic calls to "message"
    public static Object getMessage(DynamicObject exception) {
        return EXCEPTION_LAYOUT.getMessage(exception);
    }

    public static Backtrace getBacktrace(DynamicObject exception) {
        return EXCEPTION_LAYOUT.getBacktrace(exception);
    }

    public static void setBacktrace(DynamicObject exception, Backtrace backtrace) {
        EXCEPTION_LAYOUT.setBacktrace(exception, backtrace);
    }

    @TruffleBoundary
    public static DynamicObject asRubyStringArray(DynamicObject exception) {
        assert RubyGuards.isRubyException(exception);

        assert getBacktrace(exception) != null;
        final String[] lines = BACKTRACE_FORMATTER.format(BasicObjectNodes.getContext(exception), exception, getBacktrace(exception));

        final Object[] array = new Object[lines.length];

        for (int n = 0;n < lines.length; n++) {
            array[n] = StringNodes.createString(BasicObjectNodes.getContext(exception).getCoreLibrary().getStringClass(), lines[n]);
        }

        return ArrayNodes.fromObjects(BasicObjectNodes.getContext(exception).getCoreLibrary().getArrayClass(), array);
    }

    public static void setMessage(DynamicObject exception, Object message) {
        EXCEPTION_LAYOUT.setMessage(exception, message);
    }

    public static DynamicObject createRubyException(DynamicObject rubyClass) {
        return EXCEPTION_LAYOUT.createException(ModuleNodes.getModel(rubyClass).factory, null, null);
    }

    public static DynamicObject createRubyException(DynamicObject rubyClass, Object message, Backtrace backtrace) {
        return EXCEPTION_LAYOUT.createException(ModuleNodes.getModel(rubyClass).factory, message, backtrace);
    }

    @CoreMethod(names = "initialize", optional = 1)
    public abstract static class InitializeNode extends CoreMethodArrayArgumentsNode {

        public InitializeNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public DynamicObject initialize(DynamicObject exception, NotProvided message) {
            setMessage(exception, nil());
            return exception;
        }

        @Specialization(guards = "wasProvided(message)")
        public DynamicObject initialize(DynamicObject exception, Object message) {
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
        public Object backtrace(DynamicObject exception) {
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
        public DynamicObject captureBacktrace(DynamicObject exception, NotProvided offset) {
            return captureBacktrace(exception, 1);
        }

        @Specialization
        public DynamicObject captureBacktrace(DynamicObject exception, int offset) {
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
        public Object message(DynamicObject exception) {
            return getMessage(exception);
        }

    }

    public static class ExceptionAllocator implements Allocator {

        @Override
        public DynamicObject allocate(RubyContext context, DynamicObject rubyClass, Node currentNode) {
            return createRubyException(rubyClass);
        }

    }
}
