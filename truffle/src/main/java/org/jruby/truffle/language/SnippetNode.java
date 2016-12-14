/*
 * Copyright (c) 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.language;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.MaterializedFrame;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.DirectCallNode;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.source.Source;
import org.jcodings.specific.UTF8Encoding;
import org.jruby.truffle.language.arguments.RubyArguments;
import org.jruby.truffle.language.methods.DeclarationContext;
import org.jruby.truffle.parser.ParserContext;

public class SnippetNode extends RubyBaseNode {

    @CompilationFinal private String expression;
    @CompilationFinal private String[] parameters;
    @CompilationFinal private FrameDescriptor frameDescriptor;
    @CompilationFinal private FrameSlot[] parameterFrameSlots;

    @Child private DirectCallNode directCallNode;

    public SnippetNode() {
    }

    public Object execute(VirtualFrame frame, String expression, Object... arguments) {
        if (directCallNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();

            this.expression = expression;
            assert arguments.length % 2 == 0;
            parameters = new String[arguments.length / 2];
            for (int n = 0; n < parameters.length; n++) {
                parameters[n] = (String) arguments[2 * n];
            }

            frameDescriptor = new FrameDescriptor(nil());
            parameterFrameSlots = new FrameSlot[parameters.length];

            for (int n = 0; n < parameters.length; n++) {
                parameterFrameSlots[n] = frameDescriptor.findOrAddFrameSlot(parameters[n]);
            }

            final Source source = getContext().getSourceLoader().loadFragment(this.expression, "(snippet)");

            final RubyRootNode rootNode = getContext().getCodeLoader().parse(
                    source,
                    UTF8Encoding.INSTANCE,
                    ParserContext.INLINE,
                    frameDescriptor,
                    null,
                    true,
                    this);

            directCallNode = insert(Truffle.getRuntime().createDirectCallNode(
                    Truffle.getRuntime().createCallTarget(rootNode)));

            if (directCallNode.isInlinable()) {
                directCallNode.forceInlining();
            }
        }

        if (arguments.length != parameters.length * 2) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            throw new UnsupportedOperationException(
                    "number of arguments doesn't match number of parameters");
        }

        assert ensureConstantExpressionParameters(expression, arguments);

        final Object[] callArguments = RubyArguments.pack(
                parentFrame(frame, arguments),
                null,
                RubyArguments.getMethod(frame),
                DeclarationContext.INSTANCE_EVAL,
                null,
                RubyArguments.getSelf(frame),
                null,
                new Object[]{});

        return directCallNode.call(frame, callArguments);
    }

    private boolean ensureConstantExpressionParameters(String expression, Object[] arguments) {
        assert this.expression == expression :
                "has to be always called with same expression: " + this.expression + " != " + expression;
        for (int n = 0; n < parameters.length; n++) {
            assert parameters[n] == arguments[2 * n] :
                    "has to be always called with same parameter name at " + n + ": " + parameters[n] + " != " + arguments[2 * n];
        }
        return true;
    }

    @ExplodeLoop
    private MaterializedFrame parentFrame(VirtualFrame frame, Object[] arguments) {
        final Object[] parentFrameArguments = RubyArguments.pack(
                null,
                null,
                RubyArguments.getMethod(frame),
                DeclarationContext.INSTANCE_EVAL,
                null,
                RubyArguments.getSelf(frame),
                null,
                new Object[]{});

        final MaterializedFrame parentFrame = Truffle.getRuntime().createMaterializedFrame(
                parentFrameArguments,
                frameDescriptor);

        for (int n = 0; n < parameters.length; n++) {
            parentFrame.setObject(parameterFrameSlots[n], arguments[2 * n + 1]);
        }
        return parentFrame;
    }

}
