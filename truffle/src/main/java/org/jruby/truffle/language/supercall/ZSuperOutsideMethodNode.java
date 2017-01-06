/*
 * Copyright (c) 2015, 2017 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.language.supercall;

import com.oracle.truffle.api.frame.VirtualFrame;
import org.jcodings.specific.UTF8Encoding;
import org.jruby.truffle.language.RubyNode;
import org.jruby.truffle.language.arguments.RubyArguments;
import org.jruby.truffle.language.control.RaiseException;
import org.jruby.truffle.language.methods.InternalMethod;

/**
 * Represents a super call with implicit arguments without a surrounding method
 */
public class ZSuperOutsideMethodNode extends RubyNode {

    final boolean insideDefineMethod;
    
    @Child private LookupSuperMethodNode lookupSuperMethodNode = LookupSuperMethodNodeGen.create(null);

    public ZSuperOutsideMethodNode(boolean insideDefineMethod) {
        this.insideDefineMethod = insideDefineMethod;
    }

    @Override
    public final Object execute(VirtualFrame frame) {
        // This is MRI behavior
        if (insideDefineMethod) { // TODO (eregon, 22 July 2015): This check should be more dynamic.
            throw new RaiseException(coreExceptions().runtimeError(
                    "implicit argument passing of super from method defined by define_method() is not supported. Specify all arguments explicitly.", this));
        } else {
            throw new RaiseException(coreExceptions().noSuperMethodOutsideMethodError(this));
        }
    }

    @Override
    public Object isDefined(VirtualFrame frame) {
        final Object self = RubyArguments.getSelf(frame);
        final InternalMethod superMethod = lookupSuperMethodNode.executeLookupSuperMethod(frame, self);

        if (superMethod == null) {
            return nil();
        } else {
            return create7BitString("super", UTF8Encoding.INSTANCE);
        }
    }

}
