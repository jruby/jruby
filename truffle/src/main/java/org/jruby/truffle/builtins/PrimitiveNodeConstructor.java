/*
 * Copyright (c) 2015, 2017 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.builtins;

import com.oracle.truffle.api.dsl.NodeFactory;
import com.oracle.truffle.api.source.Source;
import com.oracle.truffle.api.source.SourceSection;
import org.jruby.truffle.RubyContext;
import org.jruby.truffle.RubyLanguage;
import org.jruby.truffle.core.array.ArrayUtils;
import org.jruby.truffle.core.numeric.FixnumLowerNodeGen;
import org.jruby.truffle.language.RubyNode;
import org.jruby.truffle.language.SourceIndexLength;
import org.jruby.truffle.language.arguments.MissingArgumentBehavior;
import org.jruby.truffle.language.arguments.ProfileArgumentNode;
import org.jruby.truffle.language.arguments.ReadPreArgumentNode;
import org.jruby.truffle.language.arguments.ReadSelfNode;
import org.jruby.truffle.parser.Translator;

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

    public RubyNode createCallPrimitiveNode(RubyContext context, Source source, SourceIndexLength sourceSection, RubyNode fallback) {
        int argumentsCount = getPrimitiveArity();
        final List<RubyNode> arguments = new ArrayList<>(argumentsCount);

        if (annotation.needsSelf()) {
            arguments.add(transformArgument(new ProfileArgumentNode(new ReadSelfNode()), 0));
            argumentsCount--;
        }

        for (int n = 0; n < argumentsCount; n++) {
            RubyNode readArgumentNode = new ProfileArgumentNode(new ReadPreArgumentNode(n, MissingArgumentBehavior.UNDEFINED));
            arguments.add(transformArgument(readArgumentNode, n + 1));
        }

        if (!CoreMethodNodeManager.isSafe(context, annotation.unsafe())) {
            return new UnsafeNode();
        }

        final RubyNode primitiveNode = CoreMethodNodeManager.createNodeFromFactory(context, source, sourceSection, factory, arguments);

        return Translator.withSourceSection(sourceSection, new CallPrimitiveNode(primitiveNode, fallback));
    }

    public RubyNode createInvokePrimitiveNode(RubyContext context, Source source, SourceIndexLength sourceSection, RubyNode[] arguments) {
        if (arguments.length != getPrimitiveArity()) {
            throw new AssertionError("Incorrect number of arguments at " + RubyLanguage.fileLine(sourceSection.toSourceSection(source)));
        }

        if (!CoreMethodNodeManager.isSafe(context, annotation.unsafe())) {
            return new UnsafeNode();
        }

        for (int n = 0; n < arguments.length; n++) {
            int nthArg = annotation.needsSelf() ? n : n + 1;
            arguments[n] = transformArgument(arguments[n], nthArg);
        }

        final List<List<Class<?>>> signatures = factory.getNodeSignatures();

        assert signatures.size() == 1;
        final List<Class<?>> signature = signatures.get(0);

        final RubyNode primitiveNode;

        if (signature.get(0) == SourceSection.class) {
            primitiveNode = factory.createNode(sourceSection.toSourceSection(source), arguments);
        } else if (signature.get(0) == SourceIndexLength.class) {
            primitiveNode = factory.createNode(sourceSection, arguments);
        } else {
            primitiveNode = factory.createNode(new Object[] { arguments });
        }

        return Translator.withSourceSection(sourceSection, new InvokePrimitiveNode(primitiveNode));
    }

    private RubyNode transformArgument(RubyNode argument, int n) {
        if (ArrayUtils.contains(annotation.lowerFixnum(), n)) {
            return FixnumLowerNodeGen.create(argument);
        } else {
            return argument;
        }
    }

}
