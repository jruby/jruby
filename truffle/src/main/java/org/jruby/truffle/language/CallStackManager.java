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

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.RootCallTarget;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.frame.FrameInstance;
import com.oracle.truffle.api.frame.FrameInstanceVisitor;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.source.SourceSection;
import org.jruby.truffle.Layouts;
import org.jruby.truffle.RubyContext;
import org.jruby.truffle.core.module.ModuleOperations;
import org.jruby.truffle.language.arguments.RubyArguments;
import org.jruby.truffle.language.backtrace.Activation;
import org.jruby.truffle.language.backtrace.Backtrace;
import org.jruby.truffle.language.backtrace.BacktraceFormatter;
import org.jruby.truffle.language.backtrace.InternalRootNode;
import org.jruby.truffle.language.exceptions.DisablingBacktracesNode;
import org.jruby.truffle.language.methods.InternalMethod;
import org.jruby.util.Memo;

import java.util.ArrayList;

public class CallStackManager {

    private final RubyContext context;

    public CallStackManager(RubyContext context) {
        this.context = context;
    }

    @TruffleBoundary
    public FrameInstance getCallerFrameIgnoringSend() {
        final Memo<Boolean> firstFrame = new Memo<>(true);

        final FrameInstance fi = Truffle.getRuntime().iterateFrames(new FrameInstanceVisitor<FrameInstance>() {
            @Override
            public FrameInstance visitFrame(FrameInstance frameInstance) {
                if (firstFrame.get()) {
                    firstFrame.set(false);
                    return null;
                }

                final InternalMethod method = getMethod(frameInstance);
                assert method != null;

                if (!context.getCoreLibrary().isSend(method)) {
                    return frameInstance;
                } else {
                    return null;
                }
            }
        });

        return fi;
    }

    @TruffleBoundary
    public InternalMethod getCallingMethodIgnoringSend() {
        return getMethod(getCallerFrameIgnoringSend());
    }

    @TruffleBoundary
    public Node getTopMostUserCallNode() {
        final Memo<Boolean> firstFrame = new Memo<>(true);

        return Truffle.getRuntime().iterateFrames(new FrameInstanceVisitor<Node>() {

            @Override
            public Node visitFrame(FrameInstance frameInstance) {
                if (firstFrame.get()) {
                    firstFrame.set(false);
                    return null;
                }

                final SourceSection sourceSection = frameInstance.getCallNode().getEncapsulatingSourceSection();

                if (sourceSection.getSource() == null) {
                    return null;
                } else {
                    return frameInstance.getCallNode();
                }
            }

        });
    }

    private InternalMethod getMethod(FrameInstance frame) {
        return RubyArguments.getMethod(frame.getFrame(FrameInstance.FrameAccess.READ_ONLY, true));
    }

    public Backtrace getBacktrace(Node currentNode, Throwable javaThrowable) {
        return getBacktrace(currentNode, 0, false, null, javaThrowable);
    }

    public Backtrace getBacktrace(Node currentNode) {
        return getBacktrace(currentNode, 0, false, null, null);
    }

    public Backtrace getBacktrace(Node currentNode, int omit) {
        return getBacktrace(currentNode, omit, false, null, null);
    }

    public Backtrace getBacktrace(Node currentNode, int omit, DynamicObject exception) {
        return getBacktrace(currentNode, omit, false, exception, null);
    }

    public Backtrace getBacktrace(Node currentNode,
                                  final int omit,
                                  final boolean filterNullSourceSection,
                                  DynamicObject exception) {
        return getBacktrace(currentNode, omit, filterNullSourceSection, exception, null);
    }

    @TruffleBoundary
    public Backtrace getBacktrace(Node currentNode,
                                  final int omit,
                                  final boolean filterNullSourceSection,
                                  DynamicObject exception,
                                  Throwable javaThrowable) {
        if (exception != null
                && context.getOptions().BACKTRACES_OMIT_UNUSED
                && DisablingBacktracesNode.areBacktracesDisabled()
                && ModuleOperations.assignableTo(Layouts.BASIC_OBJECT.getLogicalClass(exception),
                    context.getCoreLibrary().getStandardErrorClass())) {
            return new Backtrace(new Activation[] { Activation.OMITTED_UNUSED }, null);
        }

        final int limit = context.getOptions().BACKTRACES_LIMIT;

        final ArrayList<Activation> activations = new ArrayList<>();

        if (omit == 0 && currentNode != null && Truffle.getRuntime().getCurrentFrame() != null) {
            final InternalMethod method = RubyArguments.tryGetMethod(Truffle.getRuntime().getCurrentFrame()
                    .getFrame(FrameInstance.FrameAccess.READ_ONLY, true));

            activations.add(new Activation(currentNode, method));
        }

        final Memo<Boolean> firstFrame = new Memo<>(true);

        Truffle.getRuntime().iterateFrames(new FrameInstanceVisitor<Object>() {
            int depth = 1;

            @Override
            public Object visitFrame(FrameInstance frameInstance) {
                if (firstFrame.get()) {
                    firstFrame.set(false);
                    return null;
                }

                if (depth > limit) {
                    activations.add(Activation.OMITTED_LIMIT);
                    return new Object();
                }

                if (!ignoreFrame(frameInstance) && depth >= omit) {
                    if (!(filterNullSourceSection && hasNullSourceSection(frameInstance))) {
                        final InternalMethod method = RubyArguments.tryGetMethod(frameInstance
                                .getFrame(FrameInstance.FrameAccess.READ_ONLY, true));

                        Node callNode = getCallNode(frameInstance, method);
                        activations.add(new Activation(callNode, method));
                    }
                }

                depth++;

                return null;
            }

        });

        // TODO CS 3-Mar-16 The last activation is I think what calls jruby_root_node, and I can't seem to remove it any other way
        if (!activations.isEmpty()) {
            activations.remove(activations.size() - 1);
        }

        if (context.getOptions().EXCEPTIONS_STORE_JAVA || context.getOptions().BACKTRACES_INTERLEAVE_JAVA) {
            if (javaThrowable == null) {
                javaThrowable = new Exception();
            }
        } else {
            javaThrowable = null;
        }

        return new Backtrace(activations.toArray(new Activation[activations.size()]), javaThrowable);
    }

    private boolean ignoreFrame(FrameInstance frameInstance) {
        final Node callNode = frameInstance.getCallNode();

        // Nodes with no call node are top-level or require, which *should* appear in the backtrace.
        if (callNode == null) {
            return false;
        }

        // Ignore the call to run_jruby_root
        // TODO CS 2-Feb-16 should find a better way to detect this than a string
        final SourceSection sourceSection = callNode.getEncapsulatingSourceSection();

        if (sourceSection != null && sourceSection.getShortDescription().endsWith("#run_jruby_root")) {
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

    private boolean hasNullSourceSection(FrameInstance frameInstance) {
        final Node callNode = frameInstance.getCallNode();
        if (callNode == null) {
            return true;
        }

        final SourceSection sourceSection = callNode.getEncapsulatingSourceSection();
        return sourceSection == null || sourceSection.getSource() == null;
    }

    private Node getCallNode(FrameInstance frameInstance, final InternalMethod method) {
        Node callNode = frameInstance.getCallNode();
        if (callNode == null && method != null &&
                BacktraceFormatter.isCore(method.getSharedMethodInfo().getSourceSection())) {
            callNode = ((RootCallTarget) method.getCallTarget()).getRootNode();
        }
        return callNode;
    }

}
