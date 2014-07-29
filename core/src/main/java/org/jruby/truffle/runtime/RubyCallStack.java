/*
 * Copyright (c) 2014 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.runtime;

import com.oracle.truffle.api.*;
import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.RootNode;
import com.oracle.truffle.api.source.SourceSection;
import org.jruby.truffle.nodes.RubyRootNode;
import org.jruby.truffle.runtime.control.TruffleFatalException;
import org.jruby.truffle.runtime.core.*;
import org.jruby.truffle.runtime.methods.RubyMethod;

import java.util.ArrayList;

public abstract class RubyCallStack {

    public static void dump(RubyContext context, Node currentNode) {
        for (String line : getCallStack(currentNode, context)) {
            System.err.println(line);
        }
    }

    public static FrameInstance getCallerFrame() {
        return Truffle.getRuntime().getCallerFrame();
    }

    public static Frame getCallerFrame(FrameInstance.FrameAccess access, boolean slowPath) {
        return getCallerFrame().getFrame(access, slowPath);
    }

    public static RubyMethod getCurrentMethod() {
        CompilerAsserts.neverPartOfCompilation();

        RubyMethod method;

        final FrameInstance currentFrame = Truffle.getRuntime().getCurrentFrame();

        method = getMethod(currentFrame);

        if (method != null) {
            return method;
        }

        method = Truffle.getRuntime().iterateFrames(new FrameInstanceVisitor<RubyMethod>() {
            @Override
            public RubyMethod visitFrame(FrameInstance frameInstance) {
                final RubyMethod method = getMethod(frameInstance);

                if (method == null) {
                    return null;
                } else {
                    return method;
                }
            }
        });

        if (method != null) {
            return method;
        }

        throw new UnsupportedOperationException();
    }

    private static RubyMethod getMethod(FrameInstance frame) {
        CompilerAsserts.neverPartOfCompilation();

        if (frame == null) {
            return null;
        }

        final CallTarget callTarget = frame.getCallTarget();

        if (!(callTarget instanceof RootCallTarget)) {
            return null;
        }

        final RootCallTarget rootCallTarget = (RootCallTarget) callTarget;

        final RootNode rootNode = rootCallTarget.getRootNode();

        if (!(rootNode instanceof RubyRootNode)) {
            return null;
        }

        final RubyRootNode rubyRootNode = (RubyRootNode) rootNode;

        return RubyMethod.getMethod(rubyRootNode.getSharedMethodInfo());
    }


    public static RubyModule getCurrentDeclaringModule() {
        CompilerAsserts.neverPartOfCompilation();

        return getCurrentMethod().getDeclaringModule();
    }

    public static String getFilename(){
        return getCallerFrame().getCallNode().getEncapsulatingSourceSection().getSource().getName();
    }

    public static int getLineNumber(){
        return getCallerFrame().getCallNode().getEncapsulatingSourceSection().getStartLine();
    }

    public static String[] getCallStack(Node currentNode, RubyContext context) {
        try {
            final ArrayList<String> callStack = new ArrayList<>();

            final String suffix = "(suffix)";

            final CallStackFrame[] materializedCallStackFrames = materializeCallStackFrames();

            if (currentNode != null) {
                callStack.add(formatInLine(currentNode.getEncapsulatingSourceSection(), suffix));
            }

            for (CallStackFrame frame : materializedCallStackFrames) {
                if (frame.getCallNode() != null) {
                    callStack.add(formatFromLine(context, frame.getCallNode().getEncapsulatingSourceSection(), frame.getFrame()));
                }
            }

            return callStack.toArray(new String[callStack.size()]);
        } catch (Exception e) {
            throw new TruffleFatalException("Exception while trying to build Ruby call stack", e);
        }
    }

    private static CallStackFrame[] materializeCallStackFrames() {
        final ArrayList<CallStackFrame> frames = new ArrayList<>();

        final FrameInstance currentFrame = Truffle.getRuntime().getCurrentFrame();

        try {
            frames.add(new CallStackFrame(currentFrame.getCallNode(), currentFrame.getFrame(FrameInstance.FrameAccess.MATERIALIZE, true)));
        } catch (IndexOutOfBoundsException e) {
            // TODO(CS): what causes this error?
        }

        Truffle.getRuntime().iterateFrames(new FrameInstanceVisitor<Void>() {

            @Override
            public Void visitFrame(FrameInstance frameInstance) {
                frames.add(new CallStackFrame(frameInstance.getCallNode(), frameInstance.getFrame(FrameInstance.FrameAccess.MATERIALIZE, true)));
                return null;
            }

        });

        return frames.toArray(new CallStackFrame[frames.size()]);
    }

    private static String formatInLine(SourceSection sourceSection, String suffix) {
        if (sourceSection instanceof CoreSourceSection) {
            final CoreSourceSection coreSourceSection = ((CoreSourceSection) sourceSection);
            return String.format("in %s#%s: %s", coreSourceSection.getClassName(), coreSourceSection.getMethodName(), suffix);
        } else {
            return String.format("%s:%d:in : %s", sourceSection.getSource().getName(), sourceSection.getStartLine(), sourceSection.getIdentifier(), suffix);
        }
    }

    private static String formatFromLine(RubyContext context, SourceSection sourceSection, Frame frame) {
        if (sourceSection instanceof CoreSourceSection) {
            final CoreSourceSection coreSourceSection = ((CoreSourceSection) sourceSection);
            return String.format("\tin %s#%s", coreSourceSection.getClassName(), coreSourceSection.getMethodName(), RubyContext.BACKTRACE_PRINT_LOCALS ? formatLocals(context, frame) : "");
        } else {
            return String.format("\tfrom %s:%d:in `%s'%s", sourceSection.getSource().getName(), sourceSection.getStartLine(), sourceSection.getIdentifier(), RubyContext.BACKTRACE_PRINT_LOCALS ? formatLocals(context, frame) : "");
        }
    }

    private static String formatLocals(RubyContext context, Frame frame) {
        final StringBuilder builder = new StringBuilder();
        final FrameDescriptor fd = frame.getFrameDescriptor();

        builder.append(" with {self: ");
        builder.append(formatLocalValue(context, RubyArguments.getSelf(frame.getArguments())));

        for (Object identifier : fd.getIdentifiers()) {
            if (identifier instanceof String) {
                builder.append(", ");
                builder.append(identifier);
                builder.append(": ");
                builder.append(formatLocalValue(context, frame.getValue(fd.findFrameSlot(identifier))));
            }
        }

        if (builder.length() > 0) {
            builder.append("}");
        }

        return builder.toString();
    }

    private static String formatLocalValue(RubyContext context, Object value) {
        try {
            // TODO(CS): slow path send
            final String inspected = context.getCoreLibrary().box(value).send(null, "inspect", null).toString();

            if (inspected.length() <= RubyContext.BACKTRACE_PRINT_LOCALS_MAX) {
                return inspected;
            } else {
                return inspected.substring(0, RubyContext.BACKTRACE_PRINT_LOCALS_MAX) + "...";
            }
        } catch (Exception e) {
            if (RubyContext.EXCEPTIONS_PRINT_JAVA) {
                e.printStackTrace();
            }
            return "<exception>";
        }
    }

    public static RubyArray getCallStackAsRubyArray(RubyContext context, Node currentNode) {
        final String[] callStack = getCallStack(currentNode, context);

        final Object[] callStackAsRubyString = new Object[callStack.length];

        for (int n = 0;n < callStack.length; n++) {
            callStackAsRubyString[n] = context.makeString(callStack[n]);
        }

        return RubyArray.fromObjects(context.getCoreLibrary().getArrayClass(), callStackAsRubyString);
    }

    private static class CallStackFrame {

        private final Node callNode;
        private final Frame frame;

        public CallStackFrame(Node callNode, Frame frame) {
            this.callNode = callNode;
            this.frame = frame;
        }

        public Node getCallNode() {
            return callNode;
        }

        public Frame getFrame() {
            return frame;
        }

    }

}
