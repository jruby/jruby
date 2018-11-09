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
import org.jruby.platform.Platform;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.SignalFacade;
import org.jruby.util.NoFunctionalitySignalFacade;

import java.util.*;

@JRubyModule(name="Signal")
public class RubySignal {

    private final static SignalFacade SIGNAL_FACADE = initSignalFacade();

    private final static SignalFacade initSignalFacade() {
        try {
            return org.jruby.util.SunSignalFacade.class.newInstance();
        } catch (Throwable e) {
            return new NoFunctionalitySignalFacade();
        }
    }
    
    public static void createSignal(Ruby runtime) {
        // We force java.lang.Process et al to load so that JVM's CHLD handler can be
        // overwritten by users (jruby/jruby#3283)
        if (!Platform.IS_WINDOWS) {
            try {
                Class.forName("java.lang.Process");
                Class.forName("java.lang.UNIXProcess");
            } catch (Throwable t) {
                // if we can't access Process, other things will fail anyway; ignore for now
            }
        }

        RubyModule mSignal = runtime.defineModule("Signal");
        
        mSignal.defineAnnotatedMethods(RubySignal.class);
        //registerThreadDumpSignalHandler(runtime);
    }

    private static final String SIGNAME_PREFIX = "SIG";
    private static final Map<String, Integer> SIGNAME_MAP;
    private static final Map<Integer, String> SIGNUM_MAP;

    static {
        HashMap<String, Integer> signals = new HashMap<>();
        HashMap<Integer, String> signums = new HashMap<>();

        for (Signal s : Signal.values()) {
            // Skip signals not defined on this platform
            if (!s.defined()) continue;

            String desc = s.description();
            if (!desc.startsWith(SIGNAME_PREFIX)) continue;

            desc = signmWithoutPrefix(desc);
            if (!SIGNAME(desc)) continue;

            // replace CLD with CHLD value
            int signo = s.intValue();
            if (s == Signal.SIGCLD)
                signo = Signal.SIGCHLD.intValue();

            // omit unsupported signals
            if (signo >= 20000) continue;

            signals.put(desc, signo);
            signums.put(signo, desc);
        }

        // We always define KILL as 9 on Windows
        if (Platform.IS_WINDOWS) {
            signals.put("KILL", 9);
        }

        SIGNAME_MAP = Collections.unmodifiableMap(signals);
        SIGNUM_MAP = Collections.unmodifiableMap(signums);
    }
    
    public static Map<String, Integer> list() {
        return SIGNAME_MAP;
    }

    @JRubyMethod(meta = true)
    public static IRubyObject list(ThreadContext context, IRubyObject recv) {
        Ruby runtime = recv.getRuntime();
        RubyHash names;

        synchronized (recv) {
            names = (RubyHash) recv.getInternalVariables().getInternalVariable("signal_list");
            if (names == null) {
                names = RubyHash.newHash(runtime);
                for (Map.Entry<String, Integer> sig : RubySignal.list().entrySet()) {
                    names.op_aset(context, runtime.freezeAndDedupString(runtime.newString(sig.getKey())), runtime.newFixnum(sig.getValue()));
                }
                names.op_aset(context, runtime.freezeAndDedupString(runtime.newString("EXIT")), runtime.newFixnum(0));
                recv.getInternalVariables().setInternalVariable("signal_list", names);
            } else {
                names.dup(context);
            }
        }
        
        return names;
    }

    @JRubyMethod(required = 2, meta = true)
    public static IRubyObject __jtrap_kernel(final IRubyObject recv, IRubyObject block, IRubyObject sig) {
        return SIGNAL_FACADE.trap(recv, block, sig);
    }

    @JRubyMethod(required = 1, meta = true)
    public static IRubyObject __jtrap_platform_kernel(final IRubyObject recv, IRubyObject sig) {
        return SIGNAL_FACADE.restorePlatformDefault(recv, sig);
    }

    @JRubyMethod(required = 1, meta = true)
    public static IRubyObject __jtrap_osdefault_kernel(final IRubyObject recv, IRubyObject sig) {
        return SIGNAL_FACADE.restoreOSDefault(recv, sig);
    }

    @JRubyMethod(required = 1, meta = true)
    public static IRubyObject __jtrap_restore_kernel(final IRubyObject recv, IRubyObject sig) {
        return SIGNAL_FACADE.ignore(recv, sig);
    }

    @JRubyMethod(required = 1, meta = true)
    public static IRubyObject signame(ThreadContext context, final IRubyObject recv, IRubyObject rubySig) {
        long sig = rubySig.convertToInteger().getLongValue();
        String signame = signo2signm(sig);
        if (signame == null) {
            if(sig == 0) {
                return RubyString.newString(context.runtime, "EXIT");
            } else {
                throw context.runtime.newArgumentError("invalid signal number: " + rubySig);
            }
        }
        return context.runtime.newString(signame);
    }

    // MRI: signo2signm
    public static String signo2signm(long no) {
        return SIGNUM_MAP.get((int) no);
    }

    // MRI: signm2signo
    public static long signm2signo(String nm) {
        Integer signo = SIGNAME_MAP.get(nm);

        if (signo == null) return 0;

        return signo;
    }

    public static String signmWithPrefix(String nm) {
        return (nm.startsWith(SIGNAME_PREFIX)) ? nm : SIGNAME_PREFIX + nm;
    }

    public static String signmWithoutPrefix(String nm) {
        return (nm.startsWith(SIGNAME_PREFIX)) ? nm.substring(SIGNAME_PREFIX.length()) : nm;
    }

    private static boolean SIGNAME(final String name) {
        switch (name) {
            case "EXIT":
            case "HUP" :
            case "INT" :
            case "QUIT":
            case "ILL" :
            case "TRAP":
            case "IOT" :
            case "ABRT":
            case "EMT" :
            case "FPE" :
            case "KILL":
            case "BUS" :
            case "SEGV":
            case "SYS" :
            case "PIPE":
            case "ALRM":
            case "TERM":
            case "URG" :
            case "STOP":
            case "TSTP":
            case "CONT":
            case "CHLD":
            case "CLD" :
            case "TTIN":
            case "TTOU":
            case "IO"  :
            case "XCPU":
            case "XFSZ":
            case "PROF":
            case "VTALRM":
            case "WINCH":
            case "USR1":
            case "USR2":
            case "LOST":
            case "MSG" :
            case "PWR" :
            case "POLL":
            case "DANGER":
            case "MIGRATE":
            case "PRE" :
            case "GRANT":
            case "RETRACT":
            case "SOUND":
            case "INFO":
                return true;
            default:
                return false;
        }
    }

}
