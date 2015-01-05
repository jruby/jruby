/*
 * Copyright (c) 2013, 2014 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.nodes.methods;

import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.FrameSlotTypeException;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.source.SourceSection;
import org.jruby.runtime.Visibility;
import org.jruby.truffle.nodes.RubyNode;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.core.RubyBasicObject;
import org.jruby.truffle.runtime.core.RubyModule;
import org.jruby.truffle.runtime.core.RubySymbol;
import org.jruby.truffle.runtime.methods.RubyMethod;

public class AddMethodNode extends RubyNode {

    @Child protected RubyNode receiver;
    @Child protected MethodDefinitionNode method;

    public AddMethodNode(RubyContext context, SourceSection section, RubyNode receiver, MethodDefinitionNode method, boolean topLevel) {
        super(context, section);
        this.receiver = receiver;
        this.method = method;
    }

    @Override
    public RubySymbol execute(VirtualFrame frame) {
        notDesignedForCompilation();

        final Object receiverObject = receiver.execute(frame);

        final RubyMethod methodObject = (RubyMethod) method.execute(frame);

        RubyModule module;

        if (receiverObject instanceof RubyModule) {
            module = (RubyModule) receiverObject;
        } else {
            module = ((RubyBasicObject) receiverObject).getSingletonClass(this);
        }

        final RubyMethod methodWithDeclaringModule = methodObject.withDeclaringModule(module);

        if (moduleFunctionFlag(frame)) {
            module.addMethod(this, methodWithDeclaringModule.withVisibility(Visibility.PRIVATE));
            module.getSingletonClass(this).addMethod(this, methodWithDeclaringModule.withVisibility(Visibility.PUBLIC));
        } else {
            module.addMethod(this, methodWithDeclaringModule);
        }

        return getContext().newSymbol(method.getName());
    }

    private boolean moduleFunctionFlag(VirtualFrame frame) {
        final FrameSlot moduleFunctionFlagSlot = frame.getFrameDescriptor().findFrameSlot(RubyModule.MODULE_FUNCTION_FLAG_FRAME_SLOT_ID);

        if (moduleFunctionFlagSlot == null) {
            return false;
        } else {
            Object moduleFunctionObject;

            try {
                moduleFunctionObject = frame.getObject(moduleFunctionFlagSlot);
            } catch (FrameSlotTypeException e) {
                throw new RuntimeException(e);
            }

            return (moduleFunctionObject instanceof Boolean) && (boolean) moduleFunctionObject;
        }
    }
}
