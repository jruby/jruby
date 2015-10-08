/*
 * Copyright (c) 2013, 2015 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.nodes.methods;

import org.jruby.truffle.nodes.RubyGuards;
import org.jruby.truffle.nodes.RubyNode;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.layouts.Layouts;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.source.SourceSection;

public class AliasNode extends RubyNode {

    @Child private RubyNode defaultDefinee;

    final String newName;
    final String oldName;

    public AliasNode(RubyContext context, SourceSection sourceSection, RubyNode defaultDefinee, String newName, String oldName) {
        super(context, sourceSection);
        this.defaultDefinee = defaultDefinee;
        this.newName = newName;
        this.oldName = oldName;
    }

    @Override
    public Object execute(VirtualFrame frame) {
        final Object moduleObject = defaultDefinee.execute(frame);
        assert RubyGuards.isRubyModule(moduleObject);
        final DynamicObject module = (DynamicObject) moduleObject;

        Layouts.MODULE.getFields(module).alias(getContext(), this, newName, oldName);
        return module;
    }

}
