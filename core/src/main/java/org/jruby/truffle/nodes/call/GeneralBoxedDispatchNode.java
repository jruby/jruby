/*
 * Copyright (c) 2013 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.nodes.call;

import com.oracle.truffle.api.*;
import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.api.nodes.IndirectCallNode;
import com.oracle.truffle.api.utilities.BranchProfile;
import org.jruby.truffle.runtime.*;
import org.jruby.truffle.runtime.control.RaiseException;
import org.jruby.truffle.runtime.core.*;
import org.jruby.truffle.runtime.methods.*;

/**
 * A node in the dispatch chain that does no caching and looks up methods from scratch each time it
 * is called.
 */
public class GeneralBoxedDispatchNode extends BoxedDispatchNode {

    private final String name;
    private final BranchProfile missingProfile = new BranchProfile();
    private final BranchProfile doubleMissingProfile = new BranchProfile();

    @Child private final IndirectCallNode callNode;

    public GeneralBoxedDispatchNode(RubyContext context, SourceSection sourceSection, String name) {
        super(context, sourceSection);

        assert name != null;

        this.name = name;

        callNode = Truffle.getRuntime().createIndirectCallNode();
    }

    @Override
    public Object dispatch(VirtualFrame frame, RubyBasicObject receiverObject, RubyProc blockObject, Object[] argumentsObjects) {
        // TODO(CS): this whole method needs to use child nodes for boxing, lookup and stuff

        /*
         * TODO(CS): we should probably have some kind of cache here - even if it's just a hash map.
         * MRI and JRuby do and might avoid some pathological cases.
         */

        final RubyBasicObject boxedCallingSelf = getContext().getCoreLibrary().box(RubyArguments.getSelf(frame.getArguments()));

        try {
            final RubyMethod method = lookup(boxedCallingSelf, receiverObject, name);
            return callNode.call(frame, method.getCallTarget(), RubyArguments.create(method.getDeclarationFrame(), receiverObject, blockObject, argumentsObjects));
        } catch (UseMethodMissingException e) {
            missingProfile.enter();

            try {
                final RubyMethod method = lookup(boxedCallingSelf, receiverObject, "method_missing");

                final Object[] modifiedArgumentsObjects = new Object[1 + argumentsObjects.length];
                modifiedArgumentsObjects[0] = getContext().newSymbol(name);
                System.arraycopy(argumentsObjects, 0, modifiedArgumentsObjects, 1, argumentsObjects.length);

                return callNode.call(frame, method.getCallTarget(), RubyArguments.create(method.getDeclarationFrame(), receiverObject, blockObject, modifiedArgumentsObjects));
            } catch (UseMethodMissingException e2) {
                doubleMissingProfile.enter();

                throw new RaiseException(getContext().getCoreLibrary().runtimeError(receiverObject.toString() + " didn't have a #method_missing"));
            }
        }
    }

}
