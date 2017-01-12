/*
 * Copyright (c) 2014, 2017 Oracle and/or its affiliates. All rights reserved. This
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
import org.jruby.truffle.language.RubyNode;
import org.jruby.truffle.language.control.NextException;
import org.jruby.truffle.language.control.RaiseException;
import org.jruby.truffle.language.control.RedoException;
import org.jruby.truffle.language.control.RetryException;

public class CatchForProcNode extends RubyNode {

    @Child private RubyNode body;

    private final BranchProfile redoProfile = BranchProfile.create();
    private final BranchProfile nextProfile = BranchProfile.create();
    private final BranchProfile retryProfile = BranchProfile.create();

    public CatchForProcNode(RubyNode body) {
        this.body = body;
    }

    @Override
    public Object execute(VirtualFrame frame) {
        while (true) {
            try {
                return body.execute(frame);
            } catch (RedoException e) {
                redoProfile.enter();
                getContext().getSafepointManager().poll(this);
                continue;
            } catch (NextException e) {
                nextProfile.enter();
                return e.getResult();
            } catch (RetryException e) {
                retryProfile.enter();
                throw new RaiseException(coreExceptions().syntaxErrorInvalidRetry(this));
            }
        }
    }

}
