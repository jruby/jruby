/***** BEGIN LICENSE BLOCK *****
 * Version: CPL 1.0/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Common Public
 * License Version 1.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.eclipse.org/legal/cpl-v10.html
 *
 * Software distributed under the License is distributed on an "AS
 * IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * rights and limitations under the License.
 *
 * Copyright (C) 2007 Ola Bini <ola.bini@gmail.com>
 * 
 * Alternatively, the contents of this file may be used under the terms of
 * either of the GNU General Public License Version 2 or later (the "GPL"),
 * or the GNU Lesser General Public License Version 2.1 or later (the "LGPL"),
 * in which case the provisions of the GPL or the LGPL are applicable instead
 * of those above. If you wish to allow use of your version of this file only
 * under the terms of either the GPL or the LGPL, and not to allow others to
 * use your version of this file under the terms of the CPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the CPL, the GPL or the LGPL.
 ***** END LICENSE BLOCK *****/
package org.jruby.util;

import org.jruby.Ruby;
import org.jruby.RubyModule;
import org.jruby.RubyProc;

import org.jruby.exceptions.MainExitException;
import org.jruby.exceptions.RaiseException;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.CallBlock;
import org.jruby.runtime.Arity;
import org.jruby.runtime.Block;
import org.jruby.runtime.BlockCallback;
import org.jruby.runtime.ThreadContext;

import sun.misc.Signal;
import sun.misc.SignalHandler;

/**
 * @author <a href="mailto:ola.bini@gmail.com">Ola Bini</a>
 */
public class SunSignalFacade implements SignalFacade {
    private final static class JRubySignalHandler implements SignalHandler {
        private final Ruby runtime;
        private final IRubyObject block;
        private final String signal;
        private final BlockCallback blockCallback;

        public JRubySignalHandler(Ruby runtime, IRubyObject block, String signal) {
            this(runtime, block, null, signal);
        }

        public JRubySignalHandler(Ruby runtime, BlockCallback callback, String signal) {
            this(runtime, null, callback, signal);
        }

        private JRubySignalHandler(Ruby runtime, IRubyObject block, BlockCallback callback, String signal) {
            this.runtime = runtime;
            this.block = block;
            this.blockCallback = callback;
            this.signal = signal;
        }

        public void handle(Signal signal) {
            ThreadContext context = runtime.getCurrentContext();
            try {
                if (block != null) {
                    block.callMethod(context, "call");
                } else {
                    blockCallback.call(context, new IRubyObject[0], Block.NULL_BLOCK);
                }
            } catch(RaiseException e) {
                try {
                    runtime.getThread().callMethod(context, "main")
                        .callMethod(context, "raise", e.getException());
                } catch(Exception ignored) {}
            } catch (MainExitException mee) {
                runtime.getThreadService().getMainThread().kill();
            } finally {
                Signal.handle(new Signal(this.signal), this);
            }
        }
    }

    public IRubyObject trap(final IRubyObject recv, IRubyObject blk, IRubyObject sig) {
        return trap(recv.getRuntime(), new JRubySignalHandler(recv.getRuntime(), blk, sig.toString()));
    }
        
    public IRubyObject trap(final Ruby runtime, BlockCallback blk, String sig) {
        return trap(runtime, new JRubySignalHandler(runtime, blk, sig));
    }

    private IRubyObject trap(final Ruby runtime, final JRubySignalHandler handler) {
        final SignalHandler oldHandler;
        final Signal signal;

        try {
            signal = new Signal(handler.signal);
        } catch (Throwable e) {
            return runtime.getNil();
        }

        try {
            oldHandler = Signal.handle(signal, handler);
        } catch (Exception e) {
            throw runtime.newArgumentError(e.getMessage());
        }

        BlockCallback callback = null;
        if (oldHandler instanceof JRubySignalHandler) {
            JRubySignalHandler jsHandler = (JRubySignalHandler) oldHandler;
            if (jsHandler.blockCallback != null) {
                callback = jsHandler.blockCallback;
            } else {
                return jsHandler.block;
            }
        }
        if (callback == null) {
            callback = new BlockCallback() {
                public IRubyObject call(ThreadContext context, IRubyObject[] args, Block block) {
                    oldHandler.handle(new Signal(handler.signal));
                    return runtime.getNil();
                }
            };
        }
        final RubyModule signalModule = runtime.getModule("Signal");
        Block block = CallBlock.newCallClosure(signalModule, signalModule, Arity.noArguments(),
                callback, runtime.getCurrentContext());
        return RubyProc.newProc(runtime, block, Block.Type.NORMAL);
    }
}// SunSignalFacade
