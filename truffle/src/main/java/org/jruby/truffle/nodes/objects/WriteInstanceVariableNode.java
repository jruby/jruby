/*
 * Copyright (c) 2013, 2015 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.nodes.objects;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.UnexpectedResultException;
import com.oracle.truffle.api.source.SourceSection;
import org.jruby.truffle.nodes.RubyNode;
import org.jruby.truffle.nodes.objectstorage.WriteHeadObjectFieldNode;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.control.RaiseException;
import org.jruby.truffle.runtime.core.RubyBasicObject;
import org.jruby.truffle.translator.WriteNode;

public class WriteInstanceVariableNode extends RubyNode implements WriteNode {

    @Child private RubyNode receiver;
    @Child private RubyNode rhs;
    @Child private WriteHeadObjectFieldNode writeNode;
    private final boolean isGlobal;

    public WriteInstanceVariableNode(RubyContext context, SourceSection sourceSection, String name, RubyNode receiver, RubyNode rhs, boolean isGlobal) {
        super(context, sourceSection);
        this.receiver = receiver;
        this.rhs = rhs;
        writeNode = new WriteHeadObjectFieldNode(name);
        this.isGlobal = isGlobal;
    }

    @Override
    public int executeInteger(VirtualFrame frame) throws UnexpectedResultException {
        final Object object = receiver.execute(frame);

        if (object instanceof RubyBasicObject) {
            try {
                final int value = rhs.executeInteger(frame);

                writeNode.execute((RubyBasicObject) object, value);
                return value;
            } catch (UnexpectedResultException e) {
                writeNode.execute((RubyBasicObject) object, e.getResult());
                throw e;
            }
        } else {
            CompilerDirectives.transferToInterpreter();
            throw new RaiseException(getContext().getCoreLibrary().frozenError(getContext().getCoreLibrary().getLogicalClass(object).getName(), this));
        }
    }

    @Override
    public long executeLong(VirtualFrame frame) throws UnexpectedResultException {
        final Object object = receiver.execute(frame);

        if (object instanceof RubyBasicObject) {
            try {
                final long value = rhs.executeLong(frame);

                writeNode.execute((RubyBasicObject) object, value);
                return value;
            } catch (UnexpectedResultException e) {
                writeNode.execute((RubyBasicObject) object, e.getResult());
                throw e;
            }
        } else {
            CompilerDirectives.transferToInterpreter();
            throw new RaiseException(getContext().getCoreLibrary().frozenError(getContext().getCoreLibrary().getLogicalClass(object).getName(), this));
        }
    }

    @Override
    public double executeDouble(VirtualFrame frame) throws UnexpectedResultException {
        final Object object = receiver.execute(frame);

        if (object instanceof RubyBasicObject) {
            try {
                final double value = rhs.executeDouble(frame);

                writeNode.execute((RubyBasicObject) object, value);
                return value;
            } catch (UnexpectedResultException e) {
                writeNode.execute((RubyBasicObject) object, e.getResult());
                throw e;
            }
        } else {
            CompilerDirectives.transferToInterpreter();
            throw new RaiseException(getContext().getCoreLibrary().frozenError(getContext().getCoreLibrary().getLogicalClass(object).getName(), this));
        }
    }

    @Override
    public Object execute(VirtualFrame frame) {
        final Object object = receiver.execute(frame);
        final Object value = rhs.execute(frame);

        if (object instanceof RubyBasicObject) {
            writeNode.execute((RubyBasicObject) object, value);
        } else {
            CompilerDirectives.transferToInterpreter();
            throw new RaiseException(getContext().getCoreLibrary().frozenError(getContext().getCoreLibrary().getLogicalClass(object).getName(), this));
        }

        return value;
    }

    @Override
    public RubyNode makeReadNode() {
        return new ReadInstanceVariableNode(getContext(), getSourceSection(), (String) writeNode.getName(), receiver, isGlobal);
    }

    @Override
    public Object isDefined(VirtualFrame frame) {
        return getContext().makeString("assignment");
    }

}
