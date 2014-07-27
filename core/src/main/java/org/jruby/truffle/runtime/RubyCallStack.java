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
import org.jruby.truffle.nodes.RubyRootNode;
import org.jruby.truffle.runtime.control.TruffleFatalException;
import org.jruby.truffle.runtime.core.*;
import org.jruby.truffle.runtime.methods.RubyMethod;

import java.util.ArrayList;
import java.util.Iterator;

public abstract class RubyCallStack {

    public static void dump(RubyContext context, Node currentNode) {
        for (String line : getCallStack(currentNode, context)) {
            System.err.println(line);
        }
    }

    public static FrameInstance getCallerFrame() {
        final Iterable<FrameInstance> stackIterable = Truffle.getRuntime().getStackTrace();
        assert stackIterable != null;

        final Iterator<FrameInstance> stack = stackIterable.iterator();

        if (stack.hasNext()) {
            return stack.next();
        } else {
            return null;
        }
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

        for (FrameInstance frame : Truffle.getRuntime().getStackTrace()) {
            method = getMethod(frame);

            if (method != null) {
                return method;
            }
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

            final MaterializedCallStackFrame[] materializedCallStackFrames = materializeCallStackFrames();

            if (currentNode != null) {
                callStack.add(formatInLine(currentNode.getEncapsulatingSourceSection(), suffix));
            }

            for (MaterializedCallStackFrame frame : materializedCallStackFrames) {
                callStack.add(formatFromLine(context, frame.getCallNode().getEncapsulatingSourceSection(), frame.getMaterializedFrame()));
            }

            return callStack.toArray(new String[callStack.size()]);
        } catch (Exception e) {
            throw new TruffleFatalException("Exception while trying to build Ruby call stack", e);
        }
    }

    private static MaterializedCallStackFrame[] materializeCallStackFrames() {
        final ArrayList<MaterializedCallStackFrame> frames = new ArrayList<>();

        final FrameInstance currentFrame = Truffle.getRuntime().getCurrentFrame();

        try {
            frames.add(new MaterializedCallStackFrame(currentFrame.getCallNode(), (MaterializedFrame) currentFrame.getFrame(FrameInstance.FrameAccess.MATERIALIZE, false)));
        } catch (IndexOutOfBoundsException e) {
            // TODO(CS): what causes this error?
        }

        for (FrameInstance frame : Truffle.getRuntime().getStackTrace()) {
            frames.add(new MaterializedCallStackFrame(frame.getCallNode(), (MaterializedFrame) frame.getFrame(FrameInstance.FrameAccess.MATERIALIZE, false)));
        }

        return frames.toArray(new MaterializedCallStackFrame[frames.size()]);
    }

    private static String formatInLine(SourceSection sourceSection, String suffix) {
        return String.format("%s:%d:in `%s': %s", sourceSection.getSource().getName(), sourceSection.getStartLine(), sourceSection.getIdentifier(), suffix);
    }

    private static String formatFromLine(RubyContext context, SourceSection sourceSection, Frame frame) {
        return String.format("\tfrom %s:%d:in `%s'%s", sourceSection.getSource().getName(), sourceSection.getStartLine(), sourceSection.getIdentifier(), RubyContext.BACKTRACE_PRINT_LOCALS ? formatLocals(context, frame) : "");
    }

    private static String formatLocals(RubyContext context, Frame frame) {
        final StringBuilder builder = new StringBuilder();
        FrameDescriptor fd = frame.getFrameDescriptor();
        boolean first = true;
        for (Object ident : fd.getIdentifiers()) {
            if (ident instanceof String) {
                RubyBasicObject value = context.getCoreLibrary().box(frame.getValue(fd.findFrameSlot(ident)));
                String repr;
                try {
                    // TODO(CS): slow path send
                    repr = value.send(null, "inspect", null).toString();
                } catch (Exception e) {
                    if (RubyContext.EXCEPTIONS_PRINT_JAVA) {
                        e.printStackTrace();
                    }

                    repr = "<exception>";
                }
                if (first) {
                    first = false;
                    builder.append(" with {");
                } else {
                    builder.append(", ");
                }
                if (repr.length() > RubyContext.BACKTRACE_PRINT_LOCALS_MAX) {
                    repr = repr.substring(0, RubyContext.BACKTRACE_PRINT_LOCALS_MAX) + "...";
                }
                builder.append(ident + ": " + repr);
            }
        }

        if (builder.length() > 0) {
            builder.append("}");
        }

        return builder.toString();
    }

    public static RubyArray getCallStackAsRubyArray(RubyContext context, Node currentNode) {
        final String[] callStack = getCallStack(currentNode, context);

        final Object[] callStackAsRubyString = new Object[callStack.length];

        for (int n = 0;n < callStack.length; n++) {
            callStackAsRubyString[n] = context.makeString(callStack[n]);
        }

        return RubyArray.fromObjects(context.getCoreLibrary().getArrayClass(), callStackAsRubyString);
    }

    private static class MaterializedCallStackFrame {

        private final Node callNode;
        private final MaterializedFrame materializedFrame;

        public MaterializedCallStackFrame(Node callNode, MaterializedFrame materializedFrame) {
            this.callNode = callNode;
            this.materializedFrame = materializedFrame;
        }

        public Node getCallNode() {
            return callNode;
        }

        public MaterializedFrame getMaterializedFrame() {
            return materializedFrame;
        }

    }

}
