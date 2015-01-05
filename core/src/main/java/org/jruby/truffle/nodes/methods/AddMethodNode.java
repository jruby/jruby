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
    @Child protected MethodDefinitionNode methodNode;

    public AddMethodNode(RubyContext context, SourceSection section, RubyNode receiver, MethodDefinitionNode method, boolean topLevel) {
        super(context, section);
        this.receiver = receiver;
        this.methodNode = method;
    }

    @Override
    public RubySymbol execute(VirtualFrame frame) {
        notDesignedForCompilation();

        final Object receiverObject = receiver.execute(frame);

        final RubyMethod methodObject = (RubyMethod) methodNode.execute(frame);

        RubyModule module;

        if (receiverObject instanceof RubyModule) {
            module = (RubyModule) receiverObject;
        } else {
            module = ((RubyBasicObject) receiverObject).getSingletonClass(this);
        }

        final RubyMethod method = methodObject.withDeclaringModule(module);

        if (method.getVisibility() == Visibility.MODULE_FUNCTION) {
            module.addMethod(this, method.withVisibility(Visibility.PRIVATE));
            module.getSingletonClass(this).addMethod(this, method.withVisibility(Visibility.PUBLIC));
        } else {
            module.addMethod(this, method);
        }

        return getContext().newSymbol(method.getName());
    }
}
