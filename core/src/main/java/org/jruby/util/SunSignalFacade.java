/***** BEGIN LICENSE BLOCK *****
 * Version: EPL 2.0/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Eclipse Public
 * License Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.eclipse.org/legal/epl-v20.html
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
 * use your version of this file under the terms of the EPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the EPL, the GPL or the LGPL.
 ***** END LICENSE BLOCK *****/

package org.jruby.util;

import org.jruby.Ruby;
import org.jruby.RubyArray;
import org.jruby.RubyBoolean;
import org.jruby.RubyFixnum;
import org.jruby.RubyProc;

import org.jruby.exceptions.MainExitException;
import org.jruby.exceptions.RaiseException;
import org.jruby.runtime.Signature;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.CallBlock;
import org.jruby.runtime.Block;
import org.jruby.runtime.BlockCallback;
import org.jruby.runtime.ThreadContext;

import sun.misc.Signal;
import sun.misc.SignalHandler;

import java.util.HashMap;
import java.util.Map;

import static org.jruby.api.Access.getModule;
import static org.jruby.api.Convert.asBoolean;
import static org.jruby.api.Convert.asFixnum;
import static org.jruby.api.Create.newArray;
import static org.jruby.api.Create.newString;

public class SunSignalFacade implements SignalFacade {
    /**
     * Remembers the original signal handlers before JRuby started messing around with them,
     * to emulate {@code Signal.trap(...,"DEFAULT")} that's supposed to restore the platform
     * default handler.
     */
    private final Map<Signal, SignalHandler> original = new HashMap<>(4);
    private final Map<String, SignalHandler> fakeOriginal = new HashMap<>(4);

    /**
     * This is used instead of SignalHandler.SIG_IGN as {@code Signal.handle(sig, anyHandler)} seems to no
     * longer work after {@code Signal.handle(sig, SIG_IGN)}. See https://github.com/jruby/jruby/pull/6584
     */
    private static final SignalHandler IGNORE = sig -> {};
    
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
            // reinstall handler for platforms that clear it (HP-UX)
            Signal.handle(new Signal(this.signal), this);

            var context = runtime.getCurrentContext();
            IRubyObject oldExc = context.getErrorInfo(); // Save $!
            try {
                RubyFixnum signum = asFixnum(context, signal.getNumber());
                if (block != null) {
                    block.callMethod(context, "call", signum);
                } else {
                    blockCallback.call(context, signum, Block.NULL_BLOCK);
                }
            } catch(RaiseException e) {
                try {
                    runtime.getThread().callMethod(context, "main")
                        .callMethod(context, "raise", e.getException());
                } catch(Exception ignored) {}
                context.setErrorInfo(oldExc); // Restore $!
            } catch (MainExitException mee) {
                runtime.getThreadService().getMainThread().kill();
            }
        }
    }

    public IRubyObject trap(ThreadContext context, final IRubyObject recv, IRubyObject blk, IRubyObject sig) {
        return trap(context, new JRubySignalHandler(context.runtime, blk, sig.toString()));
    }
        
    public IRubyObject trap(ThreadContext context, BlockCallback blk, String sig) {
        return trap(context, new JRubySignalHandler(context.runtime, blk, sig));
    }

    private IRubyObject trap(ThreadContext context, final JRubySignalHandler handler) {
        return trap(context, handler.signal, handler);
    }

    public IRubyObject restorePlatformDefault(ThreadContext context, IRubyObject recv, IRubyObject sig) {
        SignalHandler handler;
        try {
            synchronized (original) {
                handler = original.get(new Signal(sig.toString()));
            }
        } catch (IllegalArgumentException e) {
            handler = null;
        }
        if (handler != null) {
            return trap(context, sig.toString(), handler);
        } else {
            // JRuby hasn't touched this signal handler, so it should be the platform default already
            // We still need to return the handler if one exists, though.

            synchronized (fakeOriginal) {
                handler = fakeOriginal.remove(sig.toString());
            }
            return getSignalResult(context, handler, true);
        }
    }

    public IRubyObject restoreOSDefault(ThreadContext context, IRubyObject recv, IRubyObject sig) {
        return trap(context, sig.toString(), SignalHandler.SIG_DFL);
    }

    public IRubyObject ignore(ThreadContext context, IRubyObject recv, IRubyObject sig) {
        return trap(context, sig.toString(), IGNORE);
    }

    private IRubyObject trap(ThreadContext context, final String signalName, final SignalHandler handler) {
        boolean handled;

        SignalHandler oldHandler;
        Signal signal;
        try {
            signal = new Signal(signalName);
            oldHandler = Signal.handle(signal, handler);
            synchronized (original) {
                if (!original.containsKey(signal))
                    original.put(signal, oldHandler);
            }
            handled = true;
        } catch (IllegalArgumentException e) {
            oldHandler = fakeOriginal.get(signalName);
            synchronized (fakeOriginal) {
                fakeOriginal.put(signalName, handler);
            }
            // EXIT is a special pseudo-signal. We want to mark this signal as handled if so.
            handled = signalName.equals("EXIT");
        }

        return getSignalResult(context, oldHandler, handled);
    }

    private static IRubyObject getSignalResult(ThreadContext context, final SignalHandler oldHandler, boolean handled) {
        RubyBoolean handledBoolean = asBoolean(context, handled);
        IRubyObject ret;
        BlockCallback callback = null;

        if (oldHandler instanceof JRubySignalHandler jsHandler) {
            if (jsHandler.blockCallback != null) {
                callback = jsHandler.blockCallback;
            } else {
                ret = jsHandler.block;
                return newArray(context, ret, handledBoolean);
            }
        }

        if (callback == null) {
            if (oldHandler == SignalHandler.SIG_DFL) {
                ret = newString(context, "SYSTEM_DEFAULT");
            } else if (oldHandler == IGNORE) {
                ret = newString(context, "IGNORE");
            } else {
                ret = newString(context,"DEFAULT");
            }
        } else {
            Block block = CallBlock.newCallClosure(context,
                    getModule(context, "Signal"), Signature.NO_ARGUMENTS, callback);
            ret = RubyProc.newProc(context.runtime, block, Block.Type.PROC);
        }

        return RubyArray.newArrayMayCopy(context.runtime, ret, handledBoolean);
    }

}// SunSignalFacade
