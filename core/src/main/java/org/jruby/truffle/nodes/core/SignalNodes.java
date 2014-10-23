/*
 * Copyright (c) 2013, 2014 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.nodes.core;

import com.oracle.truffle.api.*;
import com.oracle.truffle.api.source.*;
import com.oracle.truffle.api.dsl.*;
import org.jruby.common.IRubyWarnings;
import org.jruby.truffle.runtime.*;
import org.jruby.truffle.runtime.core.RubyProc;
import org.jruby.truffle.runtime.core.RubyString;
import org.jruby.truffle.runtime.signal.ProcSignalHandler;
import sun.misc.Signal;
import sun.misc.SignalHandler;

@CoreClass(name = "Signal")
public abstract class SignalNodes {

    @CoreMethod(names = "trap", isModuleFunction = true, needsBlock = true, minArgs = 1, maxArgs = 2)
    public abstract static class SignalNode extends CoreMethodNode {

        public SignalNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public SignalNode(SignalNode prev) {
            super(prev);
        }

        @Specialization
        public Object trap(RubyString signalName, UndefinedPlaceholder command, final RubyProc block) {
            notDesignedForCompilation();

            final Signal signal = new Signal(signalName.toString());

            final SignalHandler newHandler = new ProcSignalHandler(block);
            final SignalHandler oldHandler = Signal.handle(signal, newHandler);

            if (oldHandler instanceof ProcSignalHandler) {
                return ((ProcSignalHandler) oldHandler).getProc();
            }

            return getContext().getCoreLibrary().getNilObject();
        }

        @Specialization
        public Object trap(RubyString signalName, RubyString command, UndefinedPlaceholder block) {
            notDesignedForCompilation();
            getContext().getRuntime().getWarnings().warn(IRubyWarnings.ID.TRUFFLE, Truffle.getRuntime().getCallerFrame().getCallNode().getEncapsulatingSourceSection().getSource().getName(), Truffle.getRuntime().getCallerFrame().getCallNode().getEncapsulatingSourceSection().getStartLine(), "Signal#trap with a string command not implemented yet");
            return getContext().getCoreLibrary().getNilObject();
        }

    }

}
