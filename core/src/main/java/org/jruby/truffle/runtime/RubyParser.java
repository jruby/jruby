/*
 * Copyright (c) 2013 Oracle and/or its affiliates. All rights reserved. This
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

import org.jruby.Ruby;
import org.jruby.truffle.nodes.methods.MethodDefinitionNode;

/**
 * Interface to a Ruby parser.
 */
public interface RubyParser {

    public static enum ParserContext {
        TOP_LEVEL, SHELL, MODULE
    }

    MethodDefinitionNode parse(RubyContext context, org.jruby.ast.ArgsNode argsNode, org.jruby.ast.Node bodyNode);
    RubyParserResult parse(RubyContext context, Source source, ParserContext parserContext, MaterializedFrame parentFrame);
    RubyParserResult parse(RubyContext context, Source source, ParserContext parserContext, MaterializedFrame parentFrame, org.jruby.ast.RootNode rootNode);

}
