/*
 * Copyright (c) 2015, 2017 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.language.defined;

import com.oracle.truffle.api.frame.VirtualFrame;
import org.jruby.truffle.core.string.CoreString;
import org.jruby.truffle.language.RubyNode;

public class DefinedWrapperNode extends RubyNode {

    private final CoreString definition;

    @Child private RubyNode child;

    public DefinedWrapperNode(CoreString definition, RubyNode child) {
        this.definition = definition;
        this.child = child;
    }

    @Override
    public Object execute(VirtualFrame frame) {
        return child.execute(frame);
    }

    @Override
    public Object isDefined(VirtualFrame frame) {
        return definition.createInstance();
    }

}
