/*
 * Copyright (c) 2014, 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.language;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.frame.FrameInstance;
import com.oracle.truffle.api.frame.FrameInstanceVisitor;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.source.SourceSection;
import org.jruby.truffle.language.arguments.RubyArguments;
import org.jruby.truffle.language.exceptions.DisablingBacktracesNode;
import org.jruby.truffle.language.backtrace.Activation;
import org.jruby.truffle.language.backtrace.Backtrace;
import org.jruby.truffle.core.CoreSourceSection;
import org.jruby.truffle.RubyContext;
import org.jruby.truffle.core.Layouts;
import org.jruby.truffle.language.methods.InternalMethod;

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

    public static Backtrace getBacktrace(RubyContext context, Node currentNode) {
        return getBacktrace(context, currentNode, 0);
    }

    public static Backtrace getBacktrace(RubyContext context, Node currentNode, int omit) {
        return getBacktrace(context, currentNode, omit, null);
    }

    public static Backtrace getBacktrace(RubyContext context, Node currentNode, int omit, DynamicObject exception) {
        return getBacktrace(context, currentNode, omit, false, exception);
    }

    public static Backtrace getBacktrace(RubyContext context, Node currentNode, final int omit, final boolean filterNullSourceSection, DynamicObject exception) {
        CompilerAsserts.neverPartOfCompilation();

        if (exception != null
                && context.getOptions().BACKTRACES_OMIT_UNUSED
                && DisablingBacktracesNode.areBacktracesDisabled()
                && ModuleOperations.assignableTo(Layouts.BASIC_OBJECT.getLogicalClass(exception), context.getCoreLibrary().getStandardErrorClass())) {
            return new Backtrace(new Activation[]{Activation.OMITTED_UNUSED});
        }

        final int limit = context.getOptions().BACKTRACES_LIMIT;

        final ArrayList<Activation> activations = new ArrayList<>();

            /*
             * TODO(cs): if this materializing the frames proves really expensive
             * we might want to make it optional - I think it's only used for some
             * features beyond what MRI does like printing locals in backtraces.
             */

        if (omit == 0 && currentNode != null && Truffle.getRuntime().getCurrentFrame() != null) {
            activations.add(new Activation(currentNode, Truffle.getRuntime().getCurrentFrame().getFrame(FrameInstance.FrameAccess.MATERIALIZE, true).materialize()));
        }

        Truffle.getRuntime().iterateFrames(new FrameInstanceVisitor<Object>() {
            int depth = 1;

            @Override
            public Object visitFrame(FrameInstance frameInstance) {
                if (depth > limit) {
                    activations.add(Activation.OMITTED_LIMIT);
                    return new Object();
                }

                if (!ignoreFrame(frameInstance) && depth >= omit) {
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

    private static boolean ignoreFrame(FrameInstance frameInstance) {
        final Node callNode = frameInstance.getCallNode();

        // Nodes with no call node are top-level - we may have multiple of them due to require

        if (callNode == null) {
            return true;
        }

        // Ignore the call to run_jruby_root

        // TODO CS 2-Feb-16 should find a better way to detect this than a string

        final SourceSection sourceSection = callNode.getEncapsulatingSourceSection();

        if (sourceSection != null && sourceSection.getSource() != null && sourceSection.getSource().getName().equals("run_jruby_root")) {
            return true;
        }

        if (callNode.getRootNode() instanceof InternalRootNode) {
            return true;
        }

        if (callNode.getEncapsulatingSourceSection() == null) {
            return true;
        }

        return false;
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
