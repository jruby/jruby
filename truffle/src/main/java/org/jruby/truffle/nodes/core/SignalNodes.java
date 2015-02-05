/*
 * Copyright (c) 2013, 2015 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.nodes.core;

import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.source.SourceSection;

import org.jruby.common.IRubyWarnings;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.UndefinedPlaceholder;
import org.jruby.truffle.runtime.core.RubyProc;
import org.jruby.truffle.runtime.core.RubyString;
import org.jruby.truffle.runtime.core.RubySymbol;
import org.jruby.truffle.runtime.signal.ProcSignalHandler;

import sun.misc.Signal;
import sun.misc.SignalHandler;

@CoreClass(name = "Signal")
public abstract class SignalNodes {

    @CoreMethod(names = "trap", isModuleFunction = true, needsBlock = true, required = 1, optional = 1)
    public abstract static class SignalNode extends CoreMethodNode {

        public SignalNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public SignalNode(SignalNode prev) {
            super(prev);
        }

        @Specialization
        public Object trap(RubySymbol signalName, UndefinedPlaceholder command, RubyProc block) {
            return trap(signalName.toString(), block);
        }

        @Specialization
        public Object trap(RubyString signalName, UndefinedPlaceholder command, RubyProc block) {
            return trap(signalName.toString(), block);
        }

        @Specialization
        public Object trap(RubySymbol signalName, RubyProc proc, UndefinedPlaceholder block) {
            return trap(signalName.toString(), proc);
        }

        @Specialization
        public Object trap(RubyString signalName, RubyProc proc, UndefinedPlaceholder block) {
            return trap(signalName.toString(), proc);
        }

        @SuppressWarnings("restriction")
        private Object trap(String signalName, RubyProc block) {
            notDesignedForCompilation();

            final Signal signal = new Signal(signalName);

            final SignalHandler newHandler = new ProcSignalHandler(getContext(), block);
            final SignalHandler oldHandler = Signal.handle(signal, newHandler);

            if (oldHandler instanceof ProcSignalHandler) {
                return ((ProcSignalHandler) oldHandler).getProc();
            }

            return getContext().getCoreLibrary().getNilObject();
        }

        @Specialization
        public Object trap(RubySymbol signalName, RubyString command, UndefinedPlaceholder block) {
            return trap(signalName.toString(), command, block);
        }

        @Specialization
        public Object trap(RubyString signalName, RubyString command, UndefinedPlaceholder block) {
            return trap(signalName.toString(), command, block);
        }

        private Object trap(String signalName, RubyString command, UndefinedPlaceholder block) {
            notDesignedForCompilation();
            getContext().getRuntime().getWarnings().warn(IRubyWarnings.ID.TRUFFLE, Truffle.getRuntime().getCallerFrame().getCallNode().getEncapsulatingSourceSection().getSource().getName(), Truffle.getRuntime().getCallerFrame().getCallNode().getEncapsulatingSourceSection().getStartLine(), "Signal#trap with a string command not implemented yet");
            return getContext().getCoreLibrary().getNilObject();
        }

    }

}
