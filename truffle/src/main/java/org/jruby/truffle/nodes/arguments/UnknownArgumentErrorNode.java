/*
 * Copyright (c) 2015 Software Architecture Group, Hasso Plattner Institute.
 * All rights reserved. This code is released under a tri EPL/GPL/LGPL license.
 * You can use it, redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.nodes.arguments;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.source.SourceSection;
import org.jruby.truffle.nodes.RubyNode;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.control.RaiseException;

public class UnknownArgumentErrorNode extends RubyNode {

    private final String label;

    public UnknownArgumentErrorNode(RubyContext context,
            SourceSection sourceSection, String label) {
        super(context, sourceSection);
        this.label = label;
    }

    @Override
    public Object execute(VirtualFrame frame) {
        throw new RaiseException(getContext().getCoreLibrary().argumentError(
                "unknown keyword: " + label, this));
    }

}