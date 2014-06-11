/*
 * Copyright (c) 2013 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.nodes.objects;

import com.oracle.truffle.api.*;
import com.oracle.truffle.api.source.*;
import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.api.nodes.*;
import org.jruby.truffle.nodes.*;
import org.jruby.truffle.nodes.cast.BoxingNode;
import org.jruby.truffle.runtime.*;
import org.jruby.truffle.runtime.core.RubyClass;

/**
 * Reads the class of an object.
 */
@NodeInfo(shortName = "class")
public class ClassNode extends RubyNode {

    @Child protected BoxingNode child;

    public ClassNode(RubyContext context, SourceSection sourceSection, BoxingNode child) {
        super(context, sourceSection);
        this.child = child;
    }

    @Override
    public RubyClass executeRubyClass(VirtualFrame frame) {
        return child.executeRubyBasicObject(frame).getRubyClass();
    }

    @Override
    public Object execute(VirtualFrame frame) {
        return executeRubyClass(frame);
    }

}
