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
 * Copyright (C) 2007 Ola Bini <ola@ologix.com>
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
package org.jruby;

import org.jruby.runtime.Block;
import org.jruby.runtime.CallbackFactory;
import org.jruby.runtime.CallType;
import org.jruby.runtime.builtin.IRubyObject;

/**
 * Placeholder until/if we can support this
 *
 * @author <a href="mailto:ola.bini@ki.se">Ola Bini</a>
 */
public class RubySignal {
    public static void createSignal(Ruby runtime) {
        RubyModule mSignal = runtime.defineModule("Signal");
        CallbackFactory cf = runtime.callbackFactory(RubySignal.class);
        mSignal.getMetaClass().defineMethod("trap", cf.getOptSingletonMethod("trap"));
        mSignal.getMetaClass().defineMethod("__jtrap_kernel", cf.getOptSingletonMethod("__jtrap_kernel"));
    }

    private final static class JRubySignalHandler implements sun.misc.SignalHandler {
        public Ruby runtime;
        public IRubyObject block;
        public IRubyObject signal_object;
        public String signal;

        public void handle(sun.misc.Signal signal) {
            try {
                block.callMethod(runtime.getCurrentContext(), "call", new IRubyObject[0]);
            } catch(org.jruby.exceptions.RaiseException e) {
                try {
                    runtime.getClass("Thread").callMethod(runtime.getCurrentContext(), "main", new IRubyObject[0]).callMethod(runtime.getCurrentContext(), "raise", new IRubyObject[]{e.getException()});
                } catch(Exception ignored) {}
            } finally {
                sun.misc.Signal.handle(new sun.misc.Signal(this.signal), this);
            }
        }
    }

    public static IRubyObject trap(IRubyObject recv, IRubyObject[] args, Block block) {
        recv.getRuntime().getLoadService().require("jsignal");
        return recv.getRuntime().getKernel().callMethod(recv.getRuntime().getCurrentContext(), "__jtrap", args, CallType.FUNCTIONAL, block);
    }

    public static IRubyObject __jtrap_kernel(final IRubyObject recv, IRubyObject[] args, Block unused) {
        final JRubySignalHandler handler = new JRubySignalHandler();
        handler.runtime = recv.getRuntime();
        handler.block = args[0];
        handler.signal_object = args[1];
        handler.signal = args[2].toString();
        final sun.misc.SignalHandler oldHandler = sun.misc.Signal.handle(new sun.misc.Signal(handler.signal), handler);
        if(oldHandler instanceof JRubySignalHandler) {
            return ((JRubySignalHandler)oldHandler).block;
        } else {
            return RubyProc.newProc(recv.getRuntime(), new org.jruby.runtime.CallBlock(recv, (RubyModule)recv, 
                                                                                org.jruby.runtime.Arity.noArguments(), new org.jruby.runtime.BlockCallback(){
                                                                                        public IRubyObject call(org.jruby.runtime.ThreadContext context, IRubyObject[] args, Block block) {
                                                                                            oldHandler.handle(new sun.misc.Signal(handler.signal));
                                                                                            return recv.getRuntime().getNil();
                                                                                        }
                                                                                    }, recv.getRuntime().getCurrentContext()), Block.Type.NORMAL);
        }
    }
}// RubySignal
