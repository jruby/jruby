/*
 * Copyright (c) 2014, 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.core;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.source.SourceSection;
import org.jruby.truffle.Layouts;
import org.jruby.truffle.RubyContext;
import org.jruby.truffle.core.binding.BindingNodes;
import org.jruby.truffle.language.RubyNode;

public class SetTopLevelBindingNode extends RubyNode {

    public SetTopLevelBindingNode(RubyContext context, SourceSection sourceSection) {
        super(context, sourceSection);
    }

    @Override
    public Object execute(VirtualFrame frame) {
        final DynamicObject binding = BindingNodes.createBinding(getContext(), frame.materialize());
        Layouts.MODULE.getFields(coreLibrary().getObjectClass()).setConstant(getContext(), this, "TOPLEVEL_BINDING", binding);
        return nil();
    }

}
