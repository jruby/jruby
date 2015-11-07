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
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.frame.FrameInstance;
import com.oracle.truffle.api.frame.FrameInstanceVisitor;
import com.oracle.truffle.api.nodes.Node;

import org.jruby.truffle.runtime.backtrace.Activation;
import org.jruby.truffle.runtime.backtrace.Backtrace;
import org.jruby.truffle.runtime.core.CoreSourceSection;
import org.jruby.truffle.runtime.methods.InternalMethod;

import java.util.ArrayList;

public abstract class RubyCallStack {

    /** Ignores Kernel#send and aliases */
    @TruffleBoundary
    public static FrameInstance getCallerFrame(final RubyContext context) {
        return Truffle.getRuntime().iterateFrames(new FrameInstanceVisitor<FrameInstance>() {
            @Override
            public FrameInstance visitFrame(FrameInstance frameInstance) {
                final InternalMethod method = getMethod(frameInstance);
                assert method != null;

                if (!context.getCoreLibrary().isSend(method)) {
                    return frameInstance;
                } else {
                    return null;
                }
            }
        });
    }

    public static InternalMethod getCallingMethod(final RubyContext context) {
        return getMethod(getCallerFrame(context));
    }

    private static InternalMethod getMethod(FrameInstance frame) {
        CompilerAsserts.neverPartOfCompilation();
        return RubyArguments.getMethod(frame.getFrame(FrameInstance.FrameAccess.READ_ONLY, true).getArguments());
    }

    public static Backtrace getBacktrace(Node currentNode) {
        return getBacktrace(currentNode, 0);
    }

    public static Backtrace getBacktrace(Node currentNode, int omit) {
        return getBacktrace(currentNode, omit, false);
    }

    public static Backtrace getBacktrace(Node currentNode, final int omit, final boolean filterNullSourceSection) {
        CompilerAsserts.neverPartOfCompilation();

        final ArrayList<Activation> activations = new ArrayList<>();

            /*
             * TODO(cs): if this materializing the frames proves really expensive
             * we might want to make it optional - I think it's only used for some
             * features beyond what MRI does like printing locals in backtraces.
             */

        if (omit == 0 && currentNode != null && Truffle.getRuntime().getCurrentFrame() != null) {
            activations.add(new Activation(currentNode, Truffle.getRuntime().getCurrentFrame().getFrame(FrameInstance.FrameAccess.MATERIALIZE, true).materialize()));
        }

        Truffle.getRuntime().iterateFrames(new FrameInstanceVisitor<InternalMethod>() {
            int depth = 1;

            @Override
            public InternalMethod visitFrame(FrameInstance frameInstance) {
                // Multiple top level methods (require) introduce null call nodes - ignore them

                if (frameInstance.getCallNode() != null && depth >= omit) {
                    if (!filterNullSourceSection || !(frameInstance.getCallNode().getEncapsulatingSourceSection() == null || frameInstance.getCallNode().getEncapsulatingSourceSection().getSource() == null)) {
                        activations.add(new Activation(frameInstance.getCallNode(),
                                frameInstance.getFrame(FrameInstance.FrameAccess.MATERIALIZE, true).materialize()));
                    }
                }
                depth++;

                return null;
            }

        });

        return new Backtrace(activations.toArray(new Activation[activations.size()]));
    }

    public static Node getTopMostUserCallNode() {
        CompilerAsserts.neverPartOfCompilation();

        return Truffle.getRuntime().iterateFrames(new FrameInstanceVisitor<Node>() {

            @Override
            public Node visitFrame(FrameInstance frameInstance) {
                if (CoreSourceSection.isCoreSourceSection(frameInstance.getCallNode().getEncapsulatingSourceSection())) {
                    return null;
                } else {
                    return frameInstance.getCallNode();
                }
            }

        });
    }

}
