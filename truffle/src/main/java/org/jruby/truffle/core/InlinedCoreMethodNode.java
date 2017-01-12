/*
 * Copyright (c) 2016, 2017 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.core;

import com.oracle.truffle.api.Assumption;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.NodeFactory;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.nodes.NodeUtil;
import com.oracle.truffle.api.source.Source;
import org.jruby.truffle.RubyContext;
import org.jruby.truffle.builtins.CoreMethodNodeManager;
import org.jruby.truffle.core.array.ArrayUtils;
import org.jruby.truffle.language.RubyNode;
import org.jruby.truffle.language.dispatch.RubyCallNode;
import org.jruby.truffle.language.dispatch.RubyCallNodeParameters;
import org.jruby.truffle.language.methods.InternalMethod;
import org.jruby.truffle.language.methods.LookupMethodNode;
import org.jruby.truffle.language.methods.LookupMethodNodeGen;

import java.util.Arrays;
import java.util.List;

public class InlinedCoreMethodNode extends RubyNode {

    private final RubyCallNodeParameters callNodeParameters;
    private final InternalMethod method;
    private final Assumption tracingUnused;

    @Child private InlinableBuiltin builtin;
    @Child private LookupMethodNode lookupMethodNode;
    @Child private RubyNode receiverNode;
    @Children private final RubyNode[] argumentNodes;

    private RubyCallNode replacedBy = null;

    public InlinedCoreMethodNode(RubyCallNodeParameters callNodeParameters, InternalMethod method, InlinableBuiltin builtin) {
        this.callNodeParameters = callNodeParameters;
        this.method = method;
        this.tracingUnused = getContext().getTraceManager().getUnusedAssumption();
        this.builtin = builtin;
        this.lookupMethodNode = LookupMethodNodeGen.create(false, false, null, null);
        this.receiverNode = callNodeParameters.getReceiver();
        this.argumentNodes = callNodeParameters.getArguments();
    }

    public boolean guard(Object[] args) {
        // TODO (eregon, 10 Sep 2016): specific to some Fixnum methods obviously, use a guard node or subclasses
        return args[1] instanceof Integer;
    }

    @Override
    public Object execute(VirtualFrame frame) {
        final Object self = receiverNode.execute(frame);
        final InternalMethod lookedUpMethod = lookupMethodNode.executeLookupMethod(frame, self, method.getName());
        final Object[] arguments = executeArguments(frame, self);

        if (lookedUpMethod == method && guard(arguments) && tracingUnused.isValid()) {
            return builtin.executeBuiltin(frame, arguments);
        } else {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            Object[] argumentsObjects = ArrayUtils.extractRange(arguments, 1, arguments.length);
            return rewriteToCallNode().executeWithArgumentsEvaluated(frame, arguments[0], argumentsObjects);
        }
    }

    @ExplodeLoop
    private Object[] executeArguments(VirtualFrame frame, Object self) {
        final Object[] arguments = new Object[1 + argumentNodes.length];

        arguments[0] = self;
        for (int i = 0; i < argumentNodes.length; i++) {
            arguments[1 + i] = argumentNodes[i].execute(frame);
        }

        return arguments;
    }

    private RubyCallNode rewriteToCallNode() {
        CompilerDirectives.transferToInterpreterAndInvalidate();
        return atomic(() -> {
            // Check if we are still in the AST
            boolean found = !NodeUtil.forEachChild(getParent(), node -> node != this);

            if (found) {
                // We need to pass the updated children of this node to the call node
                RubyCallNode callNode = new RubyCallNode(callNodeParameters.withReceiverAndArguments(receiverNode, argumentNodes, callNodeParameters.getBlock()));
                replacedBy = callNode;
                return replace(callNode, method.getName() + " could not be executed inline");
            } else {
                return replacedBy;
            }
        });
    }

    public static InlinedCoreMethodNode inlineBuiltin(RubyContext context, Source source, RubyCallNodeParameters callParameters, InternalMethod method, NodeFactory<? extends InlinableBuiltin> builtinFactory) {
        // Let arguments to null as we need to execute the receiver ourselves to lookup the method
        final List<RubyNode> arguments = Arrays.asList(new RubyNode[1 + callParameters.getArguments().length]);
        final InlinableBuiltin builtinNode = CoreMethodNodeManager.createNodeFromFactory(context, source, null, builtinFactory, arguments);
        return new InlinedCoreMethodNode(callParameters, method, builtinNode);
    }

    @Override
    public Object isDefined(VirtualFrame frame) {
        return rewriteToCallNode().isDefined(frame);
    }

}
