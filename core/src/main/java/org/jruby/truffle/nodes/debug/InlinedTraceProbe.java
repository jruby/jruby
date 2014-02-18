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
import org.jruby.truffle.nodes.InlinableMethodImplementation;
import org.jruby.truffle.nodes.RubyRootNode;
import org.jruby.truffle.nodes.debug.RubyProbe;
import org.jruby.truffle.runtime.*;
import org.jruby.truffle.runtime.core.RubyBinding;
import org.jruby.truffle.runtime.core.RubyString;
import org.jruby.truffle.runtime.subsystems.*;

public final class InlinedTraceProbe extends RubyProbe {

    private final Assumption tracingAssumption;

    private final InlinableMethodImplementation inlinable;
    private final RubyRootNode inlinedRoot;

    @CompilerDirectives.CompilationFinal private RubyString event;
    @CompilerDirectives.CompilationFinal private RubyString file;
    @CompilerDirectives.CompilationFinal private int line;
    @CompilerDirectives.CompilationFinal private RubyString className;

    public InlinedTraceProbe(RubyContext context, InlinableMethodImplementation inlinable, final Assumption tracingAssumption) {
        super(context, false);
        this.tracingAssumption = tracingAssumption;
        this.inlinable = inlinable;
        this.inlinedRoot = inlinable.getCloneOfPristineRootNode();
    }

    @Override
    public void enter(Node astNode, VirtualFrame frame) {
        final RubyContext context = (RubyContext) getContext();

        try {
            tracingAssumption.check();
        } catch (InvalidAssumptionException e) {
            // Transition back to inactive, but with the same failed assumption to get it to check the trace proc for itself
            final InactiveTraceProbe inactiveTraceProbe = new InactiveTraceProbe((RubyContext) getContext(), tracingAssumption);
            replace(inactiveTraceProbe);
            inactiveTraceProbe.enter(astNode, frame);
        }

        if (context.getTraceManager().isSuspended()) {
            return;
        }

        final SourceSection sourceSection = astNode.getEncapsulatingSourceSection();

        final Object self = frame.getArguments(RubyArguments.class).getSelf();

        if (event == null) {
            CompilerDirectives.transferToInterpreter();
            event = context.makeString("line");
            file = context.makeString(sourceSection.getSource().getName());
            line = sourceSection.getStartLine();
            className = context.makeString("(unknown)");
        }

        final int objectId = 0;
        final RubyBinding binding = new RubyBinding(context.getCoreLibrary().getBindingClass(), self, frame.materialize());

        context.getTraceManager().setSuspended(true);

        try {
            final RubyArguments arguments = new RubyArguments(inlinable.getDeclarationFrame(), NilPlaceholder.INSTANCE, null, event, file, line, objectId, binding, className);
            final VirtualFrame inlinedFrame = Truffle.getRuntime().createVirtualFrame(frame.pack(), arguments, inlinable.getFrameDescriptor());
            inlinedRoot.execute(inlinedFrame);
        } finally {
            context.getTraceManager().setSuspended(false);
        }
    }

}
