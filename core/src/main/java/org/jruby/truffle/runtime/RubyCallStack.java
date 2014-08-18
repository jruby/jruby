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
import org.jruby.truffle.runtime.backtrace.MRIBacktraceFormatter;
import org.jruby.truffle.runtime.core.*;
import org.jruby.truffle.runtime.methods.MethodLike;
import org.jruby.truffle.runtime.methods.RubyMethod;
import org.jruby.util.cli.Options;

import java.util.ArrayList;
import java.util.Iterator;

public abstract class RubyCallStack {

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

    public static RubyMethod getCurrentMethod() {
        CompilerAsserts.neverPartOfCompilation();

        MethodLike method;

        final FrameInstance currentFrame = Truffle.getRuntime().getCurrentFrame();

        method = getMethod(currentFrame);

        if (method instanceof RubyMethod) {
            return (RubyMethod) method;
        }

        for (FrameInstance frame : Truffle.getRuntime().getStackTrace()) {
            method = getMethod(frame);

            if (method instanceof RubyMethod) {
                return (RubyMethod) method;
            }
        }

        return null;
    }

    private static MethodLike getMethod(FrameInstance frame) {
        CompilerAsserts.neverPartOfCompilation();

        if (frame == null) {
            return null;
        }

        return RubyArguments.getMethod(frame.getFrame(FrameInstance.FrameAccess.READ_ONLY, true).getArguments());
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

    public static Backtrace getBacktrace(Node currentNode) {
        final ArrayList<Activation> activations = new ArrayList<>();

        if (Options.TRUFFLE_BACKTRACE_GENERATE.load()) {
        /*
         * TODO(cs): if this materializing the frames proves really expensive
         * we might want to make it optional - I think it's only used for some
         * features beyond what MRI does like printing locals in backtraces.
         */

            if (Truffle.getRuntime().getCurrentFrame() != null) {
                activations.add(new Activation(currentNode, Truffle.getRuntime().getCurrentFrame().getFrame(FrameInstance.FrameAccess.MATERIALIZE, true).materialize()));
            }

            for (FrameInstance frame : Truffle.getRuntime().getStackTrace()) {
                activations.add(new Activation(frame.getCallNode(), frame.getFrame(FrameInstance.FrameAccess.MATERIALIZE, true).materialize()));
            }
        }

        return new Backtrace(activations.toArray(new Activation[activations.size()]));
    }

}
