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
        private final IRubyObject signal_object;
        private final String signal;

        public JRubySignalHandler(Ruby runtime, IRubyObject block, IRubyObject signal_object, String signal) {
            this.runtime = runtime;
            this.block = block;
            this.signal_object = signal_object;
            this.signal = signal;
        }

        public void handle(Signal signal) {
            ThreadContext context = runtime.getCurrentContext();
            try {
                block.callMethod(context, "call");
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

    public IRubyObject trap(final IRubyObject recv, IRubyObject arg1, IRubyObject arg2, IRubyObject arg3) {
        final JRubySignalHandler handler = new JRubySignalHandler(recv.getRuntime(), arg1, arg2, arg3.toString());
        
        final SignalHandler oldHandler;
        try {
            oldHandler = Signal.handle(new Signal(handler.signal), handler);
        } catch (Exception e) {
            throw recv.getRuntime().newArgumentError(e.getMessage());
        }
        if(oldHandler instanceof JRubySignalHandler) {
            return ((JRubySignalHandler)oldHandler).block;
        } else {
            return RubyProc.newProc(recv.getRuntime(), CallBlock.newCallClosure(recv, (RubyModule)recv, 
                                                                                Arity.noArguments(), new BlockCallback(){
                                                                                        public IRubyObject call(ThreadContext context, IRubyObject[] args, Block block) {
                                                                                            oldHandler.handle(new Signal(handler.signal));
                                                                                            return recv.getRuntime().getNil();
                                                                                        }
                                                                                    }, recv.getRuntime().getCurrentContext()), Block.Type.NORMAL);
        }
    }
}// SunSignalFacade
