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
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.UnexpectedResultException;
import org.jruby.truffle.nodes.*;
import org.jruby.truffle.nodes.cast.BoxingNode;
import org.jruby.truffle.nodes.objectstorage.*;
import org.jruby.truffle.runtime.*;
import org.jruby.truffle.runtime.core.RubyBasicObject;
import org.jruby.truffle.runtime.objectstorage.*;

public class WriteInstanceVariableNode extends RubyNode implements WriteNode {

    private final RespecializeHook hook = new RespecializeHook() {

        @Override
        public void hookRead(ObjectStorage object, String name) {
            final RubyBasicObject rubyObject = (RubyBasicObject) object;

            if (!rubyObject.hasPrivateLayout()) {
                rubyObject.updateLayoutToMatchClass();
            }
        }

        @Override
        public void hookWrite(ObjectStorage object, String name, Object value) {
            final RubyBasicObject rubyObject = (RubyBasicObject) object;

            if (!rubyObject.hasPrivateLayout()) {
                rubyObject.updateLayoutToMatchClass();
            }

            rubyObject.setInstanceVariable(name, value);
        }

    };

    @Child protected BoxingNode receiver;
    @Child protected RubyNode rhs;
    @Child protected WriteObjectFieldNode writeNode;

    public WriteInstanceVariableNode(RubyContext context, SourceSection sourceSection, String name, RubyNode receiver, RubyNode rhs) {
        super(context, sourceSection);
        this.receiver = adoptChild(new BoxingNode(context, sourceSection, receiver));
        this.rhs = adoptChild(rhs);
        writeNode = adoptChild(new UninitializedWriteObjectFieldNode(name, hook));
    }

    @Override
    public int executeFixnum(VirtualFrame frame) throws UnexpectedResultException {
        final RubyBasicObject object = receiver.executeRubyBasicObject(frame);

        try {
            final int value = rhs.executeFixnum(frame);
            writeNode.execute(object, value);
            return value;
        } catch (UnexpectedResultException e) {
            writeNode.execute(object, e.getResult());
            throw e;
        }
    }

    @Override
    public double executeFloat(VirtualFrame frame) throws UnexpectedResultException {
        final RubyBasicObject object = receiver.executeRubyBasicObject(frame);

        try {
            final double value = rhs.executeFloat(frame);
            writeNode.execute(object, value);
            return value;
        } catch (UnexpectedResultException e) {
            writeNode.execute(object, e.getResult());
            throw e;
        }
    }

    @Override
    public Object execute(VirtualFrame frame) {
        final RubyBasicObject object = receiver.executeRubyBasicObject(frame);

        final Object value = rhs.execute(frame);
        writeNode.execute(object, value);
        return value;
    }

    @Override
    public RubyNode makeReadNode() {
        return new ReadInstanceVariableNode(getContext(), getSourceSection(), writeNode.getName(), receiver);
    }
}
