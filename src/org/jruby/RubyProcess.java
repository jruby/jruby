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

import org.jruby.anno.JRubyMethod;
import org.jruby.runtime.Block;
import org.jruby.runtime.CallbackFactory;
import org.jruby.runtime.MethodIndex;
import org.jruby.runtime.ObjectAllocator;
import org.jruby.runtime.Visibility;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.callback.Callback;


/**
 *
 * @author  enebo
 */
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

        CallbackFactory process_statusCallbackFactory = runtime.callbackFactory(RubyProcess.RubyStatus.class);
        
        process.defineAnnotatedMethods(RubyProcess.class);
        process_status.defineAnnotatedMethods(RubyStatus.class);
        process_uid.defineAnnotatedMethods(UserAndGroupID.class);
        process_gid.defineAnnotatedMethods(UserAndGroupID.class);

//    #ifdef HAVE_GETPRIORITY
//        rb_define_const(rb_mProcess, "PRIO_PROCESS", INT2FIX(PRIO_PROCESS));
//        rb_define_const(rb_mProcess, "PRIO_PGRP", INT2FIX(PRIO_PGRP));
//        rb_define_const(rb_mProcess, "PRIO_USER", INT2FIX(PRIO_USER));
//    #endif
        
        // Process::Status methods  
        Callback notImplemented = process_statusCallbackFactory.getFastMethod("not_implemented");
        process_status.defineMethod("&", process_statusCallbackFactory.getFastMethod("not_implemented1", IRubyObject.class));
        process_status.defineMethod("to_int", notImplemented);
        process_status.defineMethod("pid", notImplemented);
        process_status.defineMethod("stopped?", notImplemented);
        process_status.defineMethod("stopsig", notImplemented);
        process_status.defineMethod("signaled?", notImplemented);
        process_status.defineMethod("termsig", notImplemented);
        process_status.defineMethod("exited?", notImplemented);
        process_status.defineMethod("coredump?", notImplemented);
        
        return process;
    }
    
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
        
        public IRubyObject not_implemented() {
            String error = "Process::Status#" + getRuntime().getCurrentContext().getFrameName() + " not implemented";
            throw getRuntime().newNotImplementedError(error);
        }
        
        public IRubyObject not_implemented1(IRubyObject arg) {
            String error = "Process::Status#" + getRuntime().getCurrentContext().getFrameName() + " not implemented";
            throw getRuntime().newNotImplementedError(error);
        }
        
        @JRubyMethod(name = "exitstatus")
        public IRubyObject exitstatus() {
            return getRuntime().newFixnum(status);
        }
        
        @JRubyMethod(name = ">>", required = 1)
        public IRubyObject op_rshift(IRubyObject other) {
            long shiftValue = other.convertToInteger().getLongValue();
            return getRuntime().newFixnum(status >> shiftValue);
        }
        
        @JRubyMethod(name = "==", required = 1)
        public IRubyObject op_equal(IRubyObject other) {
            return other.callMethod(getRuntime().getCurrentContext(), MethodIndex.EQUALEQUAL, "==", this.to_i());
        }

        @JRubyMethod(name = "to_i")
        public IRubyObject to_i() {
            return getRuntime().newFixnum(shiftedValue());
        }
        
        @JRubyMethod(name = "to_s")
        public IRubyObject to_s() {
            return getRuntime().newString(String.valueOf(shiftedValue()));
        }
        
        @JRubyMethod(name = "inspect")
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
    
    public static class UserAndGroupID {
        @JRubyMethod(name = "change_privilege", required = 1, module = true)
        public static IRubyObject change_privilege(IRubyObject self, IRubyObject arg) {
            throw self.getRuntime().newNotImplementedError("Process::UID::change_privilege not implemented yet");
        }
        
        @JRubyMethod(name = "eid", module = true)
        public static IRubyObject eid(IRubyObject self) {
            throw self.getRuntime().newNotImplementedError("Process::UID::eid not implemented yet");
        }
        
        @JRubyMethod(name = "eid=", required = 1, module = true)
        public static IRubyObject eid(IRubyObject self, IRubyObject arg) {
            throw self.getRuntime().newNotImplementedError("Process::UID::eid= not implemented yet");
        }
        
        @JRubyMethod(name = "grant_privilege", required = 1, module = true)
        public static IRubyObject grant_privilege(IRubyObject self, IRubyObject arg) {
            throw self.getRuntime().newNotImplementedError("Process::UID::grant_privilege not implemented yet");
        }
        
        @JRubyMethod(name = "re_exchange", module = true)
        public static IRubyObject re_exchange(IRubyObject self) {
            throw self.getRuntime().newNotImplementedError("Process::UID::re_exchange not implemented yet");
        }
        
        @JRubyMethod(name = "re_exchangeable?", module = true)
        public static IRubyObject re_exchangeable_p(IRubyObject self) {
            throw self.getRuntime().newNotImplementedError("Process::UID::re_exchangeable? not implemented yet");
        }
        
        @JRubyMethod(name = "rid", module = true)
        public static IRubyObject rid(IRubyObject self) {
            throw self.getRuntime().newNotImplementedError("Process::UID::rid not implemented yet");
        }
        
        @JRubyMethod(name = "sid_available?", module = true)
        public static IRubyObject sid_available_p(IRubyObject self) {
            throw self.getRuntime().newNotImplementedError("Process::UID::sid_available not implemented yet");
        }
        
        @JRubyMethod(name = "switch", module = true)
        public static IRubyObject switch_rb(IRubyObject self) {
            throw self.getRuntime().newNotImplementedError("Process::UID::switch not implemented yet");
        }
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
        return recv.getRuntime().newFixnum(Ruby.getPosix().getpgrp());
    }

    @JRubyMethod(name = "groups=", required = 1, module = true, visibility = Visibility.PRIVATE)
    public static IRubyObject groups_set(IRubyObject recv, IRubyObject arg) {
        throw recv.getRuntime().newNotImplementedError("Process#groups not yet implemented");
    }

    @JRubyMethod(name = "waitpid", rest = true, module = true, visibility = Visibility.PRIVATE)
    public static IRubyObject waitpid(IRubyObject recv, IRubyObject[] args) {
        throw recv.getRuntime().newNotImplementedError("Process#waitpid not yet implemented");
    }

    @JRubyMethod(name = "wait", rest = true, module = true, visibility = Visibility.PRIVATE)
    public static IRubyObject wait(IRubyObject recv, IRubyObject[] args) {
        throw recv.getRuntime().newNotImplementedError("Process#wait not yet implemented");
    }

    @JRubyMethod(name = "waitall", module = true, visibility = Visibility.PRIVATE)
    public static IRubyObject waitall(IRubyObject recv) {
        throw recv.getRuntime().newNotImplementedError("Process#waitall not yet implemented");
    }

    @JRubyMethod(name = "setsid", module = true, visibility = Visibility.PRIVATE)
    public static IRubyObject setsid(IRubyObject recv) {
        throw recv.getRuntime().newNotImplementedError("Process#setsid not yet implemented");
    }

    @JRubyMethod(name = "setpgrp", module = true, visibility = Visibility.PRIVATE)
    public static IRubyObject setpgrp(IRubyObject recv) {
        throw recv.getRuntime().newNotImplementedError("Process#setpgrp not yet implemented");
    }

    @JRubyMethod(name = "egid=", required = 1, module = true, visibility = Visibility.PRIVATE)
    public static IRubyObject egid_set(IRubyObject recv, IRubyObject arg) {
        throw recv.getRuntime().newNotImplementedError("Process#egid= not yet implemented");
    }

    @JRubyMethod(name = "euid", module = true, visibility = Visibility.PRIVATE)
    public static IRubyObject euid(IRubyObject recv) {
        return recv.getRuntime().newFixnum(Ruby.getPosix().geteuid());
    }

    @JRubyMethod(name = "uid=", required = 1, module = true, visibility = Visibility.PRIVATE)
    public static IRubyObject uid_set(IRubyObject recv, IRubyObject arg) {
        throw recv.getRuntime().newNotImplementedError("Process#uid= not yet implemented");
    }

    @JRubyMethod(name = "gid", module = true, visibility = Visibility.PRIVATE)
    public static IRubyObject gid(IRubyObject recv) {
        return recv.getRuntime().newFixnum(Ruby.getPosix().getgid());
    }

    @JRubyMethod(name = "maxgroups", module = true, visibility = Visibility.PRIVATE)
    public static IRubyObject maxgroups(IRubyObject recv) {
        throw recv.getRuntime().newNotImplementedError("Process#maxgroups not yet implemented");
    }

    @JRubyMethod(name = "getpriority", required = 2, module = true, visibility = Visibility.PRIVATE)
    public static IRubyObject getpriority(IRubyObject recv, IRubyObject arg1, IRubyObject arg2) {
        throw recv.getRuntime().newNotImplementedError("Process#getpriority not yet implemented");
    }

    @JRubyMethod(name = "uid", module = true, visibility = Visibility.PRIVATE)
    public static IRubyObject uid(IRubyObject recv) {
        return recv.getRuntime().newFixnum(Ruby.getPosix().getuid());
    }

    @JRubyMethod(name = "waitpid2", rest = true, module = true, visibility = Visibility.PRIVATE)
    public static IRubyObject waitpid2(IRubyObject recv, IRubyObject[] args) {
        throw recv.getRuntime().newNotImplementedError("Process#waitpid2 not yet implemented");
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
        return recv.getRuntime().newFixnum(Ruby.getPosix().getppid());
    }

    @JRubyMethod(name = "gid=", required = 1, module = true, visibility = Visibility.PRIVATE)
    public static IRubyObject gid_set(IRubyObject recv, IRubyObject arg) {
        throw recv.getRuntime().newNotImplementedError("Process#gid= not yet implemented");
    }

    @JRubyMethod(name = "wait2", rest = true, module = true, visibility = Visibility.PRIVATE)
    public static IRubyObject wait2(IRubyObject recv, IRubyObject[] args) {
        throw recv.getRuntime().newNotImplementedError("Process#wait2 not yet implemented");
    }

    @JRubyMethod(name = "euid=", required = 1, module = true, visibility = Visibility.PRIVATE)
    public static IRubyObject euid_set(IRubyObject recv, IRubyObject arg) {
        throw recv.getRuntime().newNotImplementedError("Process#euid= not yet implemented");
    }

    @JRubyMethod(name = "setpriority", required = 3, module = true, visibility = Visibility.PRIVATE)
    public static IRubyObject setpriority(IRubyObject recv, IRubyObject arg1, IRubyObject arg2, IRubyObject arg3) {
        throw recv.getRuntime().newNotImplementedError("Process#setpriority not yet implemented");
    }

    @JRubyMethod(name = "setpgid", required = 2, module = true, visibility = Visibility.PRIVATE)
    public static IRubyObject setpgid(IRubyObject recv, IRubyObject arg1, IRubyObject arg2) {
        throw recv.getRuntime().newNotImplementedError("Process#setpgid not yet implemented");
    }

    @JRubyMethod(name = "getpgid", required = 1, module = true, visibility = Visibility.PRIVATE)
    public static IRubyObject getpgid(IRubyObject recv, IRubyObject arg) {
        return recv.getRuntime().newFixnum(Ruby.getPosix().getpgid());
    }

    @JRubyMethod(name = "getrlimit", required = 1, module = true, visibility = Visibility.PRIVATE)
    public static IRubyObject getrlimit(IRubyObject recv, IRubyObject arg) {
        throw recv.getRuntime().newNotImplementedError("Process#getrlimit not yet implemented");
    }

    @JRubyMethod(name = "egid", module = true, visibility = Visibility.PRIVATE)
    public static IRubyObject egid(IRubyObject recv) {
        return recv.getRuntime().newFixnum(Ruby.getPosix().getegid());
    }

    @JRubyMethod(name = "kill", rest = true, module = true, visibility = Visibility.PRIVATE)
    public static IRubyObject kill(IRubyObject recv, IRubyObject[] args) {
        throw recv.getRuntime().newNotImplementedError("Process#kill not yet implemented");
    }

    @JRubyMethod(name = "detach", required = 1, module = true, visibility = Visibility.PRIVATE)
    public static IRubyObject detach(IRubyObject recv, IRubyObject arg) {
        throw recv.getRuntime().newNotImplementedError("Process#detach not yet implemented");
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
        return recv.getRuntime().newFixnum(System.identityHashCode(recv.getRuntime()));
    }
}
