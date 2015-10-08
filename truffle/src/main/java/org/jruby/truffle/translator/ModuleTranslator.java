/*
 * Copyright (c) 2013, 2015 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.translator;

import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.source.Source;
import com.oracle.truffle.api.source.SourceSection;

import org.jruby.truffle.nodes.RubyNode;
import org.jruby.truffle.nodes.RubyRootNode;
import org.jruby.truffle.nodes.control.SequenceNode;
import org.jruby.truffle.nodes.methods.CatchReturnPlaceholderNode;
import org.jruby.truffle.nodes.methods.MethodDefinitionNode;
import org.jruby.truffle.runtime.RubyContext;

/**
 * Translates module and class nodes.
 * <p>
 * In Ruby, a module or class definition is somewhat like a method. It has a local scope and a value
 * for self, which is the module or class object that is being defined. Therefore for a module or
 * class definition we translate into a special method. We run that method with self set to be the
 * newly allocated module or class. We then have to treat at least method and constant definitions
 * differently.
 */
class ModuleTranslator extends BodyTranslator {

    public ModuleTranslator(Node currentNode, RubyContext context, BodyTranslator parent, TranslatorEnvironment environment, Source source) {
        super(currentNode, context, parent, environment, source, false);
        useClassVariablesAsIfInClass = true;
    }

    public MethodDefinitionNode compileClassNode(SourceSection sourceSection, String name, org.jruby.ast.Node bodyNode) {
        RubyNode body;

        parentSourceSection.push(sourceSection);
        try {
            body = translateNodeOrNil(sourceSection, bodyNode);
        } finally {
            parentSourceSection.pop();
        }

        if (environment.getFlipFlopStates().size() > 0) {
            body = SequenceNode.sequence(context, sourceSection, initFlipFlopStates(sourceSection), body);
        }

        body = new CatchReturnPlaceholderNode(context, sourceSection, body, environment.getReturnID());

        final RubyRootNode rootNode = new RubyRootNode(context, sourceSection, environment.getFrameDescriptor(), environment.getSharedMethodInfo(), body, environment.needsDeclarationFrame());

        return new MethodDefinitionNode(
                context,
                sourceSection,
                environment.getSharedMethodInfo().getName(),
                environment.getSharedMethodInfo(),
                Truffle.getRuntime().createCallTarget(rootNode));
    }

}
