/***** BEGIN LICENSE BLOCK *****
 * Version: EPL 1.0/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Eclipse Public
 * License Version 1.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.eclipse.org/legal/epl-v10.html
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
 * use your version of this file under the terms of the EPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the EPL, the GPL or the LGPL.
 ***** END LICENSE BLOCK *****/
package org.jruby;

import jnr.constants.platform.Signal;
import org.jruby.anno.JRubyMethod;
import org.jruby.anno.JRubyModule;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

import org.jruby.util.SignalFacade;
import org.jruby.util.NoFunctionalitySignalFacade;

import java.util.*;

@JRubyModule(name="Signal")
public class RubySignal {
    private final static SignalFacade SIGNALS = getSignalFacade();

    private final static SignalFacade getSignalFacade() {
        try {
            Class realFacadeClass = Class.forName("org.jruby.util.SunSignalFacade");
            return (SignalFacade)realFacadeClass.newInstance();
        } catch(Throwable e) {
            return new NoFunctionalitySignalFacade();
        }
    }
    
    public static void createSignal(Ruby runtime) {
        RubyModule mSignal = runtime.defineModule("Signal");
        
        mSignal.defineAnnotatedMethods(RubySignal.class);
        //registerThreadDumpSignalHandler(runtime);
    }
    
    @JRubyMethod(module = true)
    public static IRubyObject list(ThreadContext context, IRubyObject recv) {
        Ruby runtime = recv.getRuntime();
        RubyHash names = RubyHash.newHash(runtime);
        for (Signal s : Signal.values()) {
            if (!s.description().startsWith("SIG")) continue;
            if (!RUBY_18_SIGNALS.contains(s.description().substring(3))) continue;

            // replace CLD with CHLD value
            long longValue = s.longValue();
            if (s == Signal.SIGCLD) longValue = Signal.SIGCHLD.longValue();

            // omit unsupported signals
            if (longValue >= 20000) continue;

            names.op_aset(context, runtime.newString(s.description().substring("SIG".length())), runtime.newFixnum(longValue));
        }
        names.op_aset(context, runtime.newString("EXIT"), runtime.newFixnum(0));
        return names;
    }

    @JRubyMethod(name = "__jtrap_kernel", required = 2, module = true)
    public static IRubyObject __jtrap_kernel(final IRubyObject recv, IRubyObject block, IRubyObject sig) {
        return SIGNALS.trap(recv, block, sig);
    }

    @JRubyMethod(name = "__jtrap_platform_kernel", required = 1, module = true)
    public static IRubyObject __jtrap_platform_kernel(final IRubyObject recv, IRubyObject sig) {
        return SIGNALS.restorePlatformDefault(recv, sig);
    }

    @JRubyMethod(name = "__jtrap_osdefault_kernel", required = 1, module = true)
    public static IRubyObject __jtrap_osdefault_kernel(final IRubyObject recv, IRubyObject sig) {
        return SIGNALS.restoreOSDefault(recv, sig);
    }

    @JRubyMethod(name = "__jtrap_ignore_kernel", required = 1, module = true)
    public static IRubyObject __jtrap_restore_kernel(final IRubyObject recv, IRubyObject sig) {
        return SIGNALS.ignore(recv, sig);
    }
    
    private static final Set<String> RUBY_18_SIGNALS;
    static {
        RUBY_18_SIGNALS = new HashSet<String>();
        for (String name : new String[] {
                "EXIT",
                "HUP",
                "INT",
                "QUIT",
                "ILL",
                "TRAP",
                "IOT",
                "ABRT",
                "EMT",
                "FPE",
                "KILL",
                "BUS",
                "SEGV",
                "SYS",
                "PIPE",
                "ALRM",
                "TERM",
                "URG",
                "STOP",
                "TSTP",
                "CONT",
                "CHLD",
                "CLD",
                "TTIN",
                "TTOU",
                "IO",
                "XCPU",
                "XFSZ",
                "VTALRM",
                "PROF",
                "WINCH",
                "USR1",
                "USR2",
                "LOST",
                "MSG",
                "PWR",
                "POLL",
                "DANGER",
                "MIGRATE",
                "PRE",
                "GRANT",
                "RETRACT",
                "SOUND",
                "INFO",
        }) {
            RUBY_18_SIGNALS.add(name);
        }
    }
}// RubySignal
