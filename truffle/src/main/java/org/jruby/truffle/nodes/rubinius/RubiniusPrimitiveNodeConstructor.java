/*
 * Copyright (c) 2015 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.nodes.rubinius;

import com.oracle.truffle.api.dsl.NodeFactory;
import com.oracle.truffle.api.source.SourceSection;
import org.jruby.truffle.nodes.RubyNode;
import org.jruby.truffle.nodes.arguments.MissingArgumentBehaviour;
import org.jruby.truffle.nodes.arguments.ReadPreArgumentNode;
import org.jruby.truffle.nodes.core.fixnum.FixnumLowerNode;
import org.jruby.truffle.nodes.objects.SelfNode;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.array.ArrayUtils;

import java.util.ArrayList;
import java.util.List;

public class RubiniusPrimitiveNodeConstructor implements RubiniusPrimitiveConstructor {

    private final RubiniusPrimitive annotation;
    private final NodeFactory<? extends RubyNode> factory;

    public RubiniusPrimitiveNodeConstructor(RubiniusPrimitive annotation, NodeFactory<? extends RubyNode> factory) {
        this.annotation = annotation;
        this.factory = factory;
    }

    @Override
    public int getPrimitiveArity() {
        return factory.getExecutionSignature().size();
    }

    @Override
    public RubyNode createCallPrimitiveNode(RubyContext context, SourceSection sourceSection, long returnID) {
        int argumentsCount = getPrimitiveArity();
        final List<RubyNode> arguments = new ArrayList<>(argumentsCount);

        if (annotation.needsSelf()) {
            arguments.add(new SelfNode(context, sourceSection));
            argumentsCount--;
        }

        for (int n = 0; n < argumentsCount; n++) {
            RubyNode readArgumentNode = new ReadPreArgumentNode(context, sourceSection, n, MissingArgumentBehaviour.UNDEFINED);
            arguments.add(transformArgument(readArgumentNode, n));
        }

        return new CallRubiniusPrimitiveNode(context, sourceSection,
                factory.createNode(context, sourceSection, arguments.toArray(new RubyNode[arguments.size()])), returnID);
    }

    public RubyNode createInvokePrimitiveNode(RubyContext context, SourceSection sourceSection, RubyNode[] arguments) {
        for (int n = 1; n < arguments.length; n++) {
            arguments[n] = transformArgument(arguments[n], n);
        }

        return new InvokeRubiniusPrimitiveNode(context, sourceSection,
                factory.createNode(context, sourceSection, arguments));
    }

    private RubyNode transformArgument(RubyNode argument, int n) {
        if (ArrayUtils.contains(annotation.lowerFixnumParameters(), n)) {
            return new FixnumLowerNode(argument);
        } else {
            return argument;
        }
    }


}
