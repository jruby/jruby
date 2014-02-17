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
    //private final InlinableMethodImplementation inlinable;
    //private final RubyRootNode inlinedRoot;

    private final BranchProfile profile = new BranchProfile();

    public ActiveEnterDebugProbe(RubyContext context, Assumption activeAssumption, RubyProc proc) {
        super(context, false);
        this.activeAssumption = activeAssumption;
        //inlinable = ((InlinableMethodImplementation) proc.getMethod().getImplementation());
        //inlinedRoot = inlinable.getCloneOfPristineRootNode();
        this.proc = proc;
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

        final MaterializedFrame materializedFrame = frame.materialize();

        final RubyBinding binding = new RubyBinding(context.getCoreLibrary().getBindingClass(), frame.getArguments(RubyArguments.class).getSelf(), materializedFrame);

        //final RubyArguments arguments = new RubyArguments(inlinable.getDeclarationFrame(), NilPlaceholder.INSTANCE, null, binding);
        //final VirtualFrame inlinedFrame = Truffle.getRuntime().createVirtualFrame(frame.pack(), arguments, inlinable.getFrameDescriptor());
        //inlinedRoot.execute(inlinedFrame);
        proc.call(frame.pack(), binding);
    }

    protected abstract InactiveEnterDebugProbe createInactive();

}
