/*
 * Copyright (c) 2013, 2017 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.language.methods;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.api.profiles.ConditionProfile;
import org.jruby.truffle.language.RubyNode;
import org.jruby.truffle.language.control.RaiseException;
import org.jruby.truffle.language.control.RetryException;
import org.jruby.truffle.language.control.ReturnException;
import org.jruby.truffle.language.control.ReturnID;

public class CatchForMethodNode extends RubyNode {

    private final ReturnID returnID;

    @Child private RubyNode body;

    private final ConditionProfile matchingReturnProfile = ConditionProfile.createBinaryProfile();
    private final BranchProfile retryProfile = BranchProfile.create();

    public CatchForMethodNode(ReturnID returnID, RubyNode body) {
        this.returnID = returnID;
        this.body = body;
    }

    @Override
    public Object execute(VirtualFrame frame) {
        try {
            return body.execute(frame);
        } catch (ReturnException e) {
            if (matchingReturnProfile.profile(e.getReturnID() == returnID)) {
                return e.getValue();
            } else {
                throw e;
            }
        } catch (RetryException e) {
            retryProfile.enter();
            throw new RaiseException(coreExceptions().syntaxErrorInvalidRetry(this));
        }
    }

}
