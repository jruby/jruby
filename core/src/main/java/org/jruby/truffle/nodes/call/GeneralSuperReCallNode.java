/*
 * Copyright (c) 2013-2014 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.nodes.call;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.SourceSection;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.nodes.IndirectCallNode;
import com.oracle.truffle.api.nodes.NodeInfo;
import org.jruby.truffle.nodes.RubyNode;
import org.jruby.truffle.runtime.NilPlaceholder;
import org.jruby.truffle.runtime.RubyArguments;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.control.RaiseException;
import org.jruby.truffle.runtime.core.RubyBasicObject;
import org.jruby.truffle.runtime.core.RubyClass;
import org.jruby.truffle.runtime.core.RubyProc;
import org.jruby.truffle.runtime.core.array.RubyArray;
import org.jruby.truffle.runtime.methods.RubyMethod;

public class GeneralSuperReCallNode extends RubyNode {

    private final String name;
    @Child protected IndirectCallNode callNode;

    public GeneralSuperReCallNode(RubyContext context, SourceSection sourceSection, String name) {
        super(context, sourceSection);
        assert name != null;
        this.name = name;
        callNode = Truffle.getRuntime().createIndirectCallNode();
    }

    @ExplodeLoop
    @Override
    public final Object execute(VirtualFrame frame) {
        // This method is only a simple implementation - it needs proper caching

        CompilerAsserts.neverPartOfCompilation();

        // Lookup method

        final RubyClass selfClass = ((RubyBasicObject) RubyArguments.getSelf(frame.getArguments())).getRubyClass();
        final RubyMethod method = selfClass.getSuperclass().lookupMethod(name);

        if (method == null || method.isUndefined()) {
            CompilerDirectives.transferToInterpreter();
            throw new RaiseException(getContext().getCoreLibrary().nameErrorNoMethod(name, RubyArguments.getSelf(frame.getArguments()).toString()));
        }

        // Call the method

        return callNode.call(frame, method.getCallTarget(), frame.getArguments());
    }

    @Override
    public Object isDefined(VirtualFrame frame) {
        final RubyContext context = getContext();

        try {
            final RubyBasicObject self = context.getCoreLibrary().box(RubyArguments.getSelf(frame.getArguments()));
            final RubyBasicObject receiverRubyObject = context.getCoreLibrary().box(self);

            final RubyMethod method = receiverRubyObject.getRubyClass().getSuperclass().lookupMethod(name);

            if (method == null || method.isUndefined() || !method.isVisibleTo(self)) {
                return NilPlaceholder.INSTANCE;
            } else {
                return context.makeString("super");
            }
        } catch (Exception e) {
            return NilPlaceholder.INSTANCE;
        }
    }

}
