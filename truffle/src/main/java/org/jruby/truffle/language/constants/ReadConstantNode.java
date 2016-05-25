/*
 * Copyright (c) 2013, 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.language.constants;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.source.SourceSection;
import org.jruby.truffle.RubyContext;
import org.jruby.truffle.language.RubyConstant;
import org.jruby.truffle.language.RubyNode;

public class ReadConstantNode extends RubyNode implements RestartableReadConstantNode {

    @Child RubyNode moduleNode;
    @Child RubyNode nameNode;

    @Child LookupConstantNode lookupConstantNode;
    @Child GetConstantNode getConstantNode;

    public ReadConstantNode(
            RubyContext context,
            SourceSection sourceSection,
            boolean ignoreVisibility,
            boolean lookInObject,
            RubyNode moduleNode,
            RubyNode nameNode) {

        super(context, sourceSection);
        this.moduleNode = moduleNode;
        this.nameNode = nameNode;

        this.lookupConstantNode = LookupConstantNodeGen.create(
                context,
                sourceSection,
                ignoreVisibility,
                lookInObject,
                null,
                null);

        this.getConstantNode = GetConstantNodeGen.create(
                context,
                sourceSection,
                this,
                null,
                null,
                null);
    }

    @Override
    public Object execute(VirtualFrame frame) {
        final Object module = moduleNode.execute(frame);
        final String name = (String) nameNode.execute(frame);
        return readConstant(frame, module, name);
    }

    @Override
    public Object readConstant(VirtualFrame frame, Object module, String name) {
        final RubyConstant constant = lookupConstantNode.executeLookupConstant(frame, module, name);

        return getConstantNode.executeGetConstant(frame, module, name, constant);
    }

}
