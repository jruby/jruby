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

    public GeneralBoxedDispatchNode(RubyContext context, SourceSection sourceSection, String name) {
        super(context, sourceSection);

        assert name != null;

        this.name = name;
    }

    @Override
    public Object dispatch(VirtualFrame frame, RubyBasicObject receiverObject, RubyProc blockObject, Object[] argumentsObjects) {
        /*
         * TODO(CS): we should probably have some kind of cache here - even if it's just a hash map.
         * MRI and JRuby do and might avoid some pathological cases.
         */

        try {
            final RubyMethod method = lookup(frame, receiverObject, name);
            return method.call(frame.pack(), receiverObject, blockObject, argumentsObjects);
        } catch (UseMethodMissingException e) {
            missingProfile.enter();

            try {
                final RubyMethod method = lookup(frame, receiverObject, "method_missing");

                final Object[] modifiedArgumentsObjects = new Object[1 + argumentsObjects.length];
                modifiedArgumentsObjects[0] = getContext().newSymbol(name);
                System.arraycopy(argumentsObjects, 0, modifiedArgumentsObjects, 1, argumentsObjects.length);

                return method.call(frame.pack(), receiverObject, blockObject, modifiedArgumentsObjects);
            } catch (UseMethodMissingException e2) {
                doubleMissingProfile.enter();

                throw new RaiseException(getContext().getCoreLibrary().runtimeError(receiverObject.toString() + " didn't have a #method_missing"));
            }
        }
    }

}
