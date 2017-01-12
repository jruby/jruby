/*
 * Copyright (c) 2015, 2017 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.language.methods;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.object.DynamicObject;
import org.jruby.truffle.language.RubyNode;
import org.jruby.truffle.language.arguments.RubyArguments;
import org.jruby.truffle.language.objects.SingletonClassNode;
import org.jruby.truffle.language.objects.SingletonClassNodeGen;

public class GetDefaultDefineeNode extends RubyNode {

    @Child private SingletonClassNode singletonClassNode = SingletonClassNodeGen.create(null);

    @Override
    public DynamicObject execute(VirtualFrame frame) {
        final InternalMethod method = RubyArguments.getMethod(frame);
        final DynamicObject capturedDefaultDefinee = method.getCapturedDefaultDefinee();

        if (capturedDefaultDefinee != null) {
            return capturedDefaultDefinee;
        }

        final Object self = RubyArguments.getSelf(frame);
        final DeclarationContext declarationContext = RubyArguments.getDeclarationContext(frame);
        return declarationContext.getModuleToDefineMethods(self, method, getContext(), singletonClassNode);
    }
}
