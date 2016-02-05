/*
 * Copyright (c) 2015 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.core.rubinius;

import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.source.SourceSection;
import org.jruby.truffle.language.RubyGuards;
import org.jruby.truffle.language.RubyNode;
import org.jruby.truffle.language.arguments.NodeArrayToObjectArrayNode;
import org.jruby.truffle.language.arguments.ReadAllArgumentsNode;
import org.jruby.truffle.language.arguments.ReadBlockNode;
import org.jruby.truffle.core.MethodNodesFactory.CallNodeFactory;
import org.jruby.truffle.language.literal.LiteralNode;
import org.jruby.truffle.language.NotProvided;
import org.jruby.truffle.language.control.ReturnID;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.layouts.Layouts;

public class RubiniusPrimitiveCallConstructor implements RubiniusPrimitiveConstructor {

    private final DynamicObject method;

    public RubiniusPrimitiveCallConstructor(DynamicObject method) {
        assert RubyGuards.isRubyMethod(method);
        this.method = method;
    }

    @Override
    public int getPrimitiveArity() {
        return Layouts.METHOD.getMethod(method).getSharedMethodInfo().getArity().getPreRequired();
    }

    @Override
    public RubyNode createCallPrimitiveNode(RubyContext context, SourceSection sourceSection, ReturnID returnID) {
        return new CallRubiniusPrimitiveNode(context, sourceSection,
                CallNodeFactory.create(context, sourceSection, new RubyNode[] {
                    new LiteralNode(context, sourceSection, method),
                    new ReadAllArgumentsNode(context, sourceSection),
                    new ReadBlockNode(context, sourceSection, NotProvided.INSTANCE)
        }), returnID);
    }

    @Override
    public RubyNode createInvokePrimitiveNode(RubyContext context, SourceSection sourceSection, RubyNode[] arguments) {
        return CallNodeFactory.create(context, sourceSection, new RubyNode[] {
                new LiteralNode(context, sourceSection, method),
                new NodeArrayToObjectArrayNode(context, sourceSection, arguments),
                new ReadBlockNode(context, sourceSection, NotProvided.INSTANCE)
        });
    }

}
