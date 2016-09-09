/*
 * Copyright (c) 2013, 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.language.exceptions;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.source.SourceSection;
import org.jruby.truffle.RubyContext;
import org.jruby.truffle.language.RubyNode;
import org.jruby.truffle.language.objects.IsANode;
import org.jruby.truffle.language.objects.IsANodeGen;

public abstract class RescueNode extends RubyNode {

    @Child private RubyNode body;

    @Child private IsANode isANode;

    public RescueNode(RubyContext context, SourceSection sourceSection, RubyNode body) {
        super(context, sourceSection);
        this.body = body;
    }

    public abstract boolean canHandle(VirtualFrame frame, DynamicObject exception);

    @Override
    public Object execute(VirtualFrame frame) {
        return body.execute(frame);
    }

    @Override
    public void executeVoid(VirtualFrame frame) {
        body.executeVoid(frame);
    }

    protected IsANode getIsANode() {
        if (isANode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            isANode = insert(IsANodeGen.create(getContext(), null, null, null));
        }

        return isANode;
    }

}
