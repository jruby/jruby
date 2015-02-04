/*
 * Copyright (c) 2014, 2015 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.runtime;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.frame.FrameInstance;
import com.oracle.truffle.api.frame.FrameInstanceVisitor;
import com.oracle.truffle.api.nodes.Node;
import org.jruby.truffle.nodes.CoreSourceSection;
import org.jruby.truffle.runtime.backtrace.Activation;
import org.jruby.truffle.runtime.backtrace.Backtrace;
import org.jruby.truffle.runtime.core.RubyModule;
import org.jruby.truffle.runtime.methods.InternalMethod;
import org.jruby.truffle.runtime.methods.MethodLike;
import org.jruby.util.Memo;
import org.jruby.util.cli.Options;

import java.util.ArrayList;

public abstract class RubyCallStack {

    /** Called "cref" in MRI. */
    public static RubyModule getCurrentDeclaringModule() {
        final FrameInstance currentFrame = Truffle.getRuntime().getCurrentFrame();
        final MethodLike method = getMethod(currentFrame);
        return method.getDeclaringModule();
    }

    public static InternalMethod getCallingMethod() {
        CompilerAsserts.neverPartOfCompilation();

        final Memo<Boolean> seenCurrent = new Memo<Boolean>();

        MethodLike method;

        final FrameInstance currentFrame = Truffle.getRuntime().getCurrentFrame();

        method = getMethod(currentFrame);

        if (method instanceof InternalMethod) {
            seenCurrent.set(true);
        }

        return Truffle.getRuntime().iterateFrames(new FrameInstanceVisitor<InternalMethod>() {

            @Override
            public InternalMethod visitFrame(FrameInstance frameInstance) {
                final MethodLike maybeMethod = getMethod(frameInstance);

                if (maybeMethod instanceof InternalMethod) {
                    if (seenCurrent.get()) {
                        return (InternalMethod) maybeMethod;
                    } else {
                        seenCurrent.set(true);
                        return null;
                    }
                } else {
                    return null;
                }
            }

        });
    }

    public static MethodLike getMethod(FrameInstance frame) {
        CompilerAsserts.neverPartOfCompilation();

        if (frame == null) {
            return null;
        }

        return RubyArguments.getMethod(frame.getFrame(FrameInstance.FrameAccess.READ_ONLY, true).getArguments());
    }

    public static Backtrace getBacktrace(Node currentNode) {
        CompilerAsserts.neverPartOfCompilation();

        final ArrayList<Activation> activations = new ArrayList<>();

        if (Options.TRUFFLE_BACKTRACE_GENERATE.load()) {
            /*
             * TODO(cs): if this materializing the frames proves really expensive
             * we might want to make it optional - I think it's only used for some
             * features beyond what MRI does like printing locals in backtraces.
             */

            if (currentNode != null && Truffle.getRuntime().getCurrentFrame() != null) {
                activations.add(new Activation(currentNode, Truffle.getRuntime().getCurrentFrame().getFrame(FrameInstance.FrameAccess.MATERIALIZE, true).materialize()));
            }

            Truffle.getRuntime().iterateFrames(new FrameInstanceVisitor<InternalMethod>() {

                @Override
                public InternalMethod visitFrame(FrameInstance frameInstance) {
                    // Multiple top level methods (require) introduce null call nodes - ignore them
                    
                    if (frameInstance.getCallNode() != null) {
                        activations.add(new Activation(frameInstance.getCallNode(),
                                frameInstance.getFrame(FrameInstance.FrameAccess.MATERIALIZE, true).materialize()));
                    }

                    return null;
                }

            });

        }

        return new Backtrace(activations.toArray(new Activation[activations.size()]));
    }

    public static Node getTopMostUserCallNode() {
        CompilerAsserts.neverPartOfCompilation();

        return Truffle.getRuntime().iterateFrames(new FrameInstanceVisitor<Node>() {

            @Override
            public Node visitFrame(FrameInstance frameInstance) {
                if (frameInstance.getCallNode().getEncapsulatingSourceSection() instanceof CoreSourceSection) {
                    return null;
                } else {
                    return frameInstance.getCallNode();
                }
            }

        });
    }

}
