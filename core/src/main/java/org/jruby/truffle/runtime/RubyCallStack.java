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
import org.jruby.truffle.runtime.backtrace.Activation;
import org.jruby.truffle.runtime.backtrace.Backtrace;
import org.jruby.truffle.runtime.core.*;
import org.jruby.truffle.runtime.methods.RubyMethod;

import java.util.ArrayList;

public abstract class RubyCallStack {

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
        return Truffle.getRuntime().getCallerFrame().getCallNode().getEncapsulatingSourceSection().getSource().getName();
    }

    public static int getLineNumber(){
        return Truffle.getRuntime().getCallerFrame().getCallNode().getEncapsulatingSourceSection().getStartLine();
    }

    public static Backtrace getBacktrace(Node currentNode) {
        final ArrayList<Activation> activations = new ArrayList<>();

        activations.add(new Activation(getCurrentMethod(), currentNode, Truffle.getRuntime().getCurrentFrame().getFrame(FrameInstance.FrameAccess.MATERIALIZE, false).materialize()));

        Truffle.getRuntime().iterateFrames(new FrameInstanceVisitor<RubyMethod>() {

            @Override
            public RubyMethod visitFrame(FrameInstance frameInstance) {
                activations.add(new Activation(getMethod(frameInstance), frameInstance.getCallNode(),
                        frameInstance.getFrame(FrameInstance.FrameAccess.MATERIALIZE, false).materialize()));
                return null;
            }

        });

        return new Backtrace(activations.toArray(new Activation[activations.size()]));
    }

}
