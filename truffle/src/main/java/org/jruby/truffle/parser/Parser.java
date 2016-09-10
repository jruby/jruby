/*
 * Copyright (c) 2013, 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.parser;

import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.MaterializedFrame;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.source.Source;
import org.jcodings.Encoding;
import org.jruby.truffle.RubyContext;
import org.jruby.truffle.language.RubyRootNode;

public interface Parser {

    RubyRootNode parse(RubyContext context,
                       Source source,
                       Encoding defaultEncoding,
                       ParserContext parserContext,
                       String[] argumentNames,
                       FrameDescriptor frameDescriptor,
                       MaterializedFrame parentFrame,
                       boolean ownScopeForAssignments,
                       Node currentNode);

}
