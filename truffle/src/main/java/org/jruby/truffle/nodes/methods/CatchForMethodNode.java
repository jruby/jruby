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

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.utilities.BranchProfile;
import com.oracle.truffle.api.utilities.ConditionProfile;
import com.oracle.truffle.api.source.SourceSection;
import org.jruby.truffle.nodes.RubyNode;
import org.jruby.truffle.runtime.ReturnID;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.control.RaiseException;
import org.jruby.truffle.runtime.control.RetryException;
import org.jruby.truffle.runtime.control.ReturnException;

public class CatchForMethodNode extends RubyNode {

    @Child private RubyNode body;
    private final ReturnID returnID;

    private final BranchProfile returnProfile = BranchProfile.create();
    private final ConditionProfile matchingReturnProfile = ConditionProfile.createBinaryProfile();

    public CatchForMethodNode(RubyContext context, SourceSection sourceSection, RubyNode body, ReturnID returnID) {
        super(context, sourceSection);
        this.body = body;
        this.returnID = returnID;
    }

    @Override
    public Object execute(VirtualFrame frame) {
        try {
            return body.execute(frame);
        } catch (ReturnException e) {
            returnProfile.enter();
            if (matchingReturnProfile.profile(e.getReturnID() == returnID)) {
                return e.getValue();
            } else {
                throw e;
            }
        } catch (RetryException e) {
            CompilerDirectives.transferToInterpreter();
            throw new RaiseException(getContext().getCoreLibrary().syntaxError("Invalid retry", this));
        }
    }

}
