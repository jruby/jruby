/*
 **** BEGIN LICENSE BLOCK *****
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
 * Copyright (C) 2004 Thomas E Enebo <enebo@acm.org>
 * Copyright (C) 2004 Jan Arne Petersen <jpetersen@uni-bonn.de>
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
import jnr.constants.platform.Sysconf;
import jnr.ffi.byref.IntByReference;
import jnr.posix.Times;
import org.jruby.anno.JRubyClass;
import org.jruby.anno.JRubyMethod;
import org.jruby.anno.JRubyModule;
import jnr.posix.POSIX;
import org.jruby.platform.Platform;
import org.jruby.runtime.Arity;
import org.jruby.runtime.Block;
import org.jruby.runtime.BlockCallback;
import org.jruby.runtime.CallBlock;
import org.jruby.runtime.ObjectAllocator;
import org.jruby.runtime.ThreadContext;
import static org.jruby.runtime.Visibility.*;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.ShellLauncher;
import static org.jruby.CompatVersion.*;

import static org.jruby.runtime.Helpers.invokedynamic;
import static org.jruby.runtime.invokedynamic.MethodNames.OP_EQUAL;
import static org.jruby.util.WindowsFFI.kernel32;
import static org.jruby.util.WindowsFFI.Kernel32.*;

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
        
        process.defineAnnotatedMethods(RubyProcess.class);
        process_status.defineAnnotatedMethods(RubyStatus.class);
        process_uid.defineAnnotatedMethods(UserID.class);
        process_gid.defineAnnotatedMethods(GroupID.class);
        process_sys.defineAnnotatedMethods(Sys.class);

        runtime.loadConstantSet(process, jnr.constants.platform.PRIO.class);
        runtime.loadConstantSet(process, jnr.constants.platform.RLIM.class);
        runtime.loadConstantSet(process, jnr.constants.platform.RLIMIT.class);
        
        process.defineConstant("WNOHANG", runtime.newFixnum(1));
        process.defineConstant("WUNTRACED", runtime.newFixnum(2));
        
        return process;
    }
    
    @JRubyClass(name="Process::Status")
    public static class RubyStatus extends RubyObject {
        private final long status;
        private final long pid;
        
        private static final long EXIT_SUCCESS = 0L;
        public RubyStatus(Ruby runtime, RubyClass metaClass, long status, long pid) {
            super(runtime, metaClass);
            this.status = status;
            this.pid = pid;
        }
        
        public static RubyStatus newProcessStatus(Ruby runtime, long status, long pid) {
            return new RubyStatus(runtime, runtime.getProcStatus(), status, pid);
        }
        
        // Bunch of methods still not implemented
        @JRubyMethod(name = "stopped?")
        public IRubyObject not_implemented0() {
            String error = "Process::Status#stopped? not implemented";
            throw getRuntime().newNotImplementedError(error);
        }
        
        @JRubyMethod(name = {"&"})
        public IRubyObject not_implemented1(IRubyObject arg) {
            String error = "Process::Status#& not implemented";
            throw getRuntime().newNotImplementedError(error);
        }
        
        @JRubyMethod
        public IRubyObject exitstatus() {
            return getRuntime().newFixnum(status);
        }

        @JRubyMethod(name = {"exited?"})
        public IRubyObject exited() {
            return RubyBoolean.newBoolean(getRuntime(), (status == 0));
        }

        @JRubyMethod(name = {"signaled?"})
        public IRubyObject signaled() {
            return RubyBoolean.newBoolean(getRuntime(), (status > 0));
        }

        @JRubyMethod(name = {"termsig"})
        public IRubyObject termsig() {
            return RubyFixnum.newFixnum(getRuntime(), status & 0x7f);
        }

        @JRubyMethod(name = {"stopsig"})
        public IRubyObject stopsig() {
            return RubyFixnum.newFixnum(getRuntime(), status);
        }

        @Deprecated
        public IRubyObject op_rshift(IRubyObject other) {
            return op_rshift(getRuntime(), other);
        }
        @JRubyMethod(name = ">>")
        public IRubyObject op_rshift(ThreadContext context, IRubyObject other) {
            return op_rshift(context.runtime, other);
        }
        public IRubyObject op_rshift(Ruby runtime, IRubyObject other) {
            long shiftValue = other.convertToInteger().getLongValue();
            return runtime.newFixnum(status >> shiftValue);
        }

        @Override
        @JRubyMethod(name = "==")
        public IRubyObject op_equal(ThreadContext context, IRubyObject other) {
            return invokedynamic(context, other, OP_EQUAL, this.to_i(context.runtime));
        }
        
        @Deprecated
        public IRubyObject to_i() {
            return to_i(getRuntime());
        }
        @JRubyMethod(name = {"to_i", "to_int"})
        public IRubyObject to_i(ThreadContext context) {
            return to_i(context.runtime);
        }
        public IRubyObject to_i(Ruby runtime) {
            return runtime.newFixnum(shiftedValue());
        }
        
        @Override
        public IRubyObject to_s() {
            return to_s(getRuntime());
        }
        @JRubyMethod
        public IRubyObject to_s(ThreadContext context) {
            return to_s(context.runtime);
        }
        public IRubyObject to_s(Ruby runtime) {
            return runtime.newString(String.valueOf(shiftedValue()));
        }
        
        @Override
        public IRubyObject inspect() {
            return inspect(getRuntime());
        }
        @JRubyMethod
        public IRubyObject inspect(ThreadContext context) {
            return inspect(context.runtime);
        }
        public IRubyObject inspect(Ruby runtime) {
            return runtime.newString("#<Process::Status: pid=" + pid + ",exited(" + String.valueOf(status) + ")>");
        }
        
        @JRubyMethod(name = "success?")
        public IRubyObject success_p(ThreadContext context) {
            return context.runtime.newBoolean(status == EXIT_SUCCESS);
        }

        @JRubyMethod(name = "coredump?")
        public IRubyObject coredump_p(ThreadContext context) {
            // Temporarily return false until a backend can be implemented
            // to copy the CRuby behavior (checking WCOREDUMP)
            return context.runtime.getFalse();
        }
        
        @JRubyMethod
        public IRubyObject pid(ThreadContext context) {
            return context.runtime.newFixnum(pid);
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
        
        @Deprecated
        public static IRubyObject eid(IRubyObject self) {
            return euid(self.getRuntime());
        }
        @JRubyMethod(name = "eid", module = true)
        public static IRubyObject eid(ThreadContext context, IRubyObject self) {
            return euid(context.runtime);
        }

        @Deprecated
        public static IRubyObject eid(IRubyObject self, IRubyObject arg) {
            return eid(self.getRuntime(), arg);
        }
        @JRubyMethod(name = "eid=", module = true)
        public static IRubyObject eid(ThreadContext context, IRubyObject self, IRubyObject arg) {
            return eid(context.runtime, arg);
        }
        public static IRubyObject eid(Ruby runtime, IRubyObject arg) {
            return euid_set(runtime, arg);
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

        @Deprecated
        public static IRubyObject rid(IRubyObject self) {
            return rid(self.getRuntime());
        }
        @JRubyMethod(name = "rid", module = true)
        public static IRubyObject rid(ThreadContext context, IRubyObject self) {
            return rid(context.runtime);
        }
        public static IRubyObject rid(Ruby runtime) {
            return uid(runtime);
        }
        
        @JRubyMethod(name = "sid_available?", module = true)
        public static IRubyObject sid_available_p(IRubyObject self) {
            throw self.getRuntime().newNotImplementedError("Process::UID::sid_available not implemented yet");
        }
        
        @JRubyMethod(name = "switch", module = true, visibility = PRIVATE)
        public static IRubyObject switch_rb(ThreadContext context, IRubyObject self, Block block) {
            Ruby runtime = context.runtime;
            int uid = checkErrno(runtime, runtime.getPosix().getuid());
            int euid = checkErrno(runtime, runtime.getPosix().geteuid());
            
            if (block.isGiven()) {
                try {
                    checkErrno(runtime, runtime.getPosix().seteuid(uid));
                    checkErrno(runtime, runtime.getPosix().setuid(euid));
                    
                    return block.yield(context, runtime.getNil());
                } finally {
                    checkErrno(runtime, runtime.getPosix().seteuid(euid));
                    checkErrno(runtime, runtime.getPosix().setuid(uid));
                }
            } else {
                checkErrno(runtime, runtime.getPosix().seteuid(uid));
                checkErrno(runtime, runtime.getPosix().setuid(euid));
                
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

        @Deprecated
        public static IRubyObject eid(IRubyObject self) {
            return eid(self.getRuntime());
        }
        @JRubyMethod(name = "eid", module = true)
        public static IRubyObject eid(ThreadContext context, IRubyObject self) {
            return eid(context.runtime);
        }
        public static IRubyObject eid(Ruby runtime) {
            return egid(runtime);
        }

        @Deprecated
        public static IRubyObject eid(IRubyObject self, IRubyObject arg) {
            return eid(self.getRuntime(), arg);
        }
        @JRubyMethod(name = "eid=", module = true)
        public static IRubyObject eid(ThreadContext context, IRubyObject self, IRubyObject arg) {
            return eid(context.runtime, arg);
        }
        public static IRubyObject eid(Ruby runtime, IRubyObject arg) {
            return RubyProcess.egid_set(runtime, arg);
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

        @Deprecated
        public static IRubyObject rid(IRubyObject self) {
            return rid(self.getRuntime());
        }
        @JRubyMethod(name = "rid", module = true)
        public static IRubyObject rid(ThreadContext context, IRubyObject self) {
            return rid(context.runtime);
        }
        public static IRubyObject rid(Ruby runtime) {
            return gid(runtime);
        }
        
        @JRubyMethod(name = "sid_available?", module = true)
        public static IRubyObject sid_available_p(IRubyObject self) {
            throw self.getRuntime().newNotImplementedError("Process::GID::sid_available not implemented yet");
        }
        
        @JRubyMethod(name = "switch", module = true, visibility = PRIVATE)
        public static IRubyObject switch_rb(ThreadContext context, IRubyObject self, Block block) {
            Ruby runtime = context.runtime;
            int gid = checkErrno(runtime, runtime.getPosix().getgid());
            int egid = checkErrno(runtime, runtime.getPosix().getegid());
            
            if (block.isGiven()) {
                try {
                    checkErrno(runtime, runtime.getPosix().setegid(gid));
                    checkErrno(runtime, runtime.getPosix().setgid(egid));
                    
                    return block.yield(context, runtime.getNil());
                } finally {
                    checkErrno(runtime, runtime.getPosix().setegid(egid));
                    checkErrno(runtime, runtime.getPosix().setgid(gid));
                }
            } else {
                checkErrno(runtime, runtime.getPosix().setegid(gid));
                checkErrno(runtime, runtime.getPosix().setgid(egid));
                
                return RubyFixnum.zero(runtime);
            }
        }
    }
    
    @JRubyModule(name="Process::Sys")
    public static class Sys {
        @Deprecated
        public static IRubyObject getegid(IRubyObject self) {
            return egid(self.getRuntime());
        }
        @JRubyMethod(name = "getegid", module = true, visibility = PRIVATE)
        public static IRubyObject getegid(ThreadContext context, IRubyObject self) {
            return egid(context.runtime);
        }
        
        @Deprecated
        public static IRubyObject geteuid(IRubyObject self) {
            return euid(self.getRuntime());
        }
        @JRubyMethod(name = "geteuid", module = true, visibility = PRIVATE)
        public static IRubyObject geteuid(ThreadContext context, IRubyObject self) {
            return euid(context.runtime);
        }

        @Deprecated
        public static IRubyObject getgid(IRubyObject self) {
            return gid(self.getRuntime());
        }
        @JRubyMethod(name = "getgid", module = true, visibility = PRIVATE)
        public static IRubyObject getgid(ThreadContext context, IRubyObject self) {
            return gid(context.runtime);
        }

        @Deprecated
        public static IRubyObject getuid(IRubyObject self) {
            return uid(self.getRuntime());
        }
        @JRubyMethod(name = "getuid", module = true, visibility = PRIVATE)
        public static IRubyObject getuid(ThreadContext context, IRubyObject self) {
            return uid(context.runtime);
        }

        @Deprecated
        public static IRubyObject setegid(IRubyObject recv, IRubyObject arg) {
            return egid_set(recv.getRuntime(), arg);
        }
        @JRubyMethod(name = "setegid", module = true, visibility = PRIVATE)
        public static IRubyObject setegid(ThreadContext context, IRubyObject recv, IRubyObject arg) {
            return egid_set(context.runtime, arg);
        }

        @Deprecated
        public static IRubyObject seteuid(IRubyObject recv, IRubyObject arg) {
            return euid_set(recv.getRuntime(), arg);
        }
        @JRubyMethod(name = "seteuid", module = true, visibility = PRIVATE)
        public static IRubyObject seteuid(ThreadContext context, IRubyObject recv, IRubyObject arg) {
            return euid_set(context.runtime, arg);
        }

        @Deprecated
        public static IRubyObject setgid(IRubyObject recv, IRubyObject arg) {
            return gid_set(recv.getRuntime(), arg);
        }
        @JRubyMethod(name = "setgid", module = true, visibility = PRIVATE)
        public static IRubyObject setgid(ThreadContext context, IRubyObject recv, IRubyObject arg) {
            return gid_set(context.runtime, arg);
        }

        @Deprecated
        public static IRubyObject setuid(IRubyObject recv, IRubyObject arg) {
            return uid_set(recv.getRuntime(), arg);
        }
        @JRubyMethod(name = "setuid", module = true, visibility = PRIVATE)
        public static IRubyObject setuid(ThreadContext context, IRubyObject recv, IRubyObject arg) {
            return uid_set(context.runtime, arg);
        }
    }

    @JRubyMethod(name = "abort", optional = 1, module = true, visibility = PRIVATE)
    public static IRubyObject abort(ThreadContext context, IRubyObject recv, IRubyObject[] args) {
        return RubyKernel.abort(context, recv, args);
    }

    @JRubyMethod(name = "exit!", optional = 1, module = true, visibility = PRIVATE)
    public static IRubyObject exit_bang(IRubyObject recv, IRubyObject[] args) {
        return RubyKernel.exit_bang(recv, args);
    }

    @JRubyMethod(name = "groups", module = true, visibility = PRIVATE)
    public static IRubyObject groups(IRubyObject recv) {
        throw recv.getRuntime().newNotImplementedError("Process#groups not yet implemented");
    }

    @JRubyMethod(name = "setrlimit", rest = true, module = true, visibility = PRIVATE)
    public static IRubyObject setrlimit(IRubyObject recv, IRubyObject[] args) {
        throw recv.getRuntime().newNotImplementedError("Process#setrlimit not yet implemented");
    }

    @Deprecated
    public static IRubyObject getpgrp(IRubyObject recv) {
        return getpgrp(recv.getRuntime());
    }
    @JRubyMethod(name = "getpgrp", module = true, visibility = PRIVATE)
    public static IRubyObject getpgrp(ThreadContext context, IRubyObject recv) {
        return getpgrp(context.runtime);
    }
    public static IRubyObject getpgrp(Ruby runtime) {
        return runtime.newFixnum(runtime.getPosix().getpgrp());
    }

    @JRubyMethod(name = "groups=", required = 1, module = true, visibility = PRIVATE)
    public static IRubyObject groups_set(IRubyObject recv, IRubyObject arg) {
        throw recv.getRuntime().newNotImplementedError("Process#groups not yet implemented");
    }

    @Deprecated
    public static IRubyObject waitpid(IRubyObject recv, IRubyObject[] args) {
        return waitpid(recv.getRuntime(), args);
    }
    @JRubyMethod(name = "waitpid", rest = true, module = true, visibility = PRIVATE)
    public static IRubyObject waitpid(ThreadContext context, IRubyObject recv, IRubyObject[] args) {
        return waitpid(context.runtime, args);
    }
    public static IRubyObject waitpid(Ruby runtime, IRubyObject[] args) {
        int pid = -1;
        int flags = 0;
        if (args.length > 0) {
            pid = (int)args[0].convertToInteger().getLongValue();
        }
        if (args.length > 1) {
            flags = (int)args[1].convertToInteger().getLongValue();
        }
        
        int[] status = new int[1];
        runtime.getPosix().errno(0);
        pid = runtime.getPosix().waitpid(pid, status, flags);
        raiseErrnoIfSet(runtime, ECHILD);
        
        runtime.getCurrentContext().setLastExitStatus(RubyProcess.RubyStatus.newProcessStatus(runtime, (status[0] >> 8) & 0xff, pid));
        return runtime.newFixnum(pid);
    }

    private interface NonNativeErrno {
        public int handle(Ruby runtime, int result);
    }

    private static final NonNativeErrno ECHILD = new NonNativeErrno() {
        public int handle(Ruby runtime, int result) {
            throw runtime.newErrnoECHILDError();
        }
    };

    @Deprecated
    public static IRubyObject wait(IRubyObject recv, IRubyObject[] args) {
        return wait(recv.getRuntime(), args);
    }
    @JRubyMethod(name = "wait", rest = true, module = true, visibility = PRIVATE)
    public static IRubyObject wait(ThreadContext context, IRubyObject recv, IRubyObject[] args) {
        return wait(context.runtime, args);
    }
    public static IRubyObject wait(Ruby runtime, IRubyObject[] args) {
        
        if (args.length > 0) {
            return waitpid(runtime, args);
        }
        
        int[] status = new int[1];
        runtime.getPosix().errno(0);
        int pid = runtime.getPosix().wait(status);
        raiseErrnoIfSet(runtime, ECHILD);
        
        runtime.getCurrentContext().setLastExitStatus(RubyProcess.RubyStatus.newProcessStatus(runtime, (status[0] >> 8) & 0xff, pid));
        return runtime.newFixnum(pid);
    }


    @Deprecated
    public static IRubyObject waitall(IRubyObject recv) {
        return waitall(recv.getRuntime());
    }
    @JRubyMethod(name = "waitall", module = true, visibility = PRIVATE)
    public static IRubyObject waitall(ThreadContext context, IRubyObject recv) {
        return waitall(context.runtime);
    }
    public static IRubyObject waitall(Ruby runtime) {
        POSIX posix = runtime.getPosix();
        RubyArray results = runtime.newArray();
        
        int[] status = new int[1];
        int result = posix.wait(status);
        while (result != -1) {
            results.append(runtime.newArray(runtime.newFixnum(result), RubyProcess.RubyStatus.newProcessStatus(runtime, (status[0] >> 8) & 0xff, result)));
            result = posix.wait(status);
        }
        
        return results;
    }

    @Deprecated
    public static IRubyObject setsid(IRubyObject recv) {
        return setsid(recv.getRuntime());
    }
    @JRubyMethod(name = "setsid", module = true, visibility = PRIVATE)
    public static IRubyObject setsid(ThreadContext context, IRubyObject recv) {
        return setsid(context.runtime);
    }
    public static IRubyObject setsid(Ruby runtime) {
        return runtime.newFixnum(checkErrno(runtime, runtime.getPosix().setsid()));
    }

    @Deprecated
    public static IRubyObject setpgrp(IRubyObject recv) {
        return setpgrp(recv.getRuntime());
    }
    @JRubyMethod(name = "setpgrp", module = true, visibility = PRIVATE)
    public static IRubyObject setpgrp(ThreadContext context, IRubyObject recv) {
        return setpgrp(context.runtime);
    }
    public static IRubyObject setpgrp(Ruby runtime) {
        return runtime.newFixnum(checkErrno(runtime, runtime.getPosix().setpgid(0, 0)));
    }

    @Deprecated
    public static IRubyObject egid_set(IRubyObject recv, IRubyObject arg) {
        return egid_set(recv.getRuntime(), arg);
    }
    @JRubyMethod(name = "egid=", required = 1, module = true, visibility = PRIVATE)
    public static IRubyObject egid_set(ThreadContext context, IRubyObject recv, IRubyObject arg) {
        return egid_set(context.runtime, arg);
    }
    public static IRubyObject egid_set(Ruby runtime, IRubyObject arg) {
        checkErrno(runtime, runtime.getPosix().setegid((int)arg.convertToInteger().getLongValue()));
        return RubyFixnum.zero(runtime);
    }

    @Deprecated
    public static IRubyObject euid(IRubyObject recv) {
        return euid(recv.getRuntime());
    }
    @JRubyMethod(name = "euid", module = true, visibility = PRIVATE)
    public static IRubyObject euid(ThreadContext context, IRubyObject recv) {
        return euid(context.runtime);
    }
    public static IRubyObject euid(Ruby runtime) {
        return runtime.newFixnum(checkErrno(runtime, runtime.getPosix().geteuid()));
    }

    @Deprecated
    public static IRubyObject uid_set(IRubyObject recv, IRubyObject arg) {
        return uid_set(recv.getRuntime(), arg);
    }
    @JRubyMethod(name = "uid=", required = 1, module = true, visibility = PRIVATE)
    public static IRubyObject uid_set(ThreadContext context, IRubyObject recv, IRubyObject arg) {
        return uid_set(context.runtime, arg);
    }
    public static IRubyObject uid_set(Ruby runtime, IRubyObject arg) {
        checkErrno(runtime, runtime.getPosix().setuid((int)arg.convertToInteger().getLongValue()));
        return RubyFixnum.zero(runtime);
    }

    @Deprecated
    public static IRubyObject gid(IRubyObject recv) {
        return gid(recv.getRuntime());
    }
    @JRubyMethod(name = "gid", module = true, visibility = PRIVATE)
    public static IRubyObject gid(ThreadContext context, IRubyObject recv) {
        return gid(context.runtime);
    }
    public static IRubyObject gid(Ruby runtime) {
        if (Platform.IS_WINDOWS) {
            // MRI behavior on Windows
            return RubyFixnum.zero(runtime);
        }
        return runtime.newFixnum(checkErrno(runtime, runtime.getPosix().getgid()));
    }

    @JRubyMethod(name = "maxgroups", module = true, visibility = PRIVATE)
    public static IRubyObject maxgroups(IRubyObject recv) {
        throw recv.getRuntime().newNotImplementedError("Process#maxgroups not yet implemented");
    }

    @Deprecated
    public static IRubyObject getpriority(IRubyObject recv, IRubyObject arg1, IRubyObject arg2) {
        return getpriority(recv.getRuntime(), arg1, arg2);
    }
    @JRubyMethod(name = "getpriority", required = 2, module = true, visibility = PRIVATE)
    public static IRubyObject getpriority(ThreadContext context, IRubyObject recv, IRubyObject arg1, IRubyObject arg2) {
        return getpriority(context.runtime, arg1, arg2);
    }
    public static IRubyObject getpriority(Ruby runtime, IRubyObject arg1, IRubyObject arg2) {
        int which = (int)arg1.convertToInteger().getLongValue();
        int who = (int)arg2.convertToInteger().getLongValue();
        int result = checkErrno(runtime, runtime.getPosix().getpriority(which, who));
        
        return runtime.newFixnum(result);
    }

    @Deprecated
    public static IRubyObject uid(IRubyObject recv) {
        return uid(recv.getRuntime());
    }
    @JRubyMethod(name = "uid", module = true, visibility = PRIVATE)
    public static IRubyObject uid(ThreadContext context, IRubyObject recv) {
        return uid(context.runtime);
    }
    public static IRubyObject uid(Ruby runtime) {
        return runtime.newFixnum(checkErrno(runtime, runtime.getPosix().getuid()));
    }

    @Deprecated
    public static IRubyObject waitpid2(IRubyObject recv, IRubyObject[] args) {
        return waitpid2(recv.getRuntime(), args);
    }
    @JRubyMethod(name = "waitpid2", rest = true, module = true, visibility = PRIVATE)
    public static IRubyObject waitpid2(ThreadContext context, IRubyObject recv, IRubyObject[] args) {
        return waitpid2(context.runtime, args);
    }
    public static IRubyObject waitpid2(Ruby runtime, IRubyObject[] args) {
        int pid = -1;
        int flags = 0;
        if (args.length > 0) {
            pid = (int)args[0].convertToInteger().getLongValue();
        }
        if (args.length > 1) {
            flags = (int)args[1].convertToInteger().getLongValue();
        }
        
        int[] status = new int[1];
        pid = checkErrno(runtime, runtime.getPosix().waitpid(pid, status, flags), ECHILD);
        
        return runtime.newArray(runtime.newFixnum(pid), RubyProcess.RubyStatus.newProcessStatus(runtime, (status[0] >> 8) & 0xff, pid));
    }

    @JRubyMethod(name = "initgroups", required = 2, module = true, visibility = PRIVATE)
    public static IRubyObject initgroups(IRubyObject recv, IRubyObject arg1, IRubyObject arg2) {
        throw recv.getRuntime().newNotImplementedError("Process#initgroups not yet implemented");
    }

    @JRubyMethod(name = "maxgroups=", required = 1, module = true, visibility = PRIVATE)
    public static IRubyObject maxgroups_set(IRubyObject recv, IRubyObject arg) {
        throw recv.getRuntime().newNotImplementedError("Process#maxgroups_set not yet implemented");
    }

    @Deprecated
    public static IRubyObject ppid(IRubyObject recv) {
        return ppid(recv.getRuntime());
    }
    @JRubyMethod(name = "ppid", module = true, visibility = PRIVATE)
    public static IRubyObject ppid(ThreadContext context, IRubyObject recv) {
        return ppid(context.runtime);
    }
    public static IRubyObject ppid(Ruby runtime) {
        int result = checkErrno(runtime, runtime.getPosix().getppid());

        return runtime.newFixnum(result);
    }

    @Deprecated
    public static IRubyObject gid_set(IRubyObject recv, IRubyObject arg) {
        return gid_set(recv.getRuntime(), arg);
    }
    @JRubyMethod(name = "gid=", required = 1, module = true, visibility = PRIVATE)
    public static IRubyObject gid_set(ThreadContext context, IRubyObject recv, IRubyObject arg) {
        return gid_set(context.runtime, arg);
    }
    public static IRubyObject gid_set(Ruby runtime, IRubyObject arg) {
        int result = checkErrno(runtime, runtime.getPosix().setgid((int)arg.convertToInteger().getLongValue()));

        return runtime.newFixnum(result);
    }

    @Deprecated
    public static IRubyObject wait2(IRubyObject recv, IRubyObject[] args) {
        return waitpid2(recv.getRuntime(), args);
    }
    @JRubyMethod(name = "wait2", rest = true, module = true, visibility = PRIVATE)
    public static IRubyObject wait2(ThreadContext context, IRubyObject recv, IRubyObject[] args) {
        return waitpid2(context.runtime, args);
    }

    @Deprecated
    public static IRubyObject euid_set(IRubyObject recv, IRubyObject arg) {
        return euid_set(recv.getRuntime(), arg);
    }
    @JRubyMethod(name = "euid=", required = 1, module = true, visibility = PRIVATE)
    public static IRubyObject euid_set(ThreadContext context, IRubyObject recv, IRubyObject arg) {
        return euid_set(context.runtime, arg);
    }
    public static IRubyObject euid_set(Ruby runtime, IRubyObject arg) {
        checkErrno(runtime, runtime.getPosix().seteuid((int)arg.convertToInteger().getLongValue()));
        return RubyFixnum.zero(runtime);
    }

    @Deprecated
    public static IRubyObject setpriority(IRubyObject recv, IRubyObject arg1, IRubyObject arg2, IRubyObject arg3) {
        return setpriority(recv.getRuntime(), arg1, arg2, arg3);
    }
    @JRubyMethod(name = "setpriority", required = 3, module = true, visibility = PRIVATE)
    public static IRubyObject setpriority(ThreadContext context, IRubyObject recv, IRubyObject arg1, IRubyObject arg2, IRubyObject arg3) {
        return setpriority(context.runtime, arg1, arg2, arg3);
    }
    public static IRubyObject setpriority(Ruby runtime, IRubyObject arg1, IRubyObject arg2, IRubyObject arg3) {
        int which = (int)arg1.convertToInteger().getLongValue();
        int who = (int)arg2.convertToInteger().getLongValue();
        int prio = (int)arg3.convertToInteger().getLongValue();
        runtime.getPosix().errno(0);
        int result = checkErrno(runtime, runtime.getPosix().setpriority(which, who, prio));
        
        return runtime.newFixnum(result);
    }

    @Deprecated
    public static IRubyObject setpgid(IRubyObject recv, IRubyObject arg1, IRubyObject arg2) {
        return setpgid(recv.getRuntime(), arg1, arg2);
    }
    @JRubyMethod(name = "setpgid", required = 2, module = true, visibility = PRIVATE)
    public static IRubyObject setpgid(ThreadContext context, IRubyObject recv, IRubyObject arg1, IRubyObject arg2) {
        return setpgid(context.runtime, arg1, arg2);
    }
    public static IRubyObject setpgid(Ruby runtime, IRubyObject arg1, IRubyObject arg2) {
        int pid = (int)arg1.convertToInteger().getLongValue();
        int gid = (int)arg2.convertToInteger().getLongValue();
        return runtime.newFixnum(checkErrno(runtime, runtime.getPosix().setpgid(pid, gid)));
    }

    @Deprecated
    public static IRubyObject getpgid(IRubyObject recv, IRubyObject arg) {
        return getpgid(recv.getRuntime(), arg);
    }
    @JRubyMethod(name = "getpgid", required = 1, module = true, visibility = PRIVATE)
    public static IRubyObject getpgid(ThreadContext context, IRubyObject recv, IRubyObject arg) {
        return getpgid(context.runtime, arg);
    }
    public static IRubyObject getpgid(Ruby runtime, IRubyObject arg) {
        return runtime.newFixnum(checkErrno(runtime, runtime.getPosix().getpgid((int)arg.convertToInteger().getLongValue())));
    }

    @Deprecated
    public static IRubyObject getrlimit(IRubyObject recv, IRubyObject arg) {
        return getrlimit(recv.getRuntime(), arg);
    }
    @JRubyMethod(name = "getrlimit", required = 1, module = true, visibility = PRIVATE)
    public static IRubyObject getrlimit(ThreadContext context, IRubyObject recv, IRubyObject arg) {
        return getrlimit(context.runtime, arg);
    }
    public static IRubyObject getrlimit(Ruby runtime, IRubyObject arg) {
        throw runtime.newNotImplementedError("Process#getrlimit not yet implemented");
    }

    @Deprecated
    public static IRubyObject egid(IRubyObject recv) {
        return egid(recv.getRuntime());
    }
    @JRubyMethod(name = "egid", module = true, visibility = PRIVATE)
    public static IRubyObject egid(ThreadContext context, IRubyObject recv) {
        return egid(context.runtime);
    }
    public static IRubyObject egid(Ruby runtime) {
        if (Platform.IS_WINDOWS) {
            // MRI behavior on Windows
            return RubyFixnum.zero(runtime);
        }
        return runtime.newFixnum(checkErrno(runtime, runtime.getPosix().getegid()));
    }
    
    private static int parseSignalString(Ruby runtime, String value) {
        boolean negative = value.startsWith("-");

        // Gets rid of the - if there is one present.
        if (negative) value = value.substring(1);

        // We need the SIG for sure.
        String signalName = value.startsWith("SIG") ? value : "SIG" + value;

        try {
            int signalValue = Signal.valueOf(signalName).intValue();
            return negative ? -signalValue : signalValue;

        } catch (IllegalArgumentException ex) {
            throw runtime.newArgumentError("unsupported name `" + signalName + "'");
        }
    }

    @Deprecated
    public static IRubyObject kill(IRubyObject recv, IRubyObject[] args) {
        return kill(recv.getRuntime(), args);
    }
    @JRubyMethod(name = "kill", rest = true, module = true, visibility = PRIVATE)
    public static IRubyObject kill(ThreadContext context, IRubyObject recv, IRubyObject[] args) {
        return kill(context.runtime, args);
    }
    public static IRubyObject kill(Ruby runtime, IRubyObject[] args) {
        if (args.length < 2) {
            throw runtime.newArgumentError("wrong number of arguments -- kill(sig, pid...)");
        }

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
        
        if (processGroupKill) {
		    if (Platform.IS_WINDOWS) {
                throw  runtime.newErrnoEINVALError("group signals not implemented in windows");
            }
		    signal = -signal;
        }
        
		if (Platform.IS_WINDOWS) {
		    for (int i = 1; i < args.length; i++) {
				int pid = RubyNumeric.num2int(args[i]);
				if (signal == 0) {
					jnr.ffi.Pointer ptr = kernel32().OpenProcess(PROCESS_QUERY_INFORMATION, 0, pid);
					if(ptr != null && ptr.address() != -1) {
					   try {
                           IntByReference status = new IntByReference(0);
					       if(kernel32().GetExitCodeProcess(ptr, status) == 0) {
					          throw runtime.newErrnoEPERMError("unable to call GetExitCodeProcess " + pid);
					       } else {
					           if(status.intValue() != STILL_ACTIVE) {
							       throw runtime.newErrnoEPERMError("Process exists but is not alive anymore " + pid);
                               }
					       }
					   } finally {
					     kernel32().CloseHandle(ptr);
					   }
					   
					} else {
					    if (kernel32().GetLastError() == ERROR_INVALID_PARAMETER) {
					        throw runtime.newErrnoESRCHError();
					    } else {
					        throw runtime.newErrnoEPERMError("Process does not exist " + pid);
					    }
					}
			    } else if (signal == 9) { //SIGKILL
				    jnr.ffi.Pointer ptr = kernel32().OpenProcess(PROCESS_TERMINATE | PROCESS_QUERY_INFORMATION, 0, pid);
                    if(ptr != null && ptr.address() != -1) {
					    try {
                            IntByReference status = new IntByReference(0);
					        if(kernel32().GetExitCodeProcess(ptr, status) == 0) {
					            throw runtime.newErrnoEPERMError("unable to call GetExitCodeProcess " + pid); // todo better error messages
					        } else {
					            if (status.intValue() == STILL_ACTIVE) {
						            if (kernel32().TerminateProcess(ptr, 0) == 0) {
						               throw runtime.newErrnoEPERMError("unable to call TerminateProcess " + pid);
						             }
                                     // success									 
						        }
					        }
						} finally {						   
					       kernel32().CloseHandle(ptr);
					    }
					} else {
					    if (kernel32().GetLastError() == ERROR_INVALID_PARAMETER) {
					        throw runtime.newErrnoESRCHError();
					    } else {
					        throw runtime.newErrnoEPERMError("Process does not exist " + pid);
					    }
					}					
				} else {
		            throw runtime.newNotImplementedError("this signal not yet implemented in windows");
		        }
            }			
		} else {		
			POSIX posix = runtime.getPosix();
			for (int i = 1; i < args.length; i++) {
				int pid = RubyNumeric.num2int(args[i]);

				// FIXME: It may be possible to killpg on systems which support it.  POSIX library
				// needs to tell whether a particular method works or not
				if (pid == 0) pid = runtime.getPosix().getpid();
				checkErrno(runtime, posix.kill(processGroupKill ? -pid : pid, signal));
			}
		}
        
        return runtime.newFixnum(args.length - 1);

    }

    @JRubyMethod(name = "detach", required = 1, module = true, visibility = PRIVATE)
    public static IRubyObject detach(ThreadContext context, IRubyObject recv, IRubyObject arg) {
        final int pid = (int)arg.convertToInteger().getLongValue();
        Ruby runtime = context.runtime;
        
        BlockCallback callback = new BlockCallback() {
            public IRubyObject call(ThreadContext context, IRubyObject[] args, Block block) {
                int[] status = new int[1];
                Ruby runtime = context.runtime;
                int result = checkErrno(runtime, runtime.getPosix().waitpid(pid, status, 0));
                
                return runtime.newFixnum(result);
            }
        };
        
        return RubyThread.newInstance(
                runtime.getThread(),
                IRubyObject.NULL_ARRAY,
                CallBlock.newCallClosure(recv, (RubyModule)recv, Arity.NO_ARGUMENTS, callback, context));
    }

    @Deprecated
    public static IRubyObject times(IRubyObject recv, Block unusedBlock) {
        return times(recv.getRuntime());
    }
    @JRubyMethod(module = true, visibility = PRIVATE)
    public static IRubyObject times(ThreadContext context, IRubyObject recv, Block unusedBlock) {
        return times(context.runtime);
    }

    public static IRubyObject times(Ruby runtime) {
        Times tms = runtime.getPosix().times();
        if (tms == null) {
            throw runtime.newErrnoFromLastPOSIXErrno();
        }

        long hz = runtime.getPosix().sysconf(Sysconf._SC_CLK_TCK);
        if (hz == -1) {
            throw runtime.newErrnoFromLastPOSIXErrno();
        }

        return RubyStruct.newStruct(runtime.getTmsStruct(),
                new IRubyObject[] {
                        runtime.newFloat((double) tms.utime() / (double) hz),
                        runtime.newFloat((double) tms.stime() / (double) hz),
                        runtime.newFloat((double) tms.cutime() / (double) hz),
                        runtime.newFloat((double) tms.cstime() / (double) hz)
                },
                Block.NULL_BLOCK);
    }

    @Deprecated
    public static IRubyObject pid(IRubyObject recv) {
        return pid(recv.getRuntime());
    }
    @JRubyMethod(name = "pid", module = true, visibility = PRIVATE)
    public static IRubyObject pid(ThreadContext context, IRubyObject recv) {
        return pid(context.runtime);
    }
    public static IRubyObject pid(Ruby runtime) {
        return runtime.newFixnum(runtime.getPosix().getpid());
    }
    
    @JRubyMethod(name = "fork", module = true, visibility = PRIVATE, compat = RUBY1_8)
    public static IRubyObject fork(ThreadContext context, IRubyObject recv, Block block) {
        return RubyKernel.fork(context, recv, block);
    }
    
    @JRubyMethod(name = "fork", module = true, visibility = PRIVATE, compat = RUBY1_9, notImplemented = true)
    public static IRubyObject fork19(ThreadContext context, IRubyObject recv, Block block) {
        return RubyKernel.fork(context, recv, block);
    }

    // See Process#spawn in src/jruby/kernel19/process.rb
    @JRubyMethod(required = 4, module = true, compat = CompatVersion.RUBY1_9, visibility = PRIVATE)
    public static RubyFixnum _spawn_internal(ThreadContext context, IRubyObject recv, IRubyObject[] args) {
        Ruby runtime = context.runtime;

        IRubyObject env = args[0];
        IRubyObject prog = args[1];
        IRubyObject options = args[2];
        IRubyObject arguments = args[3];

        RubyIO.checkSpawnOptions(options);
        
        long pid = ShellLauncher.runExternalWithoutWait(runtime, env, prog, options, arguments);
        return RubyFixnum.newFixnum(runtime, pid);
    }
    
    @JRubyMethod(name = "exit", optional = 1, module = true, visibility = PRIVATE)
    public static IRubyObject exit(IRubyObject recv, IRubyObject[] args) {
        return RubyKernel.exit(recv, args);
    }
    
    private static final NonNativeErrno IGNORE = new NonNativeErrno() {
        public int handle(Ruby runtime, int result) {return result;}
    };

    private static int checkErrno(Ruby runtime, int result) {
        return checkErrno(runtime, result, IGNORE);
    }

    private static int checkErrno(Ruby runtime, int result, NonNativeErrno nonNative) {
        if (result == -1) {
            if (runtime.getPosix().isNative()) {
                raiseErrnoIfSet(runtime, nonNative);
            } else {
                nonNative.handle(runtime, result);
            }
        }
        return result;
    }

    private static void raiseErrnoIfSet(Ruby runtime, NonNativeErrno nonNative) {
        if (runtime.getPosix().errno() != 0) {
            throw runtime.newErrnoFromInt(runtime.getPosix().errno());
        }
    }
}
