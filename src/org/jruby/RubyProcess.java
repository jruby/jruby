/*
 **** BEGIN LICENSE BLOCK *****
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
 * Copyright (C) 2004 Thomas E Enebo <enebo@acm.org>
 * Copyright (C) 2004 Jan Arne Petersen <jpetersen@uni-bonn.de>
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

import org.jruby.anno.JRubyClass;
import org.jruby.anno.JRubyMethod;
import org.jruby.anno.JRubyModule;
import org.jruby.ext.posix.POSIX;
import org.jruby.runtime.Arity;
import org.jruby.runtime.Block;
import org.jruby.runtime.BlockCallback;
import org.jruby.runtime.CallBlock;
import org.jruby.runtime.CallbackFactory;
import org.jruby.runtime.MethodIndex;
import org.jruby.runtime.ObjectAllocator;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.Visibility;
import org.jruby.runtime.builtin.IRubyObject;


/**
 */

@JRubyModule(name="Process")
public class RubyProcess {

    public static RubyModule createProcessModule(Ruby runtime) {
        RubyModule process = runtime.defineModule("Process");
        runtime.setProcess(process);
        
        // TODO: NOT_ALLOCATABLE_ALLOCATOR is probably ok here. Confirm. JRUBY-415
        RubyClass process_status = process.defineClassUnder("Status", runtime.getObject(), ObjectAllocator.NOT_ALLOCATABLE_ALLOCATOR);
        runtime.setProcStatus(process_status);
        
        RubyModule process_uid = process.defineModuleUnder("UID");
        runtime.setProcUID(process_uid);
        
        RubyModule process_gid = process.defineModuleUnder("GID");
        runtime.setProcGID(process_gid);
        
        RubyModule process_sys = process.defineModuleUnder("Sys");
        runtime.setProcSys(process_sys);

        CallbackFactory process_statusCallbackFactory = runtime.callbackFactory(RubyProcess.RubyStatus.class);
        
        process.defineAnnotatedMethods(RubyProcess.class);
        process_status.defineAnnotatedMethods(RubyStatus.class);
        process_uid.defineAnnotatedMethods(UserID.class);
        process_gid.defineAnnotatedMethods(GroupID.class);
        process_sys.defineAnnotatedMethods(Sys.class);

        process.defineConstant("PRIO_PROCESS", runtime.newFixnum(0));
        process.defineConstant("PRIO_PGRP", runtime.newFixnum(1));
        process.defineConstant("PRIO_USER", runtime.newFixnum(2));
        
        process.defineConstant("WNOHANG", runtime.newFixnum(1));
        
        return process;
    }

    @JRubyClass(name="Process::Status")
    public static class RubyStatus extends RubyObject {
        private long status = 0L;
        
        private static final long EXIT_SUCCESS = 0L;
        public RubyStatus(Ruby runtime, RubyClass metaClass, long status) {
            super(runtime, metaClass);
            this.status = status;
        }
        
        public static RubyStatus newProcessStatus(Ruby runtime, long status) {
            return new RubyStatus(runtime, runtime.getProcStatus(), status);
        }
        
        // Bunch of methods still not implemented
        @JRubyMethod(name = {"to_int", "pid", "stopped?", "stopsig", "signaled?", "termsig?", "exited?", "coredump?"})
        public IRubyObject not_implemented() {
            String error = "Process::Status#" + getRuntime().getCurrentContext().getFrameName() + " not implemented";
            throw getRuntime().newNotImplementedError(error);
        }
        
        @JRubyMethod(name = {"&"})
        public IRubyObject not_implemented1(IRubyObject arg) {
            String error = "Process::Status#" + getRuntime().getCurrentContext().getFrameName() + " not implemented";
            throw getRuntime().newNotImplementedError(error);
        }
        
        @JRubyMethod
        public IRubyObject exitstatus() {
            return getRuntime().newFixnum(status);
        }
        
        @JRubyMethod(name = ">>")
        public IRubyObject op_rshift(IRubyObject other) {
            long shiftValue = other.convertToInteger().getLongValue();
            return getRuntime().newFixnum(status >> shiftValue);
        }
        
        @JRubyMethod(name = "==")
        public IRubyObject op_equal(ThreadContext context, IRubyObject other) {
            return other.callMethod(context, MethodIndex.EQUALEQUAL, "==", this.to_i());
        }

        @JRubyMethod
        public IRubyObject to_i() {
            return getRuntime().newFixnum(shiftedValue());
        }
        
        @JRubyMethod
        public IRubyObject to_s() {
            return getRuntime().newString(String.valueOf(shiftedValue()));
        }
        
        @JRubyMethod
        public IRubyObject inspect() {
            return getRuntime().newString("#<Process::Status: pid=????,exited(" + String.valueOf(status) + ")>");
        }
        
        @JRubyMethod(name = "success?")
        public IRubyObject success_p() {
            return getRuntime().newBoolean(status == EXIT_SUCCESS);
        }
        
        private long shiftedValue() {
            return status << 8;
        }
    }

    @JRubyModule(name="Process::UID")
    public static class UserID {
        @JRubyMethod(name = "change_privilege", module = true)
        public static IRubyObject change_privilege(IRubyObject self, IRubyObject arg) {
            throw self.getRuntime().newNotImplementedError("Process::UID::change_privilege not implemented yet");
        }
        
        @JRubyMethod(name = "eid", module = true)
        public static IRubyObject eid(IRubyObject self) {
            return euid(self);
        }
        
        @JRubyMethod(name = "eid=", module = true)
        public static IRubyObject eid(IRubyObject self, IRubyObject arg) {
            return euid_set(self, arg);
        }
        
        @JRubyMethod(name = "grant_privilege", module = true)
        public static IRubyObject grant_privilege(IRubyObject self, IRubyObject arg) {
            throw self.getRuntime().newNotImplementedError("Process::UID::grant_privilege not implemented yet");
        }
        
        @JRubyMethod(name = "re_exchange", module = true)
        public static IRubyObject re_exchange(ThreadContext context, IRubyObject self) {
            return switch_rb(context, self, Block.NULL_BLOCK);
        }
        
        @JRubyMethod(name = "re_exchangeable?", module = true)
        public static IRubyObject re_exchangeable_p(IRubyObject self) {
            throw self.getRuntime().newNotImplementedError("Process::UID::re_exchangeable? not implemented yet");
        }
        
        @JRubyMethod(name = "rid", module = true)
        public static IRubyObject rid(IRubyObject self) {
            return uid(self);
        }
        
        @JRubyMethod(name = "sid_available?", module = true)
        public static IRubyObject sid_available_p(IRubyObject self) {
            throw self.getRuntime().newNotImplementedError("Process::UID::sid_available not implemented yet");
        }
        
        @JRubyMethod(name = "switch", module = true, visibility = Visibility.PRIVATE)
        public static IRubyObject switch_rb(ThreadContext context, IRubyObject self, Block block) {
            Ruby runtime = self.getRuntime();
            int uid = runtime.getPosix().getuid();
            int euid = runtime.getPosix().geteuid();
            
            if (block.isGiven()) {
                try {
                    runtime.getPosix().seteuid(uid);
                    runtime.getPosix().setuid(euid);
                    
                    return block.yield(context, runtime.getNil());
                } finally {
                    runtime.getPosix().seteuid(euid);
                    runtime.getPosix().setuid(uid);
                }
            } else {
                runtime.getPosix().seteuid(uid);
                runtime.getPosix().setuid(euid);
                
                return RubyFixnum.zero(runtime);
            }
        }
    }
    
    @JRubyModule(name="Process::GID")
    public static class GroupID {
        @JRubyMethod(name = "change_privilege", module = true)
        public static IRubyObject change_privilege(IRubyObject self, IRubyObject arg) {
            throw self.getRuntime().newNotImplementedError("Process::GID::change_privilege not implemented yet");
        }
        
        @JRubyMethod(name = "eid", module = true)
        public static IRubyObject eid(IRubyObject self) {
            return egid(self);
        }
        
        @JRubyMethod(name = "eid=", module = true)
        public static IRubyObject eid(IRubyObject self, IRubyObject arg) {
            return RubyProcess.egid_set(self, arg);
        }
        
        @JRubyMethod(name = "grant_privilege", module = true)
        public static IRubyObject grant_privilege(IRubyObject self, IRubyObject arg) {
            throw self.getRuntime().newNotImplementedError("Process::GID::grant_privilege not implemented yet");
        }
        
        @JRubyMethod(name = "re_exchange", module = true)
        public static IRubyObject re_exchange(ThreadContext context, IRubyObject self) {
            return switch_rb(context, self, Block.NULL_BLOCK);
        }
        
        @JRubyMethod(name = "re_exchangeable?", module = true)
        public static IRubyObject re_exchangeable_p(IRubyObject self) {
            throw self.getRuntime().newNotImplementedError("Process::GID::re_exchangeable? not implemented yet");
        }
        
        @JRubyMethod(name = "rid", module = true)
        public static IRubyObject rid(IRubyObject self) {
            return gid(self);
        }
        
        @JRubyMethod(name = "sid_available?", module = true)
        public static IRubyObject sid_available_p(IRubyObject self) {
            throw self.getRuntime().newNotImplementedError("Process::GID::sid_available not implemented yet");
        }
        
        @JRubyMethod(name = "switch", module = true, visibility = Visibility.PRIVATE)
        public static IRubyObject switch_rb(ThreadContext context, IRubyObject self, Block block) {
            Ruby runtime = self.getRuntime();
            int gid = runtime.getPosix().getgid();
            int egid = runtime.getPosix().getegid();
            
            if (block.isGiven()) {
                try {
                    runtime.getPosix().setegid(gid);
                    runtime.getPosix().setgid(egid);
                    
                    return block.yield(context, runtime.getNil());
                } finally {
                    runtime.getPosix().setegid(egid);
                    runtime.getPosix().setgid(gid);
                }
            } else {
                runtime.getPosix().setegid(gid);
                runtime.getPosix().setgid(egid);
                
                return RubyFixnum.zero(runtime);
            }
        }
    }
    
    @JRubyModule(name="Process::Sys")
    public static class Sys {
        @JRubyMethod(name = "getegid", module = true, visibility = Visibility.PRIVATE)
        public static IRubyObject getegid(IRubyObject self) {
            return egid(self);
        }
        
        @JRubyMethod(name = "geteuid", module = true, visibility = Visibility.PRIVATE)
        public static IRubyObject geteuid(IRubyObject self) {
            return euid(self);
        }
        
        @JRubyMethod(name = "getgid", module = true, visibility = Visibility.PRIVATE)
        public static IRubyObject getgid(IRubyObject self) {
            return gid(self);
        }
        
        @JRubyMethod(name = "getuid", module = true, visibility = Visibility.PRIVATE)
        public static IRubyObject getuid(IRubyObject self) {
            return uid(self);
        }

        @JRubyMethod(name = "setegid", module = true, visibility = Visibility.PRIVATE)
        public static IRubyObject setegid(IRubyObject recv, IRubyObject arg) {
            return egid_set(recv, arg);
        }

        @JRubyMethod(name = "seteuid", module = true, visibility = Visibility.PRIVATE)
        public static IRubyObject seteuid(IRubyObject recv, IRubyObject arg) {
            return euid_set(recv, arg);
        }

        @JRubyMethod(name = "setgid", module = true, visibility = Visibility.PRIVATE)
        public static IRubyObject setgid(IRubyObject recv, IRubyObject arg) {
            return gid_set(recv, arg);
        }

        @JRubyMethod(name = "setuid", module = true, visibility = Visibility.PRIVATE)
        public static IRubyObject setuid(IRubyObject recv, IRubyObject arg) {
            return uid_set(recv, arg);
        }
    }

    @JRubyMethod(name = "abort", optional = 1, module = true, visibility = Visibility.PRIVATE)
    public static IRubyObject abort(ThreadContext context, IRubyObject recv, IRubyObject[] args) {
        return RubyKernel.abort(context, recv, args);
    }

    @JRubyMethod(name = "exit!", optional = 1, module = true, visibility = Visibility.PRIVATE)
    public static IRubyObject exit_bang(IRubyObject recv, IRubyObject[] args) {
        return RubyKernel.exit_bang(recv, args);
    }

    @JRubyMethod(name = "groups", module = true, visibility = Visibility.PRIVATE)
    public static IRubyObject groups(IRubyObject recv) {
        throw recv.getRuntime().newNotImplementedError("Process#groups not yet implemented");
    }

    @JRubyMethod(name = "setrlimit", rest = true, module = true, visibility = Visibility.PRIVATE)
    public static IRubyObject setrlimit(IRubyObject recv, IRubyObject[] args) {
        throw recv.getRuntime().newNotImplementedError("Process#setrlimit not yet implemented");
    }

    @JRubyMethod(name = "getpgrp", module = true, visibility = Visibility.PRIVATE)
    public static IRubyObject getpgrp(IRubyObject recv) {
        return recv.getRuntime().newFixnum(recv.getRuntime().getPosix().getpgrp());
    }

    @JRubyMethod(name = "groups=", required = 1, module = true, visibility = Visibility.PRIVATE)
    public static IRubyObject groups_set(IRubyObject recv, IRubyObject arg) {
        throw recv.getRuntime().newNotImplementedError("Process#groups not yet implemented");
    }

    @JRubyMethod(name = "waitpid", rest = true, module = true, visibility = Visibility.PRIVATE)
    public static IRubyObject waitpid(IRubyObject recv, IRubyObject[] args) {
        Ruby runtime = recv.getRuntime();
        int pid = -1;
        int flags = 0;
        if (args.length > 0) {
            pid = (int)args[0].convertToInteger().getLongValue();
        }
        if (args.length > 1) {
            flags = (int)args[1].convertToInteger().getLongValue();
        }
        
        int[] status = new int[1];
        pid = runtime.getPosix().waitpid(pid, status, flags);
        
        if (pid == -1) {
            throw runtime.newErrnoECHILDError();
        }
        
        runtime.getGlobalVariables().set(
                "$?", 
                RubyProcess.RubyStatus.newProcessStatus(runtime, status[0]));
        return runtime.newFixnum(pid);
    }

    @JRubyMethod(name = "wait", rest = true, module = true, visibility = Visibility.PRIVATE)
    public static IRubyObject wait(IRubyObject recv, IRubyObject[] args) {
        Ruby runtime = recv.getRuntime();
        
        if (args.length > 0) {
            return waitpid(recv, args);
        }
        
        int[] status = new int[1];
        int pid = runtime.getPosix().wait(status);
        
        if (pid == -1) {
            throw runtime.newErrnoECHILDError();
        }
        
        runtime.getGlobalVariables().set(
                "$?", 
                RubyProcess.RubyStatus.newProcessStatus(runtime, status[0]));
        return runtime.newFixnum(pid);
    }

    @JRubyMethod(name = "waitall", module = true, visibility = Visibility.PRIVATE)
    public static IRubyObject waitall(IRubyObject recv) {
        Ruby runtime = recv.getRuntime();
        POSIX posix = runtime.getPosix();
        RubyArray results = recv.getRuntime().newArray();
        
        int[] status = new int[1];
        int result = posix.wait(status);
        while (result != -1) {
            results.append(runtime.newArray(runtime.newFixnum(result), RubyProcess.RubyStatus.newProcessStatus(runtime, status[0])));
            result = posix.wait(status);
        }
        
        return results;
    }

    @JRubyMethod(name = "setsid", module = true, visibility = Visibility.PRIVATE)
    public static IRubyObject setsid(IRubyObject recv) {
        return recv.getRuntime().newFixnum(recv.getRuntime().getPosix().setsid());
    }

    @JRubyMethod(name = "setpgrp", module = true, visibility = Visibility.PRIVATE)
    public static IRubyObject setpgrp(IRubyObject recv) {
        return recv.getRuntime().newFixnum(recv.getRuntime().getPosix().setpgid(0, 0));
    }

    @JRubyMethod(name = "egid=", required = 1, module = true, visibility = Visibility.PRIVATE)
    public static IRubyObject egid_set(IRubyObject recv, IRubyObject arg) {
        recv.getRuntime().getPosix().setegid((int)arg.convertToInteger().getLongValue());
        return RubyFixnum.zero(recv.getRuntime());
    }

    @JRubyMethod(name = "euid", module = true, visibility = Visibility.PRIVATE)
    public static IRubyObject euid(IRubyObject recv) {
        return recv.getRuntime().newFixnum(recv.getRuntime().getPosix().geteuid());
    }

    @JRubyMethod(name = "uid=", required = 1, module = true, visibility = Visibility.PRIVATE)
    public static IRubyObject uid_set(IRubyObject recv, IRubyObject arg) {
        recv.getRuntime().getPosix().setuid((int)arg.convertToInteger().getLongValue());
        return RubyFixnum.zero(recv.getRuntime());
    }

    @JRubyMethod(name = "gid", module = true, visibility = Visibility.PRIVATE)
    public static IRubyObject gid(IRubyObject recv) {
        return recv.getRuntime().newFixnum(recv.getRuntime().getPosix().getgid());
    }

    @JRubyMethod(name = "maxgroups", module = true, visibility = Visibility.PRIVATE)
    public static IRubyObject maxgroups(IRubyObject recv) {
        throw recv.getRuntime().newNotImplementedError("Process#maxgroups not yet implemented");
    }

    @JRubyMethod(name = "getpriority", required = 2, module = true, visibility = Visibility.PRIVATE)
    public static IRubyObject getpriority(IRubyObject recv, IRubyObject arg1, IRubyObject arg2) {
        int which = (int)arg1.convertToInteger().getLongValue();
        int who = (int)arg2.convertToInteger().getLongValue();
        int result = recv.getRuntime().getPosix().getpriority(which, who);
        
        return recv.getRuntime().newFixnum(result);
    }

    @JRubyMethod(name = "uid", module = true, visibility = Visibility.PRIVATE)
    public static IRubyObject uid(IRubyObject recv) {
        return recv.getRuntime().newFixnum(recv.getRuntime().getPosix().getuid());
    }

    @JRubyMethod(name = "waitpid2", rest = true, module = true, visibility = Visibility.PRIVATE)
    public static IRubyObject waitpid2(IRubyObject recv, IRubyObject[] args) {
        Ruby runtime = recv.getRuntime();
        int pid = -1;
        int flags = 0;
        if (args.length > 0) {
            pid = (int)args[0].convertToInteger().getLongValue();
        }
        if (args.length > 1) {
            flags = (int)args[1].convertToInteger().getLongValue();
        }
        
        int[] status = new int[1];
        pid = runtime.getPosix().waitpid(pid, status, flags);
        
        if (pid == -1) {
            throw runtime.newErrnoECHILDError();
        }
        
        return runtime.newArray(runtime.newFixnum(pid), RubyProcess.RubyStatus.newProcessStatus(runtime, status[0]));
    }

    @JRubyMethod(name = "initgroups", required = 2, module = true, visibility = Visibility.PRIVATE)
    public static IRubyObject initgroups(IRubyObject recv, IRubyObject arg1, IRubyObject arg2) {
        throw recv.getRuntime().newNotImplementedError("Process#initgroups not yet implemented");
    }

    @JRubyMethod(name = "maxgroups=", required = 1, module = true, visibility = Visibility.PRIVATE)
    public static IRubyObject maxgroups_set(IRubyObject recv, IRubyObject arg) {
        throw recv.getRuntime().newNotImplementedError("Process#maxgroups_set not yet implemented");
    }

    @JRubyMethod(name = "ppid", module = true, visibility = Visibility.PRIVATE)
    public static IRubyObject ppid(IRubyObject recv) {
        return recv.getRuntime().newFixnum(recv.getRuntime().getPosix().getppid());
    }

    @JRubyMethod(name = "gid=", required = 1, module = true, visibility = Visibility.PRIVATE)
    public static IRubyObject gid_set(IRubyObject recv, IRubyObject arg) {
        return recv.getRuntime().newFixnum(recv.getRuntime().getPosix().setgid((int)arg.convertToInteger().getLongValue()));
    }

    @JRubyMethod(name = "wait2", rest = true, module = true, visibility = Visibility.PRIVATE)
    public static IRubyObject wait2(IRubyObject recv, IRubyObject[] args) {
        return waitpid2(recv, args);
    }

    @JRubyMethod(name = "euid=", required = 1, module = true, visibility = Visibility.PRIVATE)
    public static IRubyObject euid_set(IRubyObject recv, IRubyObject arg) {
        recv.getRuntime().getPosix().seteuid((int)arg.convertToInteger().getLongValue());
        return RubyFixnum.zero(recv.getRuntime());
    }

    @JRubyMethod(name = "setpriority", required = 3, module = true, visibility = Visibility.PRIVATE)
    public static IRubyObject setpriority(IRubyObject recv, IRubyObject arg1, IRubyObject arg2, IRubyObject arg3) {
        int which = (int)arg1.convertToInteger().getLongValue();
        int who = (int)arg2.convertToInteger().getLongValue();
        int prio = (int)arg3.convertToInteger().getLongValue();
        int result = recv.getRuntime().getPosix().setpriority(which, who, prio);
        
        return recv.getRuntime().newFixnum(result);
    }

    @JRubyMethod(name = "setpgid", required = 2, module = true, visibility = Visibility.PRIVATE)
    public static IRubyObject setpgid(IRubyObject recv, IRubyObject arg1, IRubyObject arg2) {
        int pid = (int)arg1.convertToInteger().getLongValue();
        int gid = (int)arg2.convertToInteger().getLongValue();
        return recv.getRuntime().newFixnum(recv.getRuntime().getPosix().setpgid(pid, gid));
    }

    @JRubyMethod(name = "getpgid", required = 1, module = true, visibility = Visibility.PRIVATE)
    public static IRubyObject getpgid(IRubyObject recv, IRubyObject arg) {
        return recv.getRuntime().newFixnum(recv.getRuntime().getPosix().getpgid((int)arg.convertToInteger().getLongValue()));
    }

    @JRubyMethod(name = "getrlimit", required = 1, module = true, visibility = Visibility.PRIVATE)
    public static IRubyObject getrlimit(IRubyObject recv, IRubyObject arg) {
        throw recv.getRuntime().newNotImplementedError("Process#getrlimit not yet implemented");
    }

    @JRubyMethod(name = "egid", module = true, visibility = Visibility.PRIVATE)
    public static IRubyObject egid(IRubyObject recv) {
        return recv.getRuntime().newFixnum(recv.getRuntime().getPosix().getegid());
    }
    
    private static String[] signals = new String[] {"EXIT", "HUP", "INT", "QUIT", "ILL", "TRAP", 
        "ABRT", "POLL", "FPE", "KILL", "BUS", "SEGV", "SYS", "PIPE", "ALRM", "TERM", "URG", "STOP",
        "TSTP", "CONT", "CHLD", "TTIN", "TTOU", "XCPU", "XFSZ", "VTALRM", "PROF", "USR1", "USR2"};
    
    private static int parseSignalString(Ruby runtime, String value) {
        int startIndex = 0;
        boolean negative = value.startsWith("-");
        
        if (negative) startIndex++;
        
        boolean signalString = value.startsWith("SIG", startIndex);
        
        if (signalString) startIndex += 3;
       
        String signalName = value.substring(startIndex);
        
        // FIXME: This table will get moved into POSIX library so we can get all actual supported
        // signals.  This is a quick fix to support basic signals until that happens.
        for (int i = 0; i < signals.length; i++) {
            if (signals[i].equals(signalName)) return negative ? -i : i;
        }
        
        throw runtime.newArgumentError("unsupported name `SIG" + signalName + "'");
    }

    @JRubyMethod(name = "kill", rest = true, module = true, visibility = Visibility.PRIVATE)
    public static IRubyObject kill(IRubyObject recv, IRubyObject[] args) {
        if (args.length < 2) {
            throw recv.getRuntime().newArgumentError("wrong number of arguments -- kill(sig, pid...)");
        }
        
        Ruby runtime = recv.getRuntime();
        int signal;
        if (args[0] instanceof RubyFixnum) {
            signal = (int) ((RubyFixnum) args[0]).getLongValue();
        } else if (args[0] instanceof RubySymbol) {
            signal = parseSignalString(runtime, args[0].toString());
        } else if (args[0] instanceof RubyString) {
            signal = parseSignalString(runtime, args[0].toString());
        } else {
            signal = parseSignalString(runtime, args[0].checkStringType().toString());
        }

        boolean processGroupKill = signal < 0;
        
        if (processGroupKill) signal = -signal;
        
        POSIX posix = runtime.getPosix();
        for (int i = 1; i < args.length; i++) {
            int pid = RubyNumeric.num2int(args[i]);

            // FIXME: It may be possible to killpg on systems which support it.  POSIX library
            // needs to tell whether a particular method works or not
            if (pid == 0) pid = runtime.getPosix().getpid();
            posix.kill(processGroupKill ? -pid : pid, signal);            
        }
        
        return runtime.newFixnum(args.length - 1);

    }

    @JRubyMethod(name = "detach", required = 1, module = true, visibility = Visibility.PRIVATE)
    public static IRubyObject detach(ThreadContext context, IRubyObject recv, IRubyObject arg) {
        final int pid = (int)arg.convertToInteger().getLongValue();
        Ruby runtime = recv.getRuntime();
        
        BlockCallback callback = new BlockCallback() {
            public IRubyObject call(ThreadContext context, IRubyObject[] args, Block block) {
                int[] status = new int[1];
                int result = context.getRuntime().getPosix().waitpid(pid, status, 0);
                
                return context.getRuntime().newFixnum(result);
            }
        };
        
        return RubyThread.newInstance(
                runtime.getThread(),
                IRubyObject.NULL_ARRAY,
                CallBlock.newCallClosure(recv, (RubyModule)recv, Arity.NO_ARGUMENTS, callback, context));
    }
    
    @JRubyMethod(name = "times", frame = true, module = true, visibility = Visibility.PRIVATE)
    public static IRubyObject times(IRubyObject recv, Block unusedBlock) {
        Ruby runtime = recv.getRuntime();
        double currentTime = System.currentTimeMillis() / 1000.0;
        double startTime = runtime.getStartTime() / 1000.0;
        RubyFloat zero = runtime.newFloat(0.0);
        return RubyStruct.newStruct(runtime.getTmsStruct(), 
                new IRubyObject[] { runtime.newFloat(currentTime - startTime), zero, zero, zero }, 
                Block.NULL_BLOCK);
    }

    @JRubyMethod(name = "pid", module = true, visibility = Visibility.PRIVATE)
    public static IRubyObject pid(IRubyObject recv) {
        return recv.getRuntime().newFixnum(recv.getRuntime().getPosix().getpid());
    }
    
    @JRubyMethod(name = "fork", module = true, visibility = Visibility.PRIVATE)
    public static IRubyObject fork(ThreadContext context, IRubyObject recv, Block block) {
        return RubyKernel.fork(context, recv, block);
    }
    
    @JRubyMethod(name = "exit", optional = 1, module = true, visibility = Visibility.PRIVATE)
    public static IRubyObject exit(IRubyObject recv, IRubyObject[] args) {
        return RubyKernel.exit(recv, args);
    }
}
