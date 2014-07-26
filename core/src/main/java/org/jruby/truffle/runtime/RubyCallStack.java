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

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.RootCallTarget;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.frame.Frame;
import com.oracle.truffle.api.frame.FrameInstance;
import com.oracle.truffle.api.frame.FrameInstanceVisitor;
import com.oracle.truffle.api.*;
import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.RootNode;
import com.oracle.truffle.api.source.NullSourceSection;
import com.oracle.truffle.api.source.SourceSection;
import org.jruby.truffle.nodes.RubyRootNode;
import org.jruby.truffle.runtime.core.CoreSourceSection;
import org.jruby.truffle.runtime.core.RubyArray;
import org.jruby.truffle.runtime.core.RubyBasicObject;
import org.jruby.truffle.runtime.core.RubyModule;
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
                return getMethod(frameInstance);
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
        final ArrayList<String> callStack = new ArrayList<>();

        final String suffix = "(suffix)";

        if (currentNode != null) {
            callStack.add(formatInLine(currentNode.getEncapsulatingSourceSection(), suffix));
        }

        final FrameInstance currentFrame = Truffle.getRuntime().getCurrentFrame();

        if (currentFrame.getCallNode() != null) {
            callStack.add(formatFromLine(context, currentFrame.getCallNode().getEncapsulatingSourceSection(), currentFrame.getFrame(FrameInstance.FrameAccess.READ_ONLY, false)));
        }

        // TODO: pretty sure putting frame instances on the heap is wrong, but the API will change soon anyway

        final ArrayList<FrameInstance> frameInstances = new ArrayList<>();

        Truffle.getRuntime().iterateFrames(new FrameInstanceVisitor<Void>() {

            @Override
            public Void visitFrame(FrameInstance frameInstance) {
                frameInstances.add(frameInstance);
                return null;
            }

        });

        for (FrameInstance frame : frameInstances) {
            callStack.add(formatFromLine(context, frame.getCallNode().getEncapsulatingSourceSection(), frame.getFrame(FrameInstance.FrameAccess.READ_ONLY, false)));
        }

        return callStack.toArray(new String[callStack.size()]);
    }

    private static String formatInLine(SourceSection sourceSection, String suffix) {
        if (sourceSection instanceof CoreSourceSection) {
            return String.format("core:in `%s': %s", ((CoreSourceSection) sourceSection).getMethodName(), suffix);
        } else {
            return String.format("%s:%d:in `%s': %s", sourceSection.getSource().getName(), sourceSection.getStartLine(), sourceSection.getIdentifier(), suffix);
        }
    }

    private static String formatFromLine(RubyContext context, SourceSection sourceSection, Frame frame) {
        if (sourceSection instanceof CoreSourceSection) {
            return String.format("\tfrom core:in `%s'%s", ((CoreSourceSection) sourceSection).getMethodName(), formatLocals(context, frame));
        } else {
            return String.format("\tfrom %s:%d:in `%s'%s", sourceSection.getSource().getName(), sourceSection.getStartLine(), sourceSection.getIdentifier(), formatLocals(context, frame));
        }
    }

    private static String formatLocals(RubyContext context, Frame frame) {
        final StringBuilder builder = new StringBuilder();
        FrameDescriptor fd = frame.getFrameDescriptor();
        boolean first = true;
        for (Object ident : fd.getIdentifiers()) {
            if (ident instanceof String) {
                RubyBasicObject value = context.getCoreLibrary().box(frame.getValue(fd.findFrameSlot(ident)));
                // TODO(CS): slow path send
                String repr = value.send(null, "inspect", null).toString();
                if (first) {
                    first = false;
                    builder.append(" with ");
                } else {
                    builder.append(", ");
                }
                int maxLength = 12;
                if (repr.length() > maxLength) {
                    repr = repr.substring(0, maxLength) + "... (" + value.getRubyClass().getName() + ")";
                }
                builder.append(ident + " = " + repr);
            }
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

}
