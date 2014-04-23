/*
 * Copyright (c) 2013, 2014 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.nodes.debug;

import com.oracle.truffle.api.*;
import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.api.nodes.*;
import com.oracle.truffle.api.utilities.BranchProfile;
import org.jruby.truffle.nodes.*;
import org.jruby.truffle.runtime.*;
import org.jruby.truffle.runtime.core.*;

public abstract class ActiveEnterDebugProbe extends RubyProbe {

    private final Assumption activeAssumption;

    private final RubyProc proc;
    @Child protected DirectCallNode callNode;

    private final BranchProfile profile = new BranchProfile();

    public ActiveEnterDebugProbe(RubyContext context, Assumption activeAssumption, RubyProc proc) {
        super(context, false);
        this.activeAssumption = activeAssumption;
        this.proc = proc;

        callNode = Truffle.getRuntime().createDirectCallNode(proc.getMethod().getCallTarget());

        if (callNode.isInlinable()) {
            callNode.forceInlining();
        }
    }

    @Override
    public void enter(Node astNode, VirtualFrame frame) {
        profile.enter();

        try {
            activeAssumption.check();
        } catch (InvalidAssumptionException e) {
            replace(createInactive());
            return;
        }

        final RubyBinding binding = new RubyBinding(context.getCoreLibrary().getBindingClass(), new RubyArguments(frame.getArguments()).getSelf(), frame.materialize());
        callNode.call(frame, RubyArguments.create(proc.getMethod().getDeclarationFrame(), proc.getSelfCapturedInScope(), proc.getBlockCapturedInScope(), binding));
    }

    protected abstract InactiveEnterDebugProbe createInactive();

}
