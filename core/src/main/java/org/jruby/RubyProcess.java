/*
 **** BEGIN LICENSE BLOCK *****
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

import jnr.constants.platform.RLIM;
import jnr.constants.platform.RLIMIT;
import jnr.constants.platform.Sysconf;
import jnr.ffi.byref.IntByReference;
import jnr.posix.Group;
import jnr.posix.Passwd;
import jnr.posix.POSIX;
import jnr.posix.RLimit;
import jnr.posix.Times;
import jnr.posix.Timeval;
import org.jruby.anno.JRubyClass;
import org.jruby.anno.JRubyMethod;
import org.jruby.anno.JRubyModule;
import org.jruby.api.Convert;
import org.jruby.javasupport.JavaUtil;
import org.jruby.platform.Platform;
import org.jruby.runtime.Arity;
import org.jruby.runtime.Block;
import org.jruby.runtime.BlockCallback;
import org.jruby.runtime.CallBlock;
import org.jruby.runtime.ObjectMarshal;
import org.jruby.runtime.Signature;
import org.jruby.runtime.ThreadContext;

import static org.jruby.api.Convert.*;
import static org.jruby.api.Create.*;
import static org.jruby.api.Define.defineModule;
import static org.jruby.api.Error.argumentError;
import static org.jruby.api.Error.rangeError;
import static org.jruby.runtime.Helpers.throwException;
import static org.jruby.runtime.Visibility.*;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.builtin.Variable;
import org.jruby.runtime.component.VariableEntry;
import org.jruby.runtime.invokedynamic.MethodNames;
import org.jruby.runtime.marshal.CoreObjectType;
import org.jruby.runtime.marshal.MarshalStream;
import org.jruby.runtime.marshal.NewMarshal;
import org.jruby.runtime.marshal.UnmarshalStream;
import org.jruby.util.ShellLauncher;
import org.jruby.exceptions.RaiseException;
import org.jruby.util.TypeConverter;
import org.jruby.util.cli.Options;
import org.jruby.util.io.PopenExecutor;
import org.jruby.util.io.PosixShim;

import static org.jruby.runtime.Helpers.invokedynamic;
import static org.jruby.util.WindowsFFI.kernel32;
import static org.jruby.util.WindowsFFI.Kernel32.*;

import java.io.IOException;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.management.ThreadMXBean;
import java.lang.management.ManagementFactory;
import java.lang.reflect.Method;
import java.util.List;
import java.util.function.ToIntFunction;

/**
 */

@JRubyModule(name="Process")
public class RubyProcess {

    public static RubyModule createProcessModule(ThreadContext context, RubyClass Object, RubyClass Struct) {
        RubyModule Process = defineModule(context, "Process").
                defineMethods(context, RubyProcess.class).
                defineConstant(context, "WNOHANG", asFixnum(context, 1)).   // FIXME: should come from jnr-constants
                defineConstant(context, "WUNTRACED", asFixnum(context, 2)). // FIXME: should come from jnr-constants
                defineConstant(context, "CLOCK_REALTIME", asSymbol(context, CLOCK_REALTIME)).
                defineConstant(context, "CLOCK_MONOTONIC", asSymbol(context, CLOCK_MONOTONIC)).
                tap(c -> c.defineConstantsFrom(context, jnr.constants.platform.PRIO.class));
                // TODO: other clock types

        RubyClass ProcessStatus = Process.defineClassUnder(context, "Status", Object, RubyProcess::newAllocatedProcessStatus).
                marshalWith(PROCESS_STATUS_MARSHAL).defineMethods(context, RubyStatus.class);
        context.runtime.setProcStatus(ProcessStatus);
        context.runtime.setProcUID(Process.defineModuleUnder(context, "UID").defineMethods(context, UserID.class));
        context.runtime.setProcGID(Process.defineModuleUnder(context, "GID").defineMethods(context, GroupID.class));
        context.runtime.setProcSys(Process.defineModuleUnder(context, "Sys").defineMethods(context, Sys.class));

        if (Platform.IS_WINDOWS) {
            // mark rlimit methods as not implemented and skip defining the constants (GH-6491)
            Process.getSingletonClass().retrieveMethod("getrlimit").setNotImplemented(true);
            Process.getSingletonClass().retrieveMethod("setrlimit").setNotImplemented(true);
        } else {
            Process.defineConstantsFrom(context, jnr.constants.platform.RLIM.class);
            for (RLIMIT r : RLIMIT.values()) {
                if (!r.defined()) continue;
                Process.defineConstant(r.name(), asFixnum(context, r.intValue()));
            }
        }

        RubyClass tmsStruct = RubyStruct.newInstance(context, Struct,
                new IRubyObject[] {
                        newString(context, "Tms"),
                        Convert.asSymbol(context, "utime"),
                        Convert.asSymbol(context, "stime"),
                        Convert.asSymbol(context, "cutime"),
                        Convert.asSymbol(context, "cstime")},
                Block.NULL_BLOCK);

        Process.defineConstant("Tms", tmsStruct);
        context.runtime.setTmsStruct(tmsStruct);

        return Process;
    }

    public static final String CLOCK_MONOTONIC = "CLOCK_MONOTONIC";
    public static final String CLOCK_REALTIME = "CLOCK_REALTIME";
    public static final String CLOCK_UNIT_NANOSECOND = "nanosecond";
    public static final String CLOCK_UNIT_MICROSECOND = "microsecond";
    public static final String CLOCK_UNIT_MILLISECOND = "millisecond";
    public static final String CLOCK_UNIT_SECOND = "second";
    public static final String CLOCK_UNIT_FLOAT_MICROSECOND = "float_microsecond";
    public static final String CLOCK_UNIT_FLOAT_MILLISECOND = "float_millisecond";
    public static final String CLOCK_UNIT_FLOAT_SECOND = "float_second";
    public static final String CLOCK_UNIT_HERTZ = "hertz";

    private static final long PROCESS_STATUS_UNINITIALIZED = -1;

    private static final ObjectMarshal PROCESS_STATUS_MARSHAL = new ObjectMarshal() {
        @Override
        public void marshalTo(Ruby runtime, Object obj, RubyClass type,
                              MarshalStream marshalStream) throws IOException {
            RubyStatus status = (RubyStatus) obj;

            marshalStream.registerLinkTarget(status);
            List<Variable<Object>> attrs = status.getMarshalVariableList();

            attrs.add(new VariableEntry("status", runtime.newFixnum(status.status)));
            attrs.add(new VariableEntry("pid", runtime.newFixnum(status.pid)));

            marshalStream.dumpVariables(attrs);
        }

        @Override
        public void marshalTo(Object obj, RubyClass type,
                              NewMarshal marshalStream, ThreadContext context, NewMarshal.RubyOutputStream out) {
            RubyStatus status = (RubyStatus) obj;

            marshalStream.registerLinkTarget(status);

            marshalStream.dumpVariables(context, out, status, 3, (marshal, c, o, v, receiver) -> {
                // TODO: marshal these values directly
                receiver.receive(marshal, c, o, "status", asFixnum(c, v.status));
                receiver.receive(marshal, c, o, "pid", asFixnum(c, v.pid));
            });
        }

        @Override
        public Object unmarshalFrom(Ruby runtime, RubyClass type, UnmarshalStream input) throws IOException {
            RubyStatus status = (RubyStatus) input.entry(type.allocate());

            input.ivar(null, status, null);

            RubyFixnum pstatus = (RubyFixnum) status.removeInternalVariable("status");
            RubyFixnum pid = (RubyFixnum) status.removeInternalVariable("pid");

            status.status = pstatus.getLongValue();
            status.pid = pid.getLongValue();

            return status;
        }
    };

    public static RubyStatus newAllocatedProcessStatus(Ruby runtime, RubyClass metaClass) {
        return new RubyStatus(runtime, metaClass, 0, PROCESS_STATUS_UNINITIALIZED);
    }

    @JRubyClass(name="Process::Status")
    public static class RubyStatus extends RubyObject {
        private long status;
        private long pid; // pid or -1 to indicate status was allocate()d and is uninitalized.

        private static final long EXIT_SUCCESS = 0L;
        public RubyStatus(Ruby runtime, RubyClass metaClass, long status, long pid) {
            super(runtime, metaClass);
            this.status = status;
            this.pid = pid;
        }

        public static RubyStatus newProcessStatus(Ruby runtime, long status, long pid) {
            return new RubyStatus(runtime, runtime.getProcStatus(), status, pid);
        }

        @JRubyMethod(module = true, optional = 2, checkArity = false)
        public static IRubyObject wait(ThreadContext context, IRubyObject self, IRubyObject[] args) {
            int argc = Arity.checkArgumentCount(context, args, 0, 2);

            long pid = argc > 0 ? args[0].convertToInteger().getLongValue() : -1;
            int flags = argc > 1 ? args[1].convertToInteger().getIntValue() : 0;

            return waitpidStatus(context, pid, flags);
            //checkErrno(runtime, pid, ECHILD);
        }

        @JRubyMethod(name = "&")
        public IRubyObject op_and(ThreadContext context, IRubyObject arg) {
            long mask = arg.convertToInteger().getLongValue();

            if (mask < 0) throw argumentError(context, "negative mask value: " + mask);
            if (mask > Integer.MAX_VALUE || mask < Integer.MIN_VALUE) {
                throw rangeError(context, "mask value out of range: " + mask);
            }

            String message = switch ((int) mask) {
                case 0x80 -> "Process::Status#coredump?";
                case 0x7f -> "Process::Status#signaled? or Process::Status#termsig";
                case 0xff -> "Process::Status#exited?, Process::Status#stopped? or Process::Status#coredump?";
                case 0xff00 -> "Process::Status#exitstatus or Process::Status#stopsig";
                default -> "other Process::Status predicates";
            };
            deprecateAndSuggest(context, "&", message);

            return asFixnum(context, status & mask);
        }

        private static void deprecateAndSuggest(ThreadContext context, String method, String suggest) {
            context.runtime.getWarnings().warnDeprecatedForRemoval("Use " + suggest + " instead of Process::Status#" + method, "3.4");
        }

        @JRubyMethod(name = "stopped?")
        public IRubyObject stopped_p(ThreadContext context) {
            return asBoolean(context, PosixShim.WAIT_MACROS.WIFSTOPPED(status));
        }

        @Deprecated
        public IRubyObject stopped_p() {
            return stopped_p(getRuntime().getCurrentContext());
        }

        @JRubyMethod(name = "signaled?")
        public IRubyObject signaled(ThreadContext context) {
            return asBoolean(context, PosixShim.WAIT_MACROS.WIFSIGNALED(status));
        }

        @Deprecated
        public IRubyObject signaled() {
            return signaled(getRuntime().getCurrentContext());
        }

        @JRubyMethod(name = "exited?")
        public IRubyObject exited(ThreadContext context) {
            return asBoolean(context, PosixShim.WAIT_MACROS.WIFEXITED(status));
        }

        public IRubyObject exited() {
            return exited(getRuntime().getCurrentContext());
        }

        /**
         * @return ""
         * @deprecated Use {@link RubyStatus#stopsig(ThreadContext)} instead.
         */
        @Deprecated(since = "10.0", forRemoval = true)
        public IRubyObject stopsig() {
            return stopsig(getCurrentContext());
        }

        @JRubyMethod
        public IRubyObject stopsig(ThreadContext context) {
            return PosixShim.WAIT_MACROS.WIFSTOPPED(status) ?
                    asFixnum(context, PosixShim.WAIT_MACROS.WSTOPSIG(status)) : context.nil;
        }

        /**
         * @return ""
         * @deprecated Use {@link RubyStatus#termsig(ThreadContext)} instead.
         */
        @Deprecated(since = "10.0", forRemoval = true)
        public IRubyObject termsig() {
            return termsig(getCurrentContext());
        }

        @JRubyMethod
        public IRubyObject termsig(ThreadContext context) {
            return PosixShim.WAIT_MACROS.WIFSIGNALED(status) ?
                asFixnum(context, PosixShim.WAIT_MACROS.WTERMSIG(status)) : context.nil;
        }

        @Deprecated
        public IRubyObject exitstatus() {
            return exitstatus(getCurrentContext());
        }
        @JRubyMethod
        public IRubyObject exitstatus(ThreadContext context) {
            return PosixShim.WAIT_MACROS.WIFEXITED(status) ?
                asFixnum(context, PosixShim.WAIT_MACROS.WEXITSTATUS(status)) : context.nil;
        }

        @JRubyMethod(name = ">>")
        public IRubyObject op_rshift(ThreadContext context, IRubyObject other) {
            long places = other.convertToInteger().getLongValue();

            if (places < 0) throw argumentError(context, "negative shift value: " + places);
            if (places > Integer.MAX_VALUE) throw rangeError(context, "shift value out of range: " + places);

            deprecateAndSuggest(context, ">>", switch ((int) places) {
                case 7 -> "Process::Status#coredump?";
                case 8 -> "Process::Status#exitstatus or Process::Status#stopsig";
                default -> "other Process::Status predicates";
            });

            return asFixnum(context, status >> places);
        }

        @Override
        @JRubyMethod(name = "==")
        public IRubyObject op_equal(ThreadContext context, IRubyObject other) {
            return this == other ? context.tru : invokedynamic(context, asFixnum(context, status), MethodNames.OP_EQUAL, other);
        }

        @JRubyMethod
        public IRubyObject to_i(ThreadContext context) {
            return to_i(context.runtime);
        }

        @JRubyMethod
        public IRubyObject inspect(ThreadContext context) {
            return inspect(context.runtime);
        }

        @JRubyMethod(name = "success?")
        public IRubyObject success_p(ThreadContext context) {
            return !PosixShim.WAIT_MACROS.WIFEXITED(status) ?
                    context.nil : asBoolean(context, PosixShim.WAIT_MACROS.WEXITSTATUS(status) == EXIT_SUCCESS);
        }

        @JRubyMethod(name = "coredump?")
        public IRubyObject coredump_p(ThreadContext context) {
            return asBoolean(context, PosixShim.WAIT_MACROS.WCOREDUMP(status));
        }

        public IRubyObject coredump_p() {
            return coredump_p(getRuntime().getCurrentContext());
        }

        @JRubyMethod
        public IRubyObject pid(ThreadContext context) {
            return asFixnum(context, pid);
        }

        public long getStatus() {
            return status;
        }

        public IRubyObject to_i(Ruby runtime) {
            return runtime.newFixnum(status);
        }

        @Override
        @JRubyMethod
        public IRubyObject to_s(ThreadContext context) {
            return newString(context, pst_message("", pid, status));
        }

        boolean unitialized() {
            return pid == PROCESS_STATUS_UNINITIALIZED;
        }

        public IRubyObject inspect(Ruby runtime) {
            return unitialized() ?
                    runtime.newString("#<" + getMetaClass().getName() + ": uninitialized>") :
                    runtime.newString(pst_message("#<" + getMetaClass().getName() + ": ", pid, status) + ">");
        }

        // MRI: pst_message
        public static String pst_message(String prefix, long pid, long status) {
            StringBuilder sb = new StringBuilder(prefix);
            sb.append("pid ").append(pid);
            if (PosixShim.WAIT_MACROS.WIFSTOPPED(status)) {
                long stopsig = PosixShim.WAIT_MACROS.WSTOPSIG(status);
                String signame = RubySignal.signo2signm(stopsig);
                if (signame != null) {
                    sb.append(" stopped ").append(signame).append(" (signal ").append(stopsig).append(")");
                } else {
                    sb.append(" stopped signal ").append(stopsig);
                }
            }
            if (PosixShim.WAIT_MACROS.WIFSIGNALED(status)) {
                long termsig = PosixShim.WAIT_MACROS.WTERMSIG(status);
                String signame = RubySignal.signo2signm(termsig);
                if (signame != null) {
                    sb.append(" ").append(signame).append(" (signal ").append(termsig).append(")");
                } else {
                    sb.append(" signal ").append(termsig);
                }
            }
            if (PosixShim.WAIT_MACROS.WIFEXITED(status)) {
                sb.append(" exit ").append(PosixShim.WAIT_MACROS.WEXITSTATUS(status));
            }
            if (PosixShim.WAIT_MACROS.WCOREDUMP(status)) {
                sb.append(" (core dumped)");
            }
            return sb.toString();
        }

        @Override
        public IRubyObject inspect() {
            return inspect(getRuntime());
        }

        @Deprecated
        public IRubyObject to_i() {
            return to_i(getRuntime());
        }

        @Deprecated
        public IRubyObject op_rshift(Ruby runtime, IRubyObject other) {
            long shiftValue = other.convertToInteger().getLongValue();
            return asFixnum(runtime.getCurrentContext(), status >> shiftValue);
        }

        @Deprecated
        public IRubyObject op_and(IRubyObject arg) {
            return op_and(getRuntime().getCurrentContext(), arg);
        }
    }

    @JRubyModule(name="Process::UID")
    public static class UserID {
        @JRubyMethod(name = "change_privilege", module = true, visibility = PRIVATE)
        public static IRubyObject change_privilege(IRubyObject self, IRubyObject arg) {
            throw self.getRuntime().newNotImplementedError("Process::UID::change_privilege not implemented yet");
        }

        @Deprecated
        public static IRubyObject eid(IRubyObject self) {
            return euid(self.getRuntime());
        }
        @JRubyMethod(name = "eid", module = true, visibility = PRIVATE)
        public static IRubyObject eid(ThreadContext context, IRubyObject self) {
            return euid(context, self);
        }

        @Deprecated
        public static IRubyObject eid(IRubyObject self, IRubyObject arg) {
            return eid(self.getRuntime(), arg);
        }
        @JRubyMethod(name = "eid=", module = true, visibility = PRIVATE)
        public static IRubyObject eid(ThreadContext context, IRubyObject self, IRubyObject arg) {
            return euid_set(context, self, arg);
        }
        public static IRubyObject eid(Ruby runtime, IRubyObject arg) {
            return euid_set(runtime, arg);
        }

        @JRubyMethod(name = "grant_privilege", module = true, visibility = PRIVATE)
        public static IRubyObject grant_privilege(IRubyObject self, IRubyObject arg) {
            throw self.getRuntime().newNotImplementedError("Process::UID::grant_privilege not implemented yet");
        }

        @JRubyMethod(name = "re_exchange", module = true, visibility = PRIVATE)
        public static IRubyObject re_exchange(ThreadContext context, IRubyObject self) {
            return switch_rb(context, self, Block.NULL_BLOCK);
        }

        @JRubyMethod(name = "re_exchangeable?", module = true, visibility = PRIVATE)
        public static IRubyObject re_exchangeable_p(IRubyObject self) {
            throw self.getRuntime().newNotImplementedError("Process::UID::re_exchangeable? not implemented yet");
        }

        @Deprecated
        public static IRubyObject rid(IRubyObject self) {
            return rid(self.getRuntime());
        }
        @JRubyMethod(name = "rid", module = true, visibility = PRIVATE)
        public static IRubyObject rid(ThreadContext context, IRubyObject self) {
            return rid(context.runtime);
        }
        public static IRubyObject rid(Ruby runtime) {
            return uid(runtime);
        }

        @JRubyMethod(name = "sid_available?", module = true, visibility = PRIVATE)
        public static IRubyObject sid_available_p(IRubyObject self) {
            throw self.getRuntime().newNotImplementedError("Process::UID::sid_available not implemented yet");
        }

        @JRubyMethod(name = "switch", module = true, visibility = PRIVATE)
        public static IRubyObject switch_rb(ThreadContext context, IRubyObject self, Block block) {
            var posix = context.runtime.getPosix();
            int uid = checkErrno(context, posix.getuid());
            int euid = checkErrno(context, posix.geteuid());

            if (block.isGiven()) {
                try {
                    checkErrno(context, posix.seteuid(uid));
                    checkErrno(context, posix.setuid(euid));

                    return block.yield(context, context.nil);
                } finally {
                    checkErrno(context, posix.seteuid(euid));
                    checkErrno(context, posix.setuid(uid));
                }
            } else {
                checkErrno(context, posix.seteuid(uid));
                checkErrno(context, posix.setuid(euid));

                return RubyFixnum.zero(context.runtime);
            }
        }
    }

    @JRubyModule(name="Process::GID")
    public static class GroupID {
        @JRubyMethod(name = "change_privilege", module = true, visibility = PRIVATE)
        public static IRubyObject change_privilege(IRubyObject self, IRubyObject arg) {
            throw self.getRuntime().newNotImplementedError("Process::GID::change_privilege not implemented yet");
        }

        @Deprecated
        public static IRubyObject eid(IRubyObject self) {
            return eid(self.getRuntime());
        }
        @JRubyMethod(name = "eid", module = true, visibility = PRIVATE)
        public static IRubyObject eid(ThreadContext context, IRubyObject self) {
            return eid(context.runtime);
        }
        public static IRubyObject eid(Ruby runtime) {
            return egid(runtime.getCurrentContext(), null);
        }

        @Deprecated
        public static IRubyObject eid(IRubyObject self, IRubyObject arg) {
            return eid(self.getRuntime(), arg);
        }
        @JRubyMethod(name = "eid=", module = true, visibility = PRIVATE)
        public static IRubyObject eid(ThreadContext context, IRubyObject self, IRubyObject arg) {
            return eid(context.runtime, arg);
        }
        public static IRubyObject eid(Ruby runtime, IRubyObject arg) {
            return RubyProcess.egid_set(runtime.getCurrentContext(), arg);
        }

        @JRubyMethod(name = "grant_privilege", module = true, visibility = PRIVATE)
        public static IRubyObject grant_privilege(IRubyObject self, IRubyObject arg) {
            throw self.getRuntime().newNotImplementedError("Process::GID::grant_privilege not implemented yet");
        }

        @JRubyMethod(name = "re_exchange", module = true, visibility = PRIVATE)
        public static IRubyObject re_exchange(ThreadContext context, IRubyObject self) {
            return switch_rb(context, self, Block.NULL_BLOCK);
        }

        @JRubyMethod(name = "re_exchangeable?", module = true, visibility = PRIVATE)
        public static IRubyObject re_exchangeable_p(IRubyObject self) {
            throw self.getRuntime().newNotImplementedError("Process::GID::re_exchangeable? not implemented yet");
        }

        @Deprecated
        public static IRubyObject rid(IRubyObject self) {
            return rid(self.getRuntime());
        }
        @JRubyMethod(name = "rid", module = true, visibility = PRIVATE)
        public static IRubyObject rid(ThreadContext context, IRubyObject self) {
            return rid(context.runtime);
        }
        public static IRubyObject rid(Ruby runtime) {
            return gid(runtime);
        }

        @JRubyMethod(name = "sid_available?", module = true, visibility = PRIVATE)
        public static IRubyObject sid_available_p(IRubyObject self) {
            throw self.getRuntime().newNotImplementedError("Process::GID::sid_available not implemented yet");
        }

        @JRubyMethod(name = "switch", module = true, visibility = PRIVATE)
        public static IRubyObject switch_rb(ThreadContext context, IRubyObject self, Block block) {
            var posix = context.runtime.getPosix();
            int gid = checkErrno(context, posix.getgid());
            int egid = checkErrno(context, posix.getegid());

            if (block.isGiven()) {
                try {
                    checkErrno(context, posix.setegid(gid));
                    checkErrno(context, posix.setgid(egid));

                    return block.yield(context, context.nil);
                } finally {
                    checkErrno(context, posix.setegid(egid));
                    checkErrno(context, posix.setgid(gid));
                }
            } else {
                checkErrno(context, posix.setegid(gid));
                checkErrno(context, posix.setgid(egid));

                return RubyFixnum.zero(context.runtime);
            }
        }
    }

    @JRubyModule(name="Process::Sys")
    public static class Sys {
        @Deprecated
        public static IRubyObject getegid(IRubyObject self) {
            return egid(self.getRuntime().getCurrentContext(), null);
        }
        @JRubyMethod(name = "getegid", module = true, visibility = PRIVATE)
        public static IRubyObject getegid(ThreadContext context, IRubyObject self) {
            return egid(context, self);
        }

        @Deprecated
        public static IRubyObject geteuid(IRubyObject self) {
            return euid(((RubyBasicObject) self).getCurrentContext(), self);
        }
        @JRubyMethod(name = "geteuid", module = true, visibility = PRIVATE)
        public static IRubyObject geteuid(ThreadContext context, IRubyObject self) {
            return euid(context, self);
        }

        @Deprecated
        public static IRubyObject getgid(IRubyObject self) {
            return gid(self.getRuntime());
        }
        @JRubyMethod(name = "getgid", module = true, visibility = PRIVATE)
        public static IRubyObject getgid(ThreadContext context, IRubyObject self) {
            return gid(context, self);
        }

        @Deprecated
        public static IRubyObject getuid(IRubyObject self) {
            return uid(self.getRuntime());
        }
        @JRubyMethod(name = "getuid", module = true, visibility = PRIVATE)
        public static IRubyObject getuid(ThreadContext context, IRubyObject self) {
            return uid(context, self);
        }

        @Deprecated
        public static IRubyObject setegid(IRubyObject recv, IRubyObject arg) {
            return egid_set(recv.getRuntime().getCurrentContext(), arg);
        }
        @JRubyMethod(name = "setegid", module = true, visibility = PRIVATE)
        public static IRubyObject setegid(ThreadContext context, IRubyObject recv, IRubyObject arg) {
            return egid_set(context, recv, arg);
        }

        @Deprecated
        public static IRubyObject seteuid(IRubyObject recv, IRubyObject arg) {
            return euid_set(recv.getRuntime().getCurrentContext(), arg);
        }
        @JRubyMethod(name = "seteuid", module = true, visibility = PRIVATE)
        public static IRubyObject seteuid(ThreadContext context, IRubyObject recv, IRubyObject arg) {
            return euid_set(context, arg);
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
            return uid_set(recv.getRuntime().getCurrentContext(), null, arg);
        }
        @JRubyMethod(name = "setuid", module = true, visibility = PRIVATE)
        public static IRubyObject setuid(ThreadContext context, IRubyObject recv, IRubyObject arg) {
            return uid_set(context, null, arg);
        }
    }

    @JRubyMethod(name = "abort", optional = 1, checkArity = false, module = true, visibility = PRIVATE)
    public static IRubyObject abort(ThreadContext context, IRubyObject recv, IRubyObject[] args) {
        return RubyKernel.abort(context, recv, args);
    }

    @JRubyMethod(name = "exit!", optional = 1, checkArity = false, module = true, visibility = PRIVATE)
    public static IRubyObject exit_bang(IRubyObject recv, IRubyObject[] args) {
        return RubyKernel.exit_bang(recv.getRuntime().getCurrentContext(), recv, args);
    }

    @JRubyMethod(name = "groups", module = true, visibility = PRIVATE)
    public static IRubyObject groups(IRubyObject recv) {
        final Ruby runtime = recv.getRuntime();
        long[] groups = runtime.getPosix().getgroups();
        if (groups == null) { // not-implemented for the given platform (e.g. Windows)
            throw runtime.newNotImplementedError("groups() function is unimplemented on this machine");
        }
        IRubyObject[] ary = new IRubyObject[groups.length];
        for(int i = 0; i < groups.length; i++) {
            ary[i] = RubyFixnum.newFixnum(runtime, groups[i]);
        }
        return RubyArray.newArrayNoCopy(runtime, ary);
    }

    @JRubyMethod(name = "last_status", module = true, visibility = PRIVATE)
    public static IRubyObject last_status(ThreadContext context, IRubyObject recv) {
        return context.getLastExitStatus();
    }

    @JRubyMethod(name = "setrlimit", module = true, visibility = PRIVATE)
    public static IRubyObject setrlimit(ThreadContext context, IRubyObject recv, IRubyObject resource, IRubyObject rlimCur) {
        return setrlimit(context, recv, resource, rlimCur, context.nil);
    }

    @JRubyMethod(name = "setrlimit", module = true, visibility = PRIVATE)
    public static IRubyObject setrlimit(ThreadContext context, IRubyObject recv, IRubyObject resource, IRubyObject rlimCur, IRubyObject rlimMax) {
        if (Platform.IS_WINDOWS) {
            throw context.runtime.newNotImplementedError("Process#setrlimit is not implemented on Windows");
        }

        var posix = context.runtime.getPosix();

        if (!posix.isNative()) {
            context.runtime.getWarnings().warn("Process#setrlimit not supported on this platform");
            return context.nil;
        }

        RLimit rlim = posix.getrlimit(0);

        if (rlimMax == context.nil)
            rlimMax = rlimCur;

        rlim.init(rlimitResourceValue(context, rlimCur), rlimitResourceValue(context, rlimMax));

        if (posix.setrlimit(rlimitResourceType(context, resource), rlim) < 0) {
            throw context.runtime.newErrnoFromInt(posix.errno(), "setrlimit");
        }
        return context.nil;
    }

    private static int rlimitResourceValue(ThreadContext context, IRubyObject rval) {
        String name;
        IRubyObject v;

        switch (((CoreObjectType) rval).getNativeClassIndex()) {
            case SYMBOL:
                name = rval.toString();
                break;

            case STRING:
                name = rval.toString();
                break;

            default:
                v = TypeConverter.checkStringType(context.runtime, rval);
                if (!v.isNil()) {
                    rval = v;
                    name = rval.convertToString().toString();
                    break;
                }
        /* fall through */

            case INTEGER:
                return rval.convertToInteger().getIntValue();
        }

        if (RLIM.RLIM_INFINITY.defined()) {
            if (name.equals("INFINITY")) return RLIM.RLIM_INFINITY.intValue();
        }
        if (RLIM.RLIM_SAVED_MAX.defined()) {
            if (name.equals("SAVED_MAX")) return RLIM.RLIM_SAVED_MAX.intValue();
        }
        if (RLIM.RLIM_SAVED_CUR.defined()) {
            if (name.equals("SAVED_CUR")) return RLIM.RLIM_SAVED_CUR.intValue();
        }

        throw argumentError(context, "invalid resource value: " + rval);
    }

    // MRI: rlimit_resource_type
    private static int rlimitResourceType(ThreadContext context, IRubyObject rtype) {
        String name;
        IRubyObject v;
        int r;

        switch (((CoreObjectType) rtype).getNativeClassIndex()) {
            case SYMBOL:
                name = rtype.toString();
                break;

            case STRING:
                name = rtype.toString();
                break;

            default:
                v = TypeConverter.checkStringType(context.runtime, rtype);
                if (!v.isNil()) {
                    rtype = v;
                    name = rtype.toString();
                    break;
                }
        /* fall through */

            case INTEGER:
                return rtype.convertToInteger().getIntValue();
        }

        r = rlimitTypeByHname(name);
        if (r != -1)
            return r;

        throw argumentError(context, "invalid resource name: " + rtype);
    }

    // MRI: rlimit_resource_name2int
    private static int rlimitResourceName2int(String name, int casetype) {
        RLIMIT resource;

        OUTER: while (true) {
            switch (Character.toUpperCase(name.charAt(0))) {
                case 'A':
                    if (RLIMIT.RLIMIT_AS.defined()) {
                        if (name.equalsIgnoreCase("AS")) {
                            resource = RLIMIT.RLIMIT_AS;
                            break OUTER;
                        }
                    }
                    break;

                case 'C':
                    if (RLIMIT.RLIMIT_CORE.defined()) {
                        if (name.equalsIgnoreCase("CORE")) {
                            resource = RLIMIT.RLIMIT_CORE;
                            break OUTER;
                        }
                    }
                    if (RLIMIT.RLIMIT_CPU.defined()) {
                        if (name.equalsIgnoreCase("CPU")) {
                            resource = RLIMIT.RLIMIT_CPU;
                            break OUTER;
                        }
                    }
                    break;

                case 'D':
                    if (RLIMIT.RLIMIT_DATA.defined()) {
                        if (name.equalsIgnoreCase("DATA")) {
                            resource = RLIMIT.RLIMIT_DATA;
                            break OUTER;
                        }
                    }
                    break;

                case 'F':
                    if (RLIMIT.RLIMIT_FSIZE.defined()) {
                        if (name.equalsIgnoreCase("FSIZE")) {
                            resource = RLIMIT.RLIMIT_FSIZE;
                            break OUTER;
                        }
                    }
                    break;

                case 'M':
                    if (RLIMIT.RLIMIT_MEMLOCK.defined()) {
                        if (name.equalsIgnoreCase("MEMLOCK")) {
                            resource = RLIMIT.RLIMIT_MEMLOCK;
                            break OUTER;
                        }
                    }
                    if (RLIMIT.RLIMIT_MSGQUEUE.defined()) {
                        if (name.equalsIgnoreCase("MSGQUEUE")) {
                            resource = RLIMIT.RLIMIT_MSGQUEUE;
                            break OUTER;
                        }
                    }
                    break;

                case 'N':
                    if (RLIMIT.RLIMIT_NOFILE.defined()) {
                        if (name.equalsIgnoreCase("NOFILE")) {
                            resource = RLIMIT.RLIMIT_NOFILE;
                            break OUTER;
                        }
                    }
                    if (RLIMIT.RLIMIT_NPROC.defined()) {
                        if (name.equalsIgnoreCase("NPROC")) {
                            resource = RLIMIT.RLIMIT_NPROC;
                            break OUTER;
                        }
                    }
                    if (RLIMIT.RLIMIT_NICE.defined()) {
                        if (name.equalsIgnoreCase("NICE")) {
                            resource = RLIMIT.RLIMIT_NICE;
                            break OUTER;
                        }
                    }
                    break;

                case 'R':
                    if (RLIMIT.RLIMIT_RSS.defined()) {
                        if (name.equalsIgnoreCase("RSS")) {
                            resource = RLIMIT.RLIMIT_RSS;
                            break OUTER;
                        }
                    }
                    if (RLIMIT.RLIMIT_RTPRIO.defined()) {
                        if (name.equalsIgnoreCase("RTPRIO")) {
                            resource = RLIMIT.RLIMIT_RTPRIO;
                            break OUTER;
                        }
                    }
                    if (RLIMIT.RLIMIT_RTTIME.defined()) {
                        if (name.equalsIgnoreCase("RTTIME")) {
                            resource = RLIMIT.RLIMIT_RTTIME;
                            break OUTER;
                        }
                    }
                    break;

                case 'S':
                    if (RLIMIT.RLIMIT_STACK.defined()) {
                        if (name.equalsIgnoreCase("STACK")) {
                            resource = RLIMIT.RLIMIT_STACK;
                            break OUTER;
                        }
                    }
                    // Not provided by jnr-constants
//                    if (RLIMIT.RLIMIT_SBSIZE.defined()) {
//                    if (name.equalsIgnoreCase("SBSIZE") { resource = RLIMIT.RLIMIT_SBSIZE; break OUTER; }
//                    }
                    if (RLIMIT.RLIMIT_SIGPENDING.defined()) {
                        if (name.equalsIgnoreCase("SIGPENDING")) {
                            resource = RLIMIT.RLIMIT_SIGPENDING;
                            break OUTER;
                        }
                    }
                    break;
            }
            return -1;
        }

        switch (casetype) {
            case 0:
                if (!name.equals(name.toUpperCase())) return -1;
            break;

            case 1:
                if (!name.equals(name.toLowerCase())) return -1;
            break;

            default:
                throw new RuntimeException("unexpected casetype");
        }

        return resource.intValue();
    }

    // MRI: rlimit_type_by_hname
    private static int rlimitTypeByHname(String name) {
        return rlimitResourceName2int(name, 0);
    }

    @Deprecated
    public static IRubyObject getpgrp(IRubyObject recv) {
        return getpgrp(recv.getRuntime());
    }
    @JRubyMethod(name = "getpgrp", module = true, visibility = PRIVATE)
    public static IRubyObject getpgrp(ThreadContext context, IRubyObject recv) {
        return asFixnum(context, context.runtime.getPosix().getpgrp());
    }

    @Deprecated
    public static IRubyObject getpgrp(Ruby runtime) {
        return asFixnum(runtime.getCurrentContext(), runtime.getPosix().getpgrp());
    }

    @JRubyMethod(name = "groups=", module = true, visibility = PRIVATE)
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

    @JRubyMethod(module = true)
    public static IRubyObject warmup(ThreadContext context, IRubyObject recv) {
        // By the time we can call this method core will be bootstrapped.  Unless we want to differentiate
        // a better point for "optimizations" to be possible we can just assume we are good to go.
        return context.tru;
    }

    public static IRubyObject waitpid(Ruby runtime, IRubyObject[] args) {
        long pid = -1;
        int flags = 0;
        if (args.length > 0) pid = args[0].convertToInteger().getLongValue();
        if (args.length > 1) flags = (int)args[1].convertToInteger().getLongValue();

        var context = runtime.getCurrentContext();

        pid = waitpid(runtime, pid, flags);

        checkErrno(context, pid, ECHILD);

        return pid == 0 ? context.nil : asFixnum(context, pid);
    }

    static IRubyObject waitpidStatus(ThreadContext context, long pid, int flags) {
        Ruby runtime = context.runtime;
        int[] status = new int[1];
        POSIX posix = runtime.getPosix();

        posix.errno(0);

        int res = pthreadKillable(context, ctx -> posix.waitpid(pid, status, flags));

        return RubyProcess.RubyStatus.newProcessStatus(runtime, status[0], res);
    }

    @Deprecated
    public static long waitpid(Ruby runtime, long pid, int flags) {
        return waitpid(runtime.getCurrentContext(), pid, flags);
    }

    // MRI: rb_waitpid
    public static long waitpid(ThreadContext context, long pid, int flags) {
        var posix = context.runtime.getPosix();

        posix.errno(0);

        int[] status = new int[1];
        int res = pthreadKillable(context, ctx -> posix.waitpid(pid, status, flags));

        context.setLastExitStatus(res > 0 ?
                RubyProcess.RubyStatus.newProcessStatus(context.runtime, status[0], res) : context.nil);

        return res;
    }

    private static final MethodHandle NATIVE_THREAD_SIGNAL;
    private static final MethodHandle NATIVE_THREAD_CURRENT;
    private static final MethodHandles.Lookup LOOKUP = MethodHandles.lookup();

    static {
        MethodHandle signalHandle = null;
        MethodHandle currentHandle = null;

        try {
            // NativeThread is not public on Windows builds of Open JDK
            Class nativeThread = Class.forName("sun.nio.ch.NativeThread");

            Method signal = nativeThread.getDeclaredMethod("signal", long.class);
            Method current = nativeThread.getDeclaredMethod("current");

            signalHandle = JavaUtil.getHandleSafe(signal, RubyProcess.class, LOOKUP);
            currentHandle = JavaUtil.getHandleSafe(current, RubyProcess.class, LOOKUP);
        } catch (NoSuchMethodException | ClassNotFoundException e) {
            // ignore and leave it null
        }

        NATIVE_THREAD_SIGNAL = signalHandle;
        NATIVE_THREAD_CURRENT = currentHandle;

    }

    private static int pthreadKillable(ThreadContext context, ToIntFunction<ThreadContext> blockingCall) {
        if (Platform.IS_WINDOWS
                || !Options.NATIVE_PTHREAD_KILL.load()
                || NATIVE_THREAD_SIGNAL == null
                || NATIVE_THREAD_CURRENT == null) {
            // Can't use pthread_kill on Windows
            return blockingCall.applyAsInt(context);
        }

        do try {
            final long threadID = (long) NATIVE_THREAD_CURRENT.invokeExact();

            return context.getThread().executeTaskBlocking(context, blockingCall, new RubyThread.Task<ToIntFunction<ThreadContext>, Integer>() {

                @Override
                public Integer run(ThreadContext context, ToIntFunction<ThreadContext> blockingCall) {
                    return blockingCall.applyAsInt(context);
                }

                @Override
                public void wakeup(RubyThread thread, ToIntFunction<ThreadContext> blockingCall) {
                    try {
                        NATIVE_THREAD_SIGNAL.invokeExact(threadID);
                    } catch (Throwable t) {
                        throwException(t);
                    }
                }
            });
        } catch (InterruptedException ie) {
            context.pollThreadEvents();
            // try again
        } catch (Throwable t) {
            throwException(t);
        } while (true);
    }

    private interface NonNativeErrno {
        public int handle(Ruby runtime, int result);
    }

    private static final NonNativeErrno ECHILD = new NonNativeErrno() {
        @Override
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
        POSIX posix = runtime.getPosix();
        ThreadContext context = runtime.getCurrentContext();

        posix.errno(0);

        int pid = pthreadKillable(context, ctx -> posix.wait(status));

        checkErrno(context, pid, ECHILD);

        context.setLastExitStatus(RubyProcess.RubyStatus.newProcessStatus(runtime, status[0], pid));
        return asFixnum(context, pid);
    }


    @Deprecated
    public static IRubyObject waitall(IRubyObject recv) {
        return waitall(recv.getRuntime().getCurrentContext());
    }
    @JRubyMethod(name = "waitall", module = true, visibility = PRIVATE)
    public static IRubyObject waitall(ThreadContext context, IRubyObject recv) {
        return waitall(context);
    }

    /**
     * @param runtime
     * @return ""
     * @deprecated Use {@link RubyProcess#waitall(ThreadContext)}
     */
    @Deprecated(since = "10.0", forRemoval = true)
    public static IRubyObject waitall(Ruby runtime) {
        return waitall(runtime.getCurrentContext());
    }

    public static IRubyObject waitall(ThreadContext context) {
        POSIX posix = context.runtime.getPosix();
        var results = newArray(context);
        int[] status = new int[1];
        int result = pthreadKillable(context, ctx -> posix.wait(status));

        while (result != -1) {
            results.append(context, newArray(context, asFixnum(context, result),
                    RubyProcess.RubyStatus.newProcessStatus(context.runtime, status[0], result)));

            result = pthreadKillable(context, ctx -> posix.wait(status));
        }

        return results;
    }

    @Deprecated
    public static IRubyObject setsid(IRubyObject recv) {
        return setsid(recv.getRuntime());
    }
    @JRubyMethod(name = "setsid", module = true, visibility = PRIVATE)
    public static IRubyObject setsid(ThreadContext context, IRubyObject recv) {
        return asFixnum(context, checkErrno(context, context.runtime.getPosix().setsid()));
    }

    @Deprecated
    public static IRubyObject setsid(Ruby runtime) {
        return setsid(runtime.getCurrentContext(), null);
    }

    @Deprecated
    public static IRubyObject setpgrp(IRubyObject recv) {
        return setpgrp(recv.getRuntime());
    }
    @JRubyMethod(name = "setpgrp", module = true, visibility = PRIVATE)
    public static IRubyObject setpgrp(ThreadContext context, IRubyObject recv) {
        return asFixnum(context, checkErrno(context, context.runtime.getPosix().setpgid(0, 0)));
    }

    @Deprecated
    public static IRubyObject setpgrp(Ruby runtime) {
        return setpgrp(runtime.getCurrentContext(), null);
    }

    @Deprecated
    public static IRubyObject egid_set(IRubyObject recv, IRubyObject arg) {
        return egid_set(recv.getRuntime().getCurrentContext(), arg);
    }
    @JRubyMethod(name = "egid=", module = true, visibility = PRIVATE)
    public static IRubyObject egid_set(ThreadContext context, IRubyObject recv, IRubyObject arg) {
        return egid_set(context, arg);
    }

    /**
     * @param runtime
     * @param arg
     * @return ""
     * @deprecated Use {@link RubyProcess#egid_set(ThreadContext, IRubyObject)}
     */
    @Deprecated(since = "10.0", forRemoval = true)
    public static IRubyObject egid_set(Ruby runtime, IRubyObject arg) {
        return egid_set(runtime.getCurrentContext(), arg);
    }

    public static IRubyObject egid_set(ThreadContext context, IRubyObject arg) {
        int gid;
        if (arg instanceof RubyInteger || arg.checkStringType().isNil()) {
            gid = (int)arg.convertToInteger().getLongValue();
        } else {
            Group group = context.runtime.getPosix().getgrnam(arg.asJavaString());
            if (group == null) throw argumentError(context, "can't find group for " + arg.inspect());
            gid = (int)group.getGID();
        }
        checkErrno(context, context.runtime.getPosix().setegid(gid));
        return asFixnum(context, 0);
    }

    @Deprecated
    public static IRubyObject euid(IRubyObject recv) {
        return euid(recv.getRuntime());
    }
    @JRubyMethod(name = "euid", module = true, visibility = PRIVATE)
    public static IRubyObject euid(ThreadContext context, IRubyObject recv) {
        return asFixnum(context, checkErrno(context, context.runtime.getPosix().geteuid()));
    }

    @Deprecated
    public static IRubyObject euid(Ruby runtime) {
        return euid(runtime.getCurrentContext(), null);
    }

    @Deprecated
    public static IRubyObject uid_set(IRubyObject recv, IRubyObject arg) {
        return uid_set(recv.getRuntime().getCurrentContext(), null, arg);
    }
    @JRubyMethod(name = "uid=", module = true, visibility = PRIVATE)
    public static IRubyObject uid_set(ThreadContext context, IRubyObject recv, IRubyObject arg) {
        checkErrno(context, context.runtime.getPosix().setuid((int)arg.convertToInteger().getLongValue()));
        return RubyFixnum.zero(context.runtime);
    }
    @Deprecated
    public static IRubyObject uid_set(Ruby runtime, IRubyObject arg) {
        return uid_set(runtime.getCurrentContext(), null, arg);
    }

    @Deprecated
    public static IRubyObject gid(IRubyObject recv) {
        return gid(recv.getRuntime().getCurrentContext());
    }
    @JRubyMethod(name = "gid", module = true, visibility = PRIVATE)
    public static IRubyObject gid(ThreadContext context, IRubyObject recv) {
        return gid(context);
    }
    public static IRubyObject gid(ThreadContext context) {
        return Platform.IS_WINDOWS ?
                RubyFixnum.zero(context.runtime) :
                asFixnum(context, checkErrno(context, context.runtime.getPosix().getgid()));
    }

    @Deprecated
    public static IRubyObject gid(Ruby runtime) {
        return gid(runtime.getCurrentContext());
    }

    @JRubyMethod(name = "maxgroups", module = true, visibility = PRIVATE)
    public static IRubyObject maxgroups(IRubyObject recv) {
        throw recv.getRuntime().newNotImplementedError("Process#maxgroups not yet implemented");
    }

    @Deprecated
    public static IRubyObject getpriority(IRubyObject recv, IRubyObject arg1, IRubyObject arg2) {
        return getpriority(recv.getRuntime(), arg1, arg2);
    }
    @JRubyMethod(name = "getpriority", module = true, visibility = PRIVATE)
    public static IRubyObject getpriority(ThreadContext context, IRubyObject recv, IRubyObject arg1, IRubyObject arg2) {
        int which = arg1.convertToInteger().getIntValue();
        int who = arg2.convertToInteger().getIntValue();
        int result = checkErrno(context, context.runtime.getPosix().getpriority(which, who));

        return asFixnum(context, result);
    }

    @Deprecated
    public static IRubyObject getpriority(Ruby runtime, IRubyObject arg1, IRubyObject arg2) {
        return getpriority(runtime.getCurrentContext(), null, arg1, arg2);
    }

    @Deprecated
    public static IRubyObject uid(IRubyObject recv) {
        return uid(recv.getRuntime());
    }
    @JRubyMethod(name = "uid", module = true, visibility = PRIVATE)
    public static IRubyObject uid(ThreadContext context, IRubyObject recv) {
        return asFixnum(context, checkErrno(context, context.runtime.getPosix().getuid()));
    }
    @Deprecated
    public static IRubyObject uid(Ruby runtime) {
        return uid(runtime.getCurrentContext(), null);
    }

    @JRubyMethod(name = "waitpid2", rest = true, module = true, visibility = PRIVATE)
    public static IRubyObject waitpid2(ThreadContext context, IRubyObject recv, IRubyObject[] args) {
        IRubyObject pid = waitpid(context, recv, args);

        return pid.isNil() ? context.nil : newArray(context, pid, context.getLastExitStatus());
    }

    public static IRubyObject waitpid2(Ruby runtime, IRubyObject[] args) {
        return waitpid2(runtime.getCurrentContext(), runtime.getProcess(), args);
    }

    @JRubyMethod(name = "initgroups", module = true, visibility = PRIVATE)
    public static IRubyObject initgroups(IRubyObject recv, IRubyObject arg1, IRubyObject arg2) {
        throw recv.getRuntime().newNotImplementedError("Process#initgroups not yet implemented");
    }

    @JRubyMethod(name = "maxgroups=", module = true, visibility = PRIVATE)
    public static IRubyObject maxgroups_set(IRubyObject recv, IRubyObject arg) {
        throw recv.getRuntime().newNotImplementedError("Process#maxgroups_set not yet implemented");
    }

    @Deprecated
    public static IRubyObject ppid(IRubyObject recv) {
        return ppid(recv.getRuntime());
    }
    @JRubyMethod(name = "ppid", module = true, visibility = PRIVATE)
    public static IRubyObject ppid(ThreadContext context, IRubyObject recv) {
        int result = checkErrno(context, context.runtime.getPosix().getppid());

        return asFixnum(context, result);
    }
    public static IRubyObject ppid(Ruby runtime) {
        return ppid(runtime.getCurrentContext(), null);
    }

    @Deprecated
    public static IRubyObject gid_set(IRubyObject recv, IRubyObject arg) {
        return gid_set(recv.getRuntime(), arg);
    }
    @JRubyMethod(name = "gid=", module = true, visibility = PRIVATE)
    public static IRubyObject gid_set(ThreadContext context, IRubyObject recv, IRubyObject arg) {
        int result = checkErrno(context, context.runtime.getPosix().setgid(arg.convertToInteger().getIntValue()));

        return asFixnum(context, result);

    }
    @Deprecated
    public static IRubyObject gid_set(Ruby runtime, IRubyObject arg) {
        return gid_set(runtime.getCurrentContext(), null, arg);
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
        return euid_set(recv.getRuntime().getCurrentContext(), arg);
    }
    @JRubyMethod(name = "euid=", module = true, visibility = PRIVATE)
    public static IRubyObject euid_set(ThreadContext context, IRubyObject recv, IRubyObject arg) {
        return euid_set(context, arg);
    }

    /**
     * @param runtime
     * @param arg
     * @return ""
     * @deprecated Use {@link RubyProcess#egid_set(ThreadContext, IRubyObject)} instead.
     */
    @Deprecated(since = "10.0", forRemoval = true)
    public static IRubyObject euid_set(Ruby runtime, IRubyObject arg) {
        return euid_set(runtime.getCurrentContext(), arg);
    }


    public static IRubyObject euid_set(ThreadContext context, IRubyObject arg) {
        int uid;
        if (arg instanceof RubyInteger || arg.checkStringType().isNil()) {
            uid = arg.convertToInteger().getIntValue();
        } else {
            Passwd password = context.runtime.getPosix().getpwnam(arg.asJavaString());
            if (password == null) {
                throw argumentError(context, "can't find user for " + arg.inspect());
            }
            uid = (int)password.getUID();
        }
        checkErrno(context, context.runtime.getPosix().seteuid(uid));
        return asFixnum(context, 0);
    }

    @Deprecated
    public static IRubyObject setpriority(IRubyObject recv, IRubyObject arg1, IRubyObject arg2, IRubyObject arg3) {
        return setpriority(recv.getRuntime(), arg1, arg2, arg3);
    }
    @JRubyMethod(name = "setpriority", module = true, visibility = PRIVATE)
    public static IRubyObject setpriority(ThreadContext context, IRubyObject recv, IRubyObject arg1, IRubyObject arg2, IRubyObject arg3) {
        int which = arg1.convertToInteger().getIntValue();
        int who = arg2.convertToInteger().getIntValue();
        int prio = arg3.convertToInteger().getIntValue();
        var posix = context.runtime.getPosix();
        posix.errno(0);
        return asFixnum(context, checkErrno(context, posix.setpriority(which, who, prio)));
    }

    @Deprecated
    public static IRubyObject setpriority(Ruby runtime, IRubyObject arg1, IRubyObject arg2, IRubyObject arg3) {
        return setpriority(runtime.getCurrentContext(), null, arg1, arg2, arg3);
    }

    @Deprecated
    public static IRubyObject setpgid(IRubyObject recv, IRubyObject arg1, IRubyObject arg2) {
        return setpgid(recv.getRuntime(), arg1, arg2);
    }
    @JRubyMethod(name = "setpgid", module = true, visibility = PRIVATE)
    public static IRubyObject setpgid(ThreadContext context, IRubyObject recv, IRubyObject arg1, IRubyObject arg2) {
        int pid = arg1.convertToInteger().getIntValue();
        int gid = arg2.convertToInteger().getIntValue();
        return asFixnum(context, checkErrno(context, context.runtime.getPosix().setpgid(pid, gid)));
    }

    @Deprecated
    public static IRubyObject setpgid(Ruby runtime, IRubyObject arg1, IRubyObject arg2) {
        return setpgid(runtime.getCurrentContext(), null, arg1, arg2);
    }

    @Deprecated
    public static IRubyObject getpgid(IRubyObject recv, IRubyObject arg) {
        return getpgid(recv.getRuntime(), arg);
    }
    @JRubyMethod(name = "getpgid", module = true, visibility = PRIVATE)
    public static IRubyObject getpgid(ThreadContext context, IRubyObject recv, IRubyObject arg) {
        int pgid = arg.convertToInteger().getIntValue();
        return asFixnum(context, checkErrno(context, context.runtime.getPosix().getpgid(pgid)));

    }
    @Deprecated
    public static IRubyObject getpgid(Ruby runtime, IRubyObject arg) {
        return getpgid(runtime.getCurrentContext(), null, arg);
    }

    @Deprecated
    public static IRubyObject getrlimit(IRubyObject recv, IRubyObject arg) {
        return getrlimit(recv.getRuntime(), arg);
    }
    @JRubyMethod(name = "getrlimit", module = true, visibility = PRIVATE)
    public static IRubyObject getrlimit(ThreadContext context, IRubyObject recv, IRubyObject arg) {
        return getrlimit(context, arg);
    }

    /**
     * @param runtime
     * @param arg
     * @return ""
     * @deprecated Use {@link RubyProcess#getrlimit(ThreadContext, IRubyObject)}
     */
    @Deprecated(since = "10.0", forRemoval = true)
    public static IRubyObject getrlimit(Ruby runtime, IRubyObject arg) {
        return getrlimit(runtime.getCurrentContext(), arg);
    }

    public static IRubyObject getrlimit(ThreadContext context, IRubyObject arg) {
        if (Platform.IS_WINDOWS) {
            throw context.runtime.newNotImplementedError("Process#getrlimit is not implemented on Windows");
        }

        if (!context.runtime.getPosix().isNative()) {
            context.runtime.getWarnings().warn("Process#getrlimit not supported on this platform");
            RubyFixnum max = asFixnum(context, Long.MAX_VALUE);
            return newArray(context, max, max);
        }

        RLimit rlimit = context.runtime.getPosix().getrlimit(rlimitResourceType(context, arg));

        return newArray(context, asFixnum(context, rlimit.rlimCur()), asFixnum(context, rlimit.rlimMax()));
    }

    @Deprecated
    public static IRubyObject egid(IRubyObject recv) {
        return egid(recv.getRuntime().getCurrentContext(), recv);
    }
    @JRubyMethod(name = "egid", module = true, visibility = PRIVATE)
    public static IRubyObject egid(ThreadContext context, IRubyObject recv) {
        return Platform.IS_WINDOWS ? RubyFixnum.zero(context.runtime) :
                asFixnum(context, checkErrno(context, context.runtime.getPosix().getegid()));
    }

    @Deprecated
    public static IRubyObject egid(Ruby runtime) {
        return egid(runtime.getCurrentContext(), null);
    }

    private static int parseSignalString(ThreadContext context, String value) {
        boolean negative = value.startsWith("-");

        // Gets rid of the - if there is one present.
        if (negative) value = value.substring(1);

        // We need the SIG for sure.
        String signalName = value.startsWith("SIG") ? value.substring(3) : value;
        int signalValue = (int) RubySignal.signm2signo(signalName);

        if (signalValue == 0) throw argumentError(context, "unsupported name '" + signalName + "'");

        return negative ? -signalValue : signalValue;
    }

    @Deprecated
    public static IRubyObject kill(IRubyObject recv, IRubyObject[] args) {
        return kill(recv.getRuntime(), args);
    }
    @JRubyMethod(name = "kill", rest = true, module = true, visibility = PRIVATE)
    public static IRubyObject kill(ThreadContext context, IRubyObject recv, IRubyObject[] args) {
        if (args.length < 2) throw argumentError(context, "wrong number of arguments -- kill(sig, pid...)");

        int signal;
        if (args[0] instanceof RubyFixnum) {
            signal = (int) ((RubyFixnum) args[0]).getLongValue();
        } else if (args[0] instanceof RubySymbol) {
            signal = parseSignalString(context, args[0].toString());
        } else if (args[0] instanceof RubyString) {
            signal = parseSignalString(context, args[0].toString());
        } else {
            signal = parseSignalString(context, args[0].checkStringType().toString());
        }

        boolean processGroupKill = signal < 0;

        if (processGroupKill) {
            if (Platform.IS_WINDOWS) {
                throw context.runtime.newErrnoEINVALError("group signals not implemented in windows");
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
                                throw context.runtime.newErrnoEPERMError("unable to call GetExitCodeProcess " + pid);
                            } else {
                                if(status.intValue() != STILL_ACTIVE) {
                                    throw context.runtime.newErrnoEPERMError("Process exists but is not alive anymore " + pid);
                                }
                            }
                        } finally {
                            kernel32().CloseHandle(ptr);
                        }

                    } else {
                        if (kernel32().GetLastError() == ERROR_INVALID_PARAMETER) {
                            throw context.runtime.newErrnoESRCHError();
                        } else {
                            throw context.runtime.newErrnoEPERMError("Process does not exist " + pid);
                        }
                    }
                } else if (signal == 9) { //SIGKILL
                    jnr.ffi.Pointer ptr = kernel32().OpenProcess(PROCESS_TERMINATE | PROCESS_QUERY_INFORMATION, 0, pid);
                    if(ptr != null && ptr.address() != -1) {
                        try {
                            IntByReference status = new IntByReference(0);
                            if(kernel32().GetExitCodeProcess(ptr, status) == 0) {
                                throw context.runtime.newErrnoEPERMError("unable to call GetExitCodeProcess " + pid); // todo better error messages
                            } else {
                                if (status.intValue() == STILL_ACTIVE) {
                                    if (kernel32().TerminateProcess(ptr, 0) == 0) {
                                        throw context.runtime.newErrnoEPERMError("unable to call TerminateProcess " + pid);
                                    }
                                    // success
                                }
                            }
                        } finally {
                            kernel32().CloseHandle(ptr);
                        }
                    } else {
                        if (kernel32().GetLastError() == ERROR_INVALID_PARAMETER) {
                            throw context.runtime.newErrnoESRCHError();
                        } else {
                            throw context.runtime.newErrnoEPERMError("Process does not exist " + pid);
                        }
                    }
                } else {
                    throw context.runtime.newNotImplementedError("this signal not yet implemented in windows");
                }
            }
        } else {
            POSIX posix = context.runtime.getPosix();
            for (int i = 1; i < args.length; i++) {
                int pid = RubyNumeric.num2int(args[i]);

                // FIXME: It may be possible to killpg on systems which support it.  POSIX library
                // needs to tell whether a particular method works or not
                if (pid == 0) pid = context.runtime.getPosix().getpid();
                checkErrno(context, posix.kill(processGroupKill ? -pid : pid, signal));
            }
        }

        return asFixnum(context, args.length - 1);

    }

    @Deprecated
    public static IRubyObject kill(Ruby runtime, IRubyObject[] args) {
        return kill(runtime.getCurrentContext(), null, args);
    }

    @JRubyMethod(name = "detach", module = true, visibility = PRIVATE)
    public static IRubyObject detach(ThreadContext context, IRubyObject recv, IRubyObject arg) {
        final long pid = arg.convertToInteger().getLongValue();
        Ruby runtime = context.runtime;

        BlockCallback callback = (ctx, args, block) -> {
            // push a dummy frame to avoid AIOOB if an exception fires
            ctx.pushFrame();

            while (waitpid(ctx.runtime, pid, 0) == 0) {}

            return last_status(ctx, recv);
        };

        return RubyThread.startWaiterThread(
                runtime,
                pid,
                CallBlock.newCallClosure(context, recv, Signature.NO_ARGUMENTS, callback));
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
        double utime = 0.0d, stime = 0.0d, cutime = 0.0d, cstime = 0.0d;
        if (tms == null) {
            ThreadMXBean bean = ManagementFactory.getThreadMXBean();
            if(bean.isCurrentThreadCpuTimeSupported()) {
                cutime = utime = bean.getCurrentThreadUserTime();
                cstime = stime = bean.getCurrentThreadCpuTime() - bean.getCurrentThreadUserTime();
            }
        } else {
            utime = (double)tms.utime();
            stime = (double)tms.stime();
            cutime = (double)tms.cutime();
            cstime = (double)tms.cstime();
        }

        long hz = runtime.getPosix().sysconf(Sysconf._SC_CLK_TCK);
        if (hz == -1) {
            hz = 60; //https://github.com/ruby/ruby/blob/trunk/process.c#L6616
        }

        return RubyStruct.newStruct(runtime.getTmsStruct(),
                new IRubyObject[] {
                        runtime.newFloat(utime / (double) hz),
                        runtime.newFloat(stime / (double) hz),
                        runtime.newFloat(cutime / (double) hz),
                        runtime.newFloat(cstime / (double) hz)
                },
                Block.NULL_BLOCK);
    }

    @JRubyMethod(module = true, visibility = PRIVATE)
    public static IRubyObject clock_gettime(ThreadContext context, IRubyObject self, IRubyObject _clock_id) {
        return makeClockResult(context, getTimeForClock(context, _clock_id), CLOCK_UNIT_FLOAT_SECOND);
    }

    @JRubyMethod(module = true, visibility = PRIVATE)
    public static IRubyObject clock_gettime(ThreadContext context, IRubyObject self, IRubyObject _clock_id, IRubyObject _unit) {
        if (!(_unit instanceof RubySymbol) && !_unit.isNil()) throw argumentError(context, "unexpected unit: " + _unit);

        return makeClockResult(context, getTimeForClock(context, _clock_id), _unit.toString());
    }

    @JRubyMethod(rest = true, meta = true)
    public static IRubyObject exec(ThreadContext context, IRubyObject self, IRubyObject[] args) {
        return RubyKernel.exec(context, self, args);
    }

    /**
     * Get the time in nanoseconds corresponding to the requested clock.
     */
    private static long getTimeForClock(ThreadContext context, IRubyObject _clock_id) throws RaiseException {
        long nanos;

        if (_clock_id instanceof RubySymbol) {
            RubySymbol clock_id = (RubySymbol) _clock_id;
            if (clock_id.idString().equals(CLOCK_MONOTONIC)) {
                nanos = System.nanoTime();
            } else if (clock_id.idString().equals(CLOCK_REALTIME)) {
                POSIX posix = context.runtime.getPosix();
                if (posix.isNative()) {
                    Timeval tv = posix.allocateTimeval();
                    posix.gettimeofday(tv);
                    nanos = tv.sec() * 1_000_000_000 + tv.usec() * 1000;
                } else {
                    nanos = System.currentTimeMillis() * 1000000;
                }
            } else {
                throw context.runtime.newErrnoEINVALError("clock_gettime");
            }
        } else {
            // TODO: probably need real clock_id values to do this right.
            throw context.runtime.newErrnoEINVALError("clock_gettime");
        }
        return nanos;
    }

    /**
     * Get the time resolution in nanoseconds corresponding to the requested clock.
     */
    private static long getResolutionForClock(ThreadContext context, IRubyObject _clock_id) throws RaiseException {
        long nanos;

        if (_clock_id instanceof RubySymbol) {
            RubySymbol clock_id = (RubySymbol) _clock_id;
            if (clock_id.idString().equals(CLOCK_MONOTONIC)) {
                nanos = 1;
            } else if (clock_id.idString().equals(CLOCK_REALTIME)) {
                nanos = 1000000;
            } else {
                throw context.runtime.newErrnoEINVALError("clock_gettime");
            }
        } else {
            // TODO: probably need real clock_id values to do this right.
            throw context.runtime.newErrnoEINVALError("clock_gettime");
        }
        return nanos;
    }

    private static IRubyObject makeClockResult(ThreadContext context, long nanos, String unit) {
        if (unit.equals(CLOCK_UNIT_NANOSECOND)) {
            return asFixnum(context, nanos);
        } else if (unit.equals(CLOCK_UNIT_MICROSECOND)) {
            return asFixnum(context, nanos / 1000);
        } else if (unit.equals(CLOCK_UNIT_MILLISECOND)) {
            return asFixnum(context, nanos / 1000000);
        } else if (unit.equals(CLOCK_UNIT_SECOND)) {
            return asFixnum(context, nanos / 1000000000);
        } else if (unit.equals(CLOCK_UNIT_FLOAT_MICROSECOND)) {
            return asFloat(context, nanos / 1000.0);
        } else if (unit.equals(CLOCK_UNIT_FLOAT_MILLISECOND)) {
            return asFloat(context, nanos / 1000000.0);
        } else if (unit.equals(CLOCK_UNIT_FLOAT_SECOND) || unit.equals("")) {
            return asFloat(context, nanos / 1000000000.0);
        }

        throw argumentError(context, "unexpected unit: " + unit);
    }

    // this is only in 2.1. See https://bugs.ruby-lang.org/issues/8658
    @JRubyMethod(module = true, visibility = PRIVATE)
    public static IRubyObject clock_getres(ThreadContext context, IRubyObject self, IRubyObject _clock_id) {
        return makeClockResolutionResult(context, getResolutionForClock(context, _clock_id), CLOCK_UNIT_FLOAT_SECOND);
    }

    // this is only in 2.1. See https://bugs.ruby-lang.org/issues/8658
    @JRubyMethod(module = true, visibility = PRIVATE)
    public static IRubyObject clock_getres(ThreadContext context, IRubyObject self, IRubyObject _clock_id, IRubyObject _unit) {
        if (!(_unit instanceof RubySymbol) && !_unit.isNil()) throw argumentError(context, "unexpected unit: " + _unit);

        return makeClockResolutionResult(context, getResolutionForClock(context, _clock_id), _unit.toString());
    }

    private static IRubyObject makeClockResolutionResult(ThreadContext context, long nanos, String unit) {
        return unit.equals(CLOCK_UNIT_HERTZ) ?
                asFloat(context, 1000000000.0 / nanos) : makeClockResult(context, nanos, unit);
    }

    @Deprecated
    public static IRubyObject pid(IRubyObject recv) {
        return pid(recv.getRuntime().getCurrentContext());
    }
    @JRubyMethod(name = "pid", module = true, visibility = PRIVATE)
    public static IRubyObject pid(ThreadContext context, IRubyObject recv) {
        return pid(context);
    }
    @Deprecated
    public static IRubyObject pid(Ruby runtime) {
        return pid(runtime.getCurrentContext());
    }
    public static IRubyObject pid(ThreadContext context) {
        return asFixnum(context, context.runtime.getPosix().getpid());
    }

    @JRubyMethod(module = true, visibility = PRIVATE, notImplemented = true)
    public static IRubyObject _fork(ThreadContext context, IRubyObject recv, Block block) {
        throw context.runtime.newNotImplementedError("fork is not available on this platform");
    }

    @Deprecated
    public static IRubyObject fork19(ThreadContext context, IRubyObject recv, Block block) {
        return fork(context, recv, block);
    }

    @JRubyMethod(name = "fork", module = true, visibility = PRIVATE, notImplemented = true)
    public static IRubyObject fork(ThreadContext context, IRubyObject recv, Block block) {
        return RubyKernel.fork(context, recv, block);
    }

    @JRubyMethod(rest = true, module = true, visibility = PRIVATE)
    public static RubyFixnum spawn(ThreadContext context, IRubyObject recv, IRubyObject[] args) {
        Ruby runtime = context.runtime;

        if (PopenExecutor.nativePopenAvailable(runtime)) {
            return PopenExecutor.spawn(context, args);
        }

        return RubyFixnum.newFixnum(runtime, ShellLauncher.runExternalWithoutWait(runtime, args));
    }

    @JRubyMethod(name = "exit", optional = 1, checkArity = false, module = true, visibility = PRIVATE)
    public static IRubyObject exit(IRubyObject recv, IRubyObject[] args) {
        return RubyKernel.exit(recv.getRuntime().getCurrentContext(), recv, args);
    }

    @JRubyMethod(name = "setproctitle", module = true, visibility = PRIVATE)
    public static IRubyObject setproctitle(IRubyObject recv, IRubyObject name) {
        // Not possible for us to implement on most platforms, so we just noop.
        name.convertToString();

        return name;
    }

    // This isn't quite right, and should probably work with a new Process + pid aggregate object
    public static void syswait(Ruby runtime, int pid) {
        int[] status = {0};
        runtime.getPosix().waitpid(pid, status, 0);
    }

    private static final NonNativeErrno IGNORE = new NonNativeErrno() {
        @Override
        public int handle(Ruby runtime, int result) {return result;}
    };

    private static int checkErrno(ThreadContext context, int result) {
        return (int) checkErrno(context, result, IGNORE);
    }

    private static long checkErrno(ThreadContext context, long result, NonNativeErrno nonNative) {
        if (result == -1) {
            if (context.runtime.getPosix().isNative()) {
                raiseErrnoIfSet(context.runtime, nonNative);
            } else {
                nonNative.handle(context.runtime, (int) result);
            }
        }
        return result;
    }

    private static void raiseErrnoIfSet(Ruby runtime, NonNativeErrno nonNative) {
        if (runtime.getPosix().errno() != 0) {
            throw runtime.newErrnoFromInt(runtime.getPosix().errno());
        }
    }

    @Deprecated
    public static IRubyObject waitpid2(IRubyObject recv, IRubyObject[] args) {
        return waitpid2(recv.getRuntime(), args);
    }
}
