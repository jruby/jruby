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

import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.frame.Frame;
import com.oracle.truffle.api.frame.FrameInstance;
import com.oracle.truffle.api.frame.FrameInstanceVisitor;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.frame.FrameInstance.FrameAccess;
import com.oracle.truffle.api.source.SourceSection;

import org.jruby.runtime.Visibility;
import org.jruby.truffle.nodes.RubyNode;
import org.jruby.truffle.runtime.RubyArguments;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.control.TruffleFatalException;
import org.jruby.truffle.runtime.core.RubyBasicObject;
import org.jruby.truffle.runtime.core.RubyModule;
import org.jruby.truffle.runtime.core.RubySymbol;
import org.jruby.truffle.runtime.methods.InternalMethod;

public class AddMethodNode extends RubyNode {

    @Child private RubyNode receiver;
    @Child private MethodDefinitionNode methodNode;

    public AddMethodNode(RubyContext context, SourceSection section, RubyNode receiver, MethodDefinitionNode method) {
        super(context, section);
        this.receiver = receiver;
        this.methodNode = method;
    }

    @Override
    public RubySymbol execute(VirtualFrame frame) {
        notDesignedForCompilation("5d56b9f6cfb34b1cbd69f9de08b91dfa");

        final Object receiverObject = receiver.execute(frame);

        final InternalMethod methodObject = (InternalMethod) methodNode.execute(frame);

        RubyModule module;

        if (receiverObject instanceof RubyModule) {
            module = (RubyModule) receiverObject;
        } else {
            module = ((RubyBasicObject) receiverObject).getSingletonClass(this);
        }

        final Visibility visibility = getVisibility(frame, methodObject.getName());
        final InternalMethod method = methodObject.withDeclaringModule(module).withVisibility(visibility);

        if (method.getVisibility() == Visibility.MODULE_FUNCTION) {
            module.addMethod(this, method.withVisibility(Visibility.PRIVATE));
            module.getSingletonClass(this).addMethod(this, method.withVisibility(Visibility.PUBLIC));
        } else {
            module.addMethod(this, method);
        }

        return getContext().newSymbol(method.getName());
    }

    private static Visibility getVisibility(Frame frame, String name) {
        notDesignedForCompilation("54177ed5af1b406381d31c1cf763f623");

        if (name.equals("initialize") || name.equals("initialize_copy") || name.equals("initialize_clone") || name.equals("initialize_dup") || name.equals("respond_to_missing?")) {
            return Visibility.PRIVATE;
        } else {
            return getVisibility(frame);
        }
    }

    private static Visibility getVisibility(Frame frame) {
        notDesignedForCompilation("a05f87ceef294350970cb1db8c20bed8");

        while (frame != null) {
            Visibility visibility = findVisibility(frame);
            if (visibility != null) {
                return visibility;
            }
            frame = RubyArguments.getDeclarationFrame(frame.getArguments());
        }

        throw new UnsupportedOperationException("No declaration frame with visibility found");
    }

    private static Visibility findVisibility(Frame frame) {
        FrameSlot slot = frame.getFrameDescriptor().findFrameSlot(RubyModule.VISIBILITY_FRAME_SLOT_ID);
        if (slot == null) {
            return null;
        } else {
            Object visibilityObject = frame.getValue(slot);
            if (visibilityObject instanceof Visibility) {
                return (Visibility) visibilityObject;
            } else {
                return Visibility.PUBLIC;
            }
        }
    }
}
