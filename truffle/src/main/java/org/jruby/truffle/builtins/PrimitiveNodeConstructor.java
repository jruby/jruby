/*
 * Copyright (c) 2015, 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.builtins;

import com.oracle.truffle.api.dsl.NodeFactory;
import com.oracle.truffle.api.source.SourceSection;
import org.jruby.truffle.RubyContext;
import org.jruby.truffle.core.array.ArrayUtils;
import org.jruby.truffle.core.numeric.FixnumLowerNodeGen;
import org.jruby.truffle.language.RubyNode;
import org.jruby.truffle.language.arguments.MissingArgumentBehavior;
import org.jruby.truffle.language.arguments.ProfileArgumentNode;
import org.jruby.truffle.language.arguments.ReadPreArgumentNode;
import org.jruby.truffle.language.arguments.ReadSelfNode;
import org.jruby.truffle.language.control.ReturnID;

import java.util.ArrayList;
import java.util.List;

public class PrimitiveNodeConstructor {

    private final Primitive annotation;
    private final NodeFactory<? extends RubyNode> factory;

    public PrimitiveNodeConstructor(Primitive annotation, NodeFactory<? extends RubyNode> factory) {
        this.annotation = annotation;
        this.factory = factory;
    }

    public int getPrimitiveArity() {
        return factory.getExecutionSignature().size();
    }

    public RubyNode createCallPrimitiveNode(RubyContext context, SourceSection sourceSection, ReturnID returnID) {
        int argumentsCount = getPrimitiveArity();
        final List<RubyNode> arguments = new ArrayList<>(argumentsCount);
        List<List<Class<?>>> signatures = factory.getNodeSignatures();

        assert signatures.size() == 1;
        List<Class<?>> signature = signatures.get(0);

        if (annotation.needsSelf()) {
            arguments.add(transformArgument(new ProfileArgumentNode(new ReadSelfNode()), 0));
            argumentsCount--;
        }

        for (int n = 0; n < argumentsCount; n++) {
            RubyNode readArgumentNode = new ProfileArgumentNode(new ReadPreArgumentNode(n, MissingArgumentBehavior.UNDEFINED));
            arguments.add(transformArgument(readArgumentNode, n + 1));
        }

        if (!CoreMethodNodeManager.isSafe(context, annotation.unsafe())) {
            return new UnsafeNode(context, sourceSection);
        }

        if (signature.size() >= 3 && signature.get(2) == RubyNode[].class) {
            return new CallPrimitiveNode(context, sourceSection,
                    factory.createNode(context, sourceSection, arguments.toArray(new RubyNode[arguments.size()])), returnID);
        } else if (signature.size() == 1 && signature.get(0) == RubyNode[].class) {
            return new CallPrimitiveNode(context, sourceSection,
                    factory.createNode(new Object[]{arguments.toArray(new RubyNode[arguments.size()])}), returnID);
        } else if (signature.size() == 0) {
            return new CallPrimitiveNode(context, sourceSection,
                    factory.createNode(), returnID);
        } else if (signature.get(0) != RubyContext.class) {
            final Object[] varargs = new Object[arguments.size()];
            System.arraycopy(arguments.toArray(new RubyNode[arguments.size()]), 0, varargs, 0, arguments.size());
            return new CallPrimitiveNode(context, sourceSection, factory.createNode(varargs), returnID);
        } else {
            final Object[] varargs = new Object[2 + arguments.size()];
            varargs[0] = context;
            varargs[1] = sourceSection;
            System.arraycopy(arguments.toArray(new RubyNode[arguments.size()]), 0, varargs, 2, arguments.size());

            return new CallPrimitiveNode(context, sourceSection, factory.createNode(varargs), returnID);
        }
    }

    public RubyNode createInvokePrimitiveNode(RubyContext context, SourceSection sourceSection, RubyNode[] arguments) {
        assert arguments.length == getPrimitiveArity();

        if (!CoreMethodNodeManager.isSafe(context, annotation.unsafe())) {
            return new UnsafeNode(context, sourceSection);
        }

        for (int n = 0; n < arguments.length; n++) {
            int nthArg = annotation.needsSelf() ? n : n + 1;
            arguments[n] = transformArgument(arguments[n], nthArg);
        }

        List<List<Class<?>>> signatures = factory.getNodeSignatures();

        assert signatures.size() == 1;
        List<Class<?>> signature = signatures.get(0);

        final RubyNode primitiveNode;
        if (signature.get(0) == RubyContext.class) {
            primitiveNode = factory.createNode(context, sourceSection, arguments);
        } else {
            primitiveNode = factory.createNode(new Object[] { arguments });
        }
        return new InvokePrimitiveNode(context, sourceSection, primitiveNode);
    }

    private RubyNode transformArgument(RubyNode argument, int n) {
        if (ArrayUtils.contains(annotation.lowerFixnum(), n)) {
            return FixnumLowerNodeGen.create(null, null, argument);
        } else {
            return argument;
        }
    }

}
