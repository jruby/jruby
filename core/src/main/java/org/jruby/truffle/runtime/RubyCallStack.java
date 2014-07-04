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
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.NodeUtil;
import com.oracle.truffle.api.nodes.RootNode;
import org.jruby.truffle.nodes.RubyRootNode;
import org.jruby.truffle.runtime.core.RubyModule;
import org.jruby.truffle.runtime.methods.RubyMethod;

import java.util.Iterator;

public abstract class RubyCallStack {

    public static void dump(Node currentNode) {
        CompilerAsserts.neverPartOfCompilation();

        System.err.println("call stack ----------");

        if (currentNode != null) {
            System.err.println("    at " + currentNode.getEncapsulatingSourceSection());
        }

        System.err.println("    in " + Truffle.getRuntime().getCurrentFrame().getCallTarget());

        for (FrameInstance frame : Truffle.getRuntime().getStackTrace()) {
            System.err.println("  from " + frame.getCallNode().getEncapsulatingSourceSection());
        }

        System.err.println("---------------------");
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

    public static String getRubyStacktrace(){
        StringBuilder stack = new StringBuilder();
        for (FrameInstance frame : Truffle.getRuntime().getStackTrace()) {
            stack.append("from " + frame.getCallNode().getEncapsulatingSourceSection() +"\n");
        }
        try {
            return String.format("%s at %s \n %s", getCurrentMethod().getName(), getCurrentMethod().getSharedMethodInfo().getSourceSection().getShortDescription(), stack);
        } catch(UnsupportedOperationException ex) {
            return String.format("(root) at %s:%s", getFilename(), getLineNumber());
        }
    }

    public static String getFilename(){
        return getCallerFrame().getCallNode().getEncapsulatingSourceSection().getSource().getName();
    }

    public static int getLineNumber(){
        return getCallerFrame().getCallNode().getEncapsulatingSourceSection().getStartLine();
    }

}
