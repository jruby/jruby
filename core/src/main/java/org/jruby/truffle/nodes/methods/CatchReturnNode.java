/*
 * Copyright (c) 2013 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.nodes.methods;

import com.oracle.truffle.api.*;
import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.api.utilities.*;
import com.oracle.truffle.api.CompilerDirectives.*;
import org.jruby.truffle.nodes.*;
import org.jruby.truffle.runtime.*;
import org.jruby.truffle.runtime.control.*;

/**
 * Catch a {@code return} jump at the root of a method.
 */
public class CatchReturnNode extends RubyNode {

    @Child protected RubyNode body;
    private final long returnID;

    /*
     * Methods catch the return exception and use it as the return value. Procs don't catch return, as returns are
     * lexically associated with the enclosing method. However when a proc becomes a method, such as through
     * Module#define_method it then starts to behave as a method and must catch the return. This flag allows us to turn
     * on that functionality. We don't need to deoptimize as it will always be a new copy of the tree.
     */

    @CompilationFinal private boolean isProc;

    private final BranchProfile returnProfile = new BranchProfile();
    private final BranchProfile returnToOtherMethodProfile = new BranchProfile();

    public CatchReturnNode(RubyContext context, SourceSection sourceSection, RubyNode body, long returnID, boolean isProc) {
        super(context, sourceSection);
        this.body = adoptChild(body);
        this.returnID = returnID;
        this.isProc = isProc;
    }

    @Override
    public Object execute(VirtualFrame frame) {
        if (isProc) {
            return body.execute(frame);
        } else {
            try {
                return body.execute(frame);
            } catch (ReturnException e) {
                returnProfile.enter();

                if (e.getReturnID() == returnID) {
                    return e.getValue();
                } else {
                    returnToOtherMethodProfile.enter();
                    throw e;
                }
            }
        }
    }

    public void setIsProc(boolean isProc) {
        this.isProc = isProc;
    }

}
