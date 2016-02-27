/*
 * Copyright (c) 2015, 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.language.methods;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.source.SourceSection;
import org.jruby.truffle.RubyContext;
import org.jruby.truffle.language.RubyNode;
import org.jruby.truffle.language.arguments.RubyArguments;
import org.jruby.truffle.language.objects.SingletonClassNode;
import org.jruby.truffle.language.objects.SingletonClassNodeGen;

public class GetDefaultDefineeNode extends RubyNode {

    @Child private SingletonClassNode singletonClassNode;

    public GetDefaultDefineeNode(RubyContext context, SourceSection sourceSection) {
        super(context, sourceSection);
        this.singletonClassNode = SingletonClassNodeGen.create(context, sourceSection, null);
    }

    @Override
    public DynamicObject execute(VirtualFrame frame) {
        CompilerDirectives.transferToInterpreter();

        final DynamicObject capturedDefaultDefinee = RubyArguments.getMethod(frame).getCapturedDefaultDefinee();

        if (capturedDefaultDefinee != null) {
            return capturedDefaultDefinee;
        }

        return RubyArguments.getDeclarationContext(frame).getModuleToDefineMethods(frame, getContext(), singletonClassNode);
    }
}
