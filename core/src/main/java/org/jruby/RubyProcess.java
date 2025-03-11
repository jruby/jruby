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
import jnr.posix.POSIX;
import jnr.posix.Passwd;
import jnr.posix.RLimit;
import jnr.posix.Times;
import jnr.posix.Timeval;
import org.jruby.anno.JRubyClass;
import org.jruby.anno.JRubyMethod;
import org.jruby.anno.JRubyModule;
import org.jruby.api.Convert;
import org.jruby.exceptions.RaiseException;
import org.jruby.javasupport.JavaUtil;
import org.jruby.platform.Platform;
import org.jruby.runtime.Arity;
import org.jruby.runtime.Block;
import org.jruby.runtime.BlockCallback;
import org.jruby.runtime.CallBlock;
import org.jruby.runtime.ObjectMarshal;
import org.jruby.runtime.Signature;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.builtin.Variable;
import org.jruby.runtime.component.VariableEntry;
import org.jruby.runtime.invokedynamic.MethodNames;
import org.jruby.runtime.marshal.CoreObjectType;
import org.jruby.runtime.marshal.Dumper;
import org.jruby.runtime.marshal.MarshalLoader;
import org.jruby.util.ShellLauncher;
import org.jruby.util.TypeConverter;
import org.jruby.util.cli.Options;
import org.jruby.util.io.PopenExecutor;
import org.jruby.util.io.PosixShim;
import org.jruby.util.io.RubyInputStream;
import org.jruby.util.io.RubyOutputStream;

import java.io.IOException;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.lang.reflect.Method;
import java.util.List;
import java.util.function.ToIntFunction;

import static org.jruby.api.Convert.asBoolean;
import static org.jruby.api.Convert.asFixnum;
import static org.jruby.api.Convert.asFloat;
import static org.jruby.api.Convert.asSymbol;
import static org.jruby.api.Convert.toInt;
import static org.jruby.api.Convert.toLong;
import static org.jruby.api.Create.newArray;
import static org.jruby.api.Create.newString;
import static org.jruby.api.Create.newStruct;
import static org.jruby.api.Define.defineModule;
import static org.jruby.api.Error.argumentError;
import static org.jruby.api.Error.notImplementedError;
import static org.jruby.api.Error.rangeError;
import static org.jruby.api.Warn.warn;
import static org.jruby.runtime.Helpers.invokedynamic;
import static org.jruby.runtime.Helpers.nullToNil;
import static org.jruby.runtime.Helpers.throwException;
import static org.jruby.runtime.Visibility.PRIVATE;
import static org.jruby.util.WindowsFFI.Kernel32.ERROR_INVALID_PARAMETER;
import static org.jruby.util.WindowsFFI.Kernel32.PROCESS_QUERY_INFORMATION;
import static org.jruby.util.WindowsFFI.Kernel32.PROCESS_TERMINATE;
import static org.jruby.util.WindowsFFI.Kernel32.STILL_ACTIVE;
import static org.jruby.util.WindowsFFI.kernel32;

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
            var singleton = Process.singletonClass(context);
            // mark rlimit methods as not implemented and skip defining the constants (GH-6491)
            singleton.retrieveMethod("getrlimit").setNotImplemented(true);
            singleton.retrieveMethod("setrlimit").setNotImplemented(true);
        } else {
            Process.defineConstantsFrom(context, jnr.constants.platform.RLIM.class);
            for (RLIMIT r : RLIMIT.values()) {
                if (!r.defined()) continue;
                Process.defineConstant(context, r.name(), asFixnum(context, r.intValue()));
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

        Process.defineConstant(context, "Tms", tmsStruct);
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
        @Deprecated(since = "10.0", forRemoval = true)
        @SuppressWarnings("removal")
        public void marshalTo(Ruby runtime, Object obj, RubyClass type,
                              org.jruby.runtime.marshal.MarshalStream marshalStream) throws IOException {
            RubyStatus status = (RubyStatus) obj;
            var context = runtime.getCurrentContext();

            marshalStream.registerLinkTarget(context, status);
            List<Variable<Object>> attrs = status.getMarshalVariableList();

            attrs.add(new VariableEntry("status", asFixnum(context, status.status)));
            attrs.add(new VariableEntry("pid", asFixnum(context, status.pid)));

            marshalStream.dumpVariables(attrs);
        }

        @Override
        public void marshalTo(ThreadContext context, RubyOutputStream out, Object obj, RubyClass type,
                              Dumper marshalStream) {
            RubyStatus status = (RubyStatus) obj;

            marshalStream.registerLinkTarget(status);

            marshalStream.dumpVariables(context, out, status, 2, (marshal, c, o, v, receiver) -> {
                // TODO: marshal these values directly
                receiver.receive(marshal, c, o, "status", asFixnum(c, v.status));
                receiver.receive(marshal, c, o, "pid", asFixnum(c, v.pid));
            });
        }

        @Override
        @Deprecated(since = "10.0", forRemoval = true)
        @SuppressWarnings("removal")
        public Object unmarshalFrom(Ruby runtime, RubyClass type, org.jruby.runtime.marshal.UnmarshalStream input) throws IOException {
            var context = runtime.getCurrentContext();
            RubyStatus status = (RubyStatus) input.entry(type.allocate(context));

            input.ivar(null, status, null);

            var pstatus = (RubyFixnum) status.removeInternalVariable("status");
            var pid = (RubyFixnum) status.removeInternalVariable("pid");

            status.status = pstatus.getValue();
            status.pid = pid.getValue();

            return status;
        }

        @Override
        public Object unmarshalFrom(ThreadContext context, RubyInputStream in, RubyClass type, MarshalLoader input) {
            RubyStatus status = (RubyStatus) input.entry(type.allocate(context));

            input.ivar(context, in, null, status, null);

            var pstatus = (RubyFixnum) status.removeInternalVariable("status");
            var pid = (RubyFixnum) status.removeInternalVariable("pid");

            status.status = pstatus.getValue();
            status.pid = pid.getValue();

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

            long pid = argc > 0 ? toInt(context, args[0]) : -1;
            int flags = argc > 1 ? toInt(context, args[1]) : 0;

            return waitpidStatus(context, pid, flags);
            //checkErrno(runtime, pid, ECHILD);
        }

        @JRubyMethod(name = "&")
        public IRubyObject op_and(ThreadContext context, IRubyObject arg) {
            long mask = toInt(context, arg);

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
            deprecateAndSuggest(context, "Process::Status#&", "3.5", message);

            return asFixnum(context, status & mask);
        }

        private static void deprecateAndSuggest(ThreadContext context, String method, String version, String suggest) {
            context.runtime.getWarnings().warnDeprecatedForRemovalAlternate(method, version, suggest);
        }

        @JRubyMethod(name = "stopped?")
        public IRubyObject stopped_p(ThreadContext context) {
            return asBoolean(context, PosixShim.WAIT_MACROS.WIFSTOPPED(status));
        }

        @Deprecated
        public IRubyObject stopped_p() {
            return stopped_p(getCurrentContext());
        }

        @JRubyMethod(name = "signaled?")
        public IRubyObject signaled(ThreadContext context) {
            return asBoolean(context, PosixShim.WAIT_MACROS.WIFSIGNALED(status));
        }

        @Deprecated
        public IRubyObject signaled() {
            return signaled(getCurrentContext());
        }

        @JRubyMethod(name = "exited?")
        public IRubyObject exited(ThreadContext context) {
            return asBoolean(context, PosixShim.WAIT_MACROS.WIFEXITED(status));
        }

        @Deprecated(since = "10.0")
        public IRubyObject exited() {
            return exited(getCurrentContext());
        }

        @Deprecated(since = "10.0")
        public IRubyObject stopsig() {
            return stopsig(getCurrentContext());
        }

        @JRubyMethod
        public IRubyObject stopsig(ThreadContext context) {
            return PosixShim.WAIT_MACROS.WIFSTOPPED(status) ?
                    asFixnum(context, PosixShim.WAIT_MACROS.WSTOPSIG(status)) : context.nil;
        }

        @Deprecated(since = "10.0")
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
            long places = toInt(context, other);

            if (places < 0) throw argumentError(context, "negative shift value: " + places);
            if (places > Integer.MAX_VALUE) throw rangeError(context, "shift value out of range: " + places);

            String message = switch ((int) places) {
                case 7 -> "Process::Status#coredump?";
                case 8 -> "Process::Status#exitstatus or Process::Status#stopsig";
                default -> "other Process::Status attributes";
            };
            deprecateAndSuggest(context, "Process::Status#>>", "3.5", message);

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
            var className = getMetaClass().getName(context);
            return unitialized() ?
                    newString(context, "#<" + className + ": uninitialized>") :
                    newString(context, pst_message("#<" + className + ": ", pid, status) + ">");
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

        @Deprecated(since = "10.0")
        public IRubyObject coredump_p() {
            return coredump_p(getCurrentContext());
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

        @Deprecated(since = "10.0")
        public IRubyObject to_s(Ruby runtime) {
            return to_s(getCurrentContext());
        }

        @Override
        @JRubyMethod
        public IRubyObject to_s(ThreadContext context) {
            return newString(context, pst_message("", pid, status));
        }

        boolean unitialized() {
            return pid == PROCESS_STATUS_UNINITIALIZED;
        }

        @Deprecated(since = "10.0")
        public IRubyObject inspect(Ruby runtime) {
            return inspect(runtime.getCurrentContext());
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

        @Deprecated
        public IRubyObject to_i() {
            return to_i(getCurrentContext().runtime);
        }

        @Deprecated
        public IRubyObject op_rshift(Ruby runtime, IRubyObject other) {
            var context = getCurrentContext();
            long shiftValue = toLong(context, other);
            return asFixnum(context, status >> shiftValue);
        }

        @Deprecated
        public IRubyObject op_and(IRubyObject arg) {
            return op_and(getCurrentContext(), arg);
        }
    }

    @JRubyModule(name="Process::UID")
    public static class UserID {
        @Deprecated(since = "10.0")
        public static IRubyObject change_privilege(IRubyObject self, IRubyObject arg) {
            return change_privilege(((RubyBasicObject) self).getCurrentContext(), self, arg);
        }

        @JRubyMethod(name = "change_privilege", module = true, visibility = PRIVATE)
        public static IRubyObject change_privilege(ThreadContext context, IRubyObject self, IRubyObject arg) {
            throw notImplementedError(context, "Process::UID::change_privilege not implemented yet");
        }

        @Deprecated
        public static IRubyObject eid(IRubyObject self) {
            return euid(((RubyBasicObject) self).getCurrentContext(), null);
        }
        @JRubyMethod(name = "eid", module = true, visibility = PRIVATE)
        public static IRubyObject eid(ThreadContext context, IRubyObject self) {
            return euid(context, self);
        }

        @Deprecated
        public static IRubyObject eid(IRubyObject self, IRubyObject arg) {
            return eid(((RubyBasicObject) self).getCurrentContext(), arg);
        }
        @JRubyMethod(name = "eid=", module = true, visibility = PRIVATE)
        public static IRubyObject eid(ThreadContext context, IRubyObject self, IRubyObject arg) {
            return euid_set(context, self, arg);
        }
        public static IRubyObject eid(Ruby runtime, IRubyObject arg) {
            return euid_set(runtime, arg);
        }

        @Deprecated(since = "10.0")
        public static IRubyObject grant_privilege(IRubyObject self, IRubyObject arg) {
            return grant_privilege(((RubyBasicObject) self).getCurrentContext(), self, arg);
        }

        @JRubyMethod(name = "grant_privilege", module = true, visibility = PRIVATE)
        public static IRubyObject grant_privilege(ThreadContext context, IRubyObject self, IRubyObject arg) {
            throw notImplementedError(context, "Process::UID::grant_privilege not implemented yet");
        }

        @JRubyMethod(name = "re_exchange", module = true, visibility = PRIVATE)
        public static IRubyObject re_exchange(ThreadContext context, IRubyObject self) {
            return switch_rb(context, self, Block.NULL_BLOCK);
        }

        @Deprecated(since = "10.0")
        public static IRubyObject re_exchangeable_p(IRubyObject self) {
            return re_exchangeable_p(((RubyBasicObject) self).getCurrentContext(), self);
        }

        @JRubyMethod(name = "re_exchangeable?", module = true, visibility = PRIVATE)
        public static IRubyObject re_exchangeable_p(ThreadContext context, IRubyObject self) {
            throw notImplementedError(context, "Process::UID::re_exchangeable? not implemented yet");
        }

        @Deprecated
        public static IRubyObject rid(IRubyObject self) {
            return rid(((RubyBasicObject) self).getCurrentContext(), self);
        }
        @JRubyMethod(name = "rid", module = true, visibility = PRIVATE)
        public static IRubyObject rid(ThreadContext context, IRubyObject self) {
            return rid(context.runtime);
        }
        public static IRubyObject rid(Ruby runtime) {
            return uid(runtime);
        }

        @Deprecated(since = "10.0")
        public static IRubyObject sid_available_p(IRubyObject self) {
            return sid_available_p(((RubyBasicObject) self).getCurrentContext(), self);
        }

        @JRubyMethod(name = "sid_available?", module = true, visibility = PRIVATE)
        public static IRubyObject sid_available_p(ThreadContext context, IRubyObject self) {
            throw notImplementedError(context, "Process::UID::sid_available not implemented yet");
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
        @Deprecated(since = "10.0")
        public static IRubyObject change_privilege(IRubyObject self, IRubyObject arg) {
            return change_privilege(((RubyBasicObject) self).getCurrentContext(), self, arg);
        }

        @JRubyMethod(name = "change_privilege", module = true, visibility = PRIVATE)
        public static IRubyObject change_privilege(ThreadContext context, IRubyObject self, IRubyObject arg) {
            throw notImplementedError(context, "Process::GID::change_privilege not implemented yet");
        }

        @Deprecated
        public static IRubyObject eid(IRubyObject self) {
            return eid(((RubyBasicObject) self).getCurrentContext(), self);
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
            return eid(((RubyBasicObject) self).getCurrentContext(), arg);
        }
        @JRubyMethod(name = "eid=", module = true, visibility = PRIVATE)
        public static IRubyObject eid(ThreadContext context, IRubyObject self, IRubyObject arg) {
            return eid(context.runtime, arg);
        }
        public static IRubyObject eid(Ruby runtime, IRubyObject arg) {
            return RubyProcess.egid_set(runtime.getCurrentContext(), arg);
        }

        @Deprecated(since = "10.0")
        public static IRubyObject grant_privilege(IRubyObject self, IRubyObject arg) {
            return grant_privilege(((RubyBasicObject) self).getCurrentContext(), self, arg);
        }

        @JRubyMethod(name = "grant_privilege", module = true, visibility = PRIVATE)
        public static IRubyObject grant_privilege(ThreadContext context, IRubyObject self, IRubyObject arg) {
            throw notImplementedError(context, "Process::GID::grant_privilege not implemented yet");
        }

        @JRubyMethod(name = "re_exchange", module = true, visibility = PRIVATE)
        public static IRubyObject re_exchange(ThreadContext context, IRubyObject self) {
            return switch_rb(context, self, Block.NULL_BLOCK);
        }

        @Deprecated(since = "10.0")
        public static IRubyObject re_exchangeable_p(IRubyObject self) {
            return re_exchangeable_p(((RubyBasicObject) self).getCurrentContext(), self);
        }

        @JRubyMethod(name = "re_exchangeable?", module = true, visibility = PRIVATE)
        public static IRubyObject re_exchangeable_p(ThreadContext context, IRubyObject self) {
            throw notImplementedError(context, "Process::GID::re_exchangeable? not implemented yet");
        }

        @Deprecated
        public static IRubyObject rid(IRubyObject self) {
            return rid(((RubyBasicObject) self).getCurrentContext(), self);
        }
        @JRubyMethod(name = "rid", module = true, visibility = PRIVATE)
        public static IRubyObject rid(ThreadContext context, IRubyObject self) {
            return rid(context.runtime);
        }
        public static IRubyObject rid(Ruby runtime) {
            return gid(runtime);
        }

        @Deprecated(since = "10.0")
        public static IRubyObject sid_available_p(IRubyObject self) {
            return sid_available_p(((RubyBasicObject) self).getCurrentContext(), self);
        }

        @JRubyMethod(name = "sid_available?", module = true, visibility = PRIVATE)
        public static IRubyObject sid_available_p(ThreadContext context, IRubyObject self) {
            throw notImplementedError(context, "Process::GID::sid_available not implemented yet");
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
            return egid(((RubyBasicObject) self).getCurrentContext(), null);
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
            return gid(((RubyBasicObject) self).getCurrentContext());
        }
        @JRubyMethod(name = "getgid", module = true, visibility = PRIVATE)
        public static IRubyObject getgid(ThreadContext context, IRubyObject self) {
            return gid(context, self);
        }

        @Deprecated
        public static IRubyObject getuid(IRubyObject self) {
            return uid(((RubyBasicObject) self).getCurrentContext(), self);
        }
        @JRubyMethod(name = "getuid", module = true, visibility = PRIVATE)
        public static IRubyObject getuid(ThreadContext context, IRubyObject self) {
            return uid(context, self);
        }

        @Deprecated
        public static IRubyObject setegid(IRubyObject recv, IRubyObject arg) {
            return egid_set(((RubyBasicObject) recv).getCurrentContext(), arg);
        }
        @JRubyMethod(name = "setegid", module = true, visibility = PRIVATE)
        public static IRubyObject setegid(ThreadContext context, IRubyObject recv, IRubyObject arg) {
            return egid_set(context, recv, arg);
        }

        @Deprecated
        public static IRubyObject seteuid(IRubyObject recv, IRubyObject arg) {
            return euid_set(((RubyBasicObject) recv).getCurrentContext(), arg);
        }
        @JRubyMethod(name = "seteuid", module = true, visibility = PRIVATE)
        public static IRubyObject seteuid(ThreadContext context, IRubyObject recv, IRubyObject arg) {
            return euid_set(context, arg);
        }

        @Deprecated
        public static IRubyObject setgid(IRubyObject recv, IRubyObject arg) {
            return gid_set(((RubyBasicObject) recv).getCurrentContext().runtime, arg);
        }
        @JRubyMethod(name = "setgid", module = true, visibility = PRIVATE)
        public static IRubyObject setgid(ThreadContext context, IRubyObject recv, IRubyObject arg) {
            return gid_set(context.runtime, arg);
        }

        @Deprecated
        public static IRubyObject setuid(IRubyObject recv, IRubyObject arg) {
            return uid_set(((RubyBasicObject) recv).getCurrentContext(), null, arg);
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

    @Deprecated(since = "10.0")
    public static IRubyObject exit_bang(IRubyObject recv, IRubyObject[] args) {
        return exit_bang(((RubyBasicObject) recv).getCurrentContext(), recv, args);
    }

    @JRubyMethod(name = "exit!", optional = 1, checkArity = false, module = true, visibility = PRIVATE)
    public static IRubyObject exit_bang(ThreadContext context, IRubyObject recv, IRubyObject[] args) {
        return RubyKernel.exit_bang(context, recv, args);
    }

    @Deprecated(since = "10.0")
    public static IRubyObject groups(IRubyObject recv) {
        return groups(((RubyBasicObject) recv).getCurrentContext(), recv);
    }

    @JRubyMethod(name = "groups", module = true, visibility = PRIVATE)
    public static IRubyObject groups(ThreadContext context, IRubyObject recv) {
        long[] groups = context.runtime.getPosix().getgroups();
        if (groups == null) throw notImplementedError(context, "groups() function is unimplemented on this machine");

        IRubyObject[] ary = new IRubyObject[groups.length];
        for(int i = 0; i < groups.length; i++) {
            ary[i] = asFixnum(context, groups[i]);
        }
        return RubyArray.newArrayNoCopy(context.runtime, ary);
    }

    @JRubyMethod(name = "last_status", module = true, visibility = PRIVATE)
    public static IRubyObject last_status(ThreadContext context, IRubyObject recv) {
        return nullToNil(context.getLastExitStatus(), context.nil);
    }

    @JRubyMethod(name = "setrlimit", module = true, visibility = PRIVATE)
    public static IRubyObject setrlimit(ThreadContext context, IRubyObject recv, IRubyObject resource, IRubyObject rlimCur) {
        return setrlimit(context, recv, resource, rlimCur, context.nil);
    }

    @JRubyMethod(name = "setrlimit", module = true, visibility = PRIVATE)
    public static IRubyObject setrlimit(ThreadContext context, IRubyObject recv, IRubyObject resource, IRubyObject rlimCur, IRubyObject rlimMax) {
        if (Platform.IS_WINDOWS) throw notImplementedError(context, "Process#setrlimit is not implemented on Windows");

        var posix = context.runtime.getPosix();

        if (!posix.isNative()) {
            warn(context, "Process#setrlimit not supported on this platform");
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

    private static long rlimitResourceValue(ThreadContext context, IRubyObject rval) {
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
                return toLong(context, rval);
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
                return toInt(context, rtype);
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
        return getpgrp(((RubyBasicObject) recv).getCurrentContext(), recv);
    }

    @JRubyMethod(name = "getpgrp", module = true, visibility = PRIVATE)
    public static IRubyObject getpgrp(ThreadContext context, IRubyObject recv) {
        return asFixnum(context, context.runtime.getPosix().getpgrp());
    }

    @Deprecated(since = "10.0")
    public static IRubyObject getpgrp(Ruby runtime) {
        return asFixnum(runtime.getCurrentContext(), runtime.getPosix().getpgrp());
    }

    @Deprecated(since = "10.0")
    public static IRubyObject groups_set(IRubyObject recv, IRubyObject arg) {
        return groups_set(((RubyBasicObject) recv).getCurrentContext(), recv, arg);
    }

    @JRubyMethod(name = "groups=", module = true, visibility = PRIVATE)
    public static IRubyObject groups_set(ThreadContext context, IRubyObject recv, IRubyObject arg) {
        throw notImplementedError(context, "Process#groups not yet implemented");
    }

    @Deprecated
    public static IRubyObject waitpid(IRubyObject recv, IRubyObject[] args) {
        return waitpid(((RubyBasicObject) recv).getCurrentContext(), args);
    }
    @JRubyMethod(name = "waitpid", rest = true, module = true, visibility = PRIVATE)
    public static IRubyObject waitpid(ThreadContext context, IRubyObject recv, IRubyObject[] args) {
        return waitpid(context, args);
    }

    @JRubyMethod(module = true)
    public static IRubyObject warmup(ThreadContext context, IRubyObject recv) {
        // By the time we can call this method core will be bootstrapped.  Unless we want to differentiate
        // a better point for "optimizations" to be possible we can just assume we are good to go.
        return context.tru;
    }

    @Deprecated(since = "10.0")
    public static IRubyObject waitpid(Ruby runtime, IRubyObject[] args) {
        return waitpid(runtime.getCurrentContext(), args);
    }

    public static IRubyObject waitpid(ThreadContext context, IRubyObject[] args) {
        long pid = args.length > 0 ? toLong(context, args[0]) : -1;
        int flags = args.length > 1 ? toInt(context, args[1]) : 0;
        var result = waitpid(context, pid, flags);

        checkErrno(context, result, ECHILD);

        return result == 0 ? context.nil : asFixnum(context, result);
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
        return wait(((RubyBasicObject) recv).getCurrentContext(), args);
    }
    @JRubyMethod(name = "wait", rest = true, module = true, visibility = PRIVATE)
    public static IRubyObject wait(ThreadContext context, IRubyObject recv, IRubyObject[] args) {
        return wait(context, args);
    }

    @Deprecated(since = "10.0")
    public static IRubyObject wait(Ruby runtime, IRubyObject[] args) {
        return wait(((RubyBasicObject) args[0]).getCurrentContext(), args);
    }

    public static IRubyObject wait(ThreadContext context, IRubyObject[] args) {
        if (args.length > 0) return waitpid(context, args);

        int[] status = new int[1];
        POSIX posix = context.runtime.getPosix();

        posix.errno(0);

        int pid = pthreadKillable(context, ctx -> posix.wait(status));

        checkErrno(context, pid, ECHILD);

        context.setLastExitStatus(RubyProcess.RubyStatus.newProcessStatus(context.runtime, status[0], pid));
        return asFixnum(context, pid);
    }


    @Deprecated
    public static IRubyObject waitall(IRubyObject recv) {
        return waitall(((RubyBasicObject) recv).getCurrentContext());
    }
    @JRubyMethod(name = "waitall", module = true, visibility = PRIVATE)
    public static IRubyObject waitall(ThreadContext context, IRubyObject recv) {
        return waitall(context);
    }

    @Deprecated(since = "10.0")
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
        return setsid(((RubyBasicObject) recv).getCurrentContext().runtime);
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
        return setpgrp(((RubyBasicObject) recv).getCurrentContext().runtime);
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
        return egid_set(((RubyBasicObject) recv).getCurrentContext(), arg);
    }
    @JRubyMethod(name = "egid=", module = true, visibility = PRIVATE)
    public static IRubyObject egid_set(ThreadContext context, IRubyObject recv, IRubyObject arg) {
        return egid_set(context, arg);
    }

    @Deprecated(since = "10.0")
    public static IRubyObject egid_set(Ruby runtime, IRubyObject arg) {
        return egid_set(runtime.getCurrentContext(), arg);
    }

    public static IRubyObject egid_set(ThreadContext context, IRubyObject arg) {
        int gid;
        if (arg instanceof RubyInteger || arg.checkStringType().isNil()) {
            gid = toInt(context, arg);
        } else {
            Group group = context.runtime.getPosix().getgrnam(arg.asJavaString());
            if (group == null) throw argumentError(context, "can't find group for " + arg.inspect(context));
            gid = (int)group.getGID();
        }
        checkErrno(context, context.runtime.getPosix().setegid(gid));
        return asFixnum(context, 0);
    }

    @Deprecated
    public static IRubyObject euid(IRubyObject recv) {
        return euid(((RubyBasicObject) recv).getCurrentContext(), recv);
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
        return uid_set(((RubyBasicObject) recv).getCurrentContext(), null, arg);
    }
    @JRubyMethod(name = "uid=", module = true, visibility = PRIVATE)
    public static IRubyObject uid_set(ThreadContext context, IRubyObject recv, IRubyObject arg) {
        checkErrno(context, context.runtime.getPosix().setuid(toInt(context, arg)));
        return asFixnum(context, 0);
    }
    @Deprecated
    public static IRubyObject uid_set(Ruby runtime, IRubyObject arg) {
        return uid_set(runtime.getCurrentContext(), null, arg);
    }

    @Deprecated
    public static IRubyObject gid(IRubyObject recv) {
        return gid(((RubyBasicObject) recv).getCurrentContext());
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

    @Deprecated(since = "10.0")
    public static IRubyObject maxgroups(IRubyObject recv) {
        return maxgroups(((RubyBasicObject) recv).getCurrentContext(), recv);
    }

    @JRubyMethod(name = "maxgroups", module = true, visibility = PRIVATE)
    public static IRubyObject maxgroups(ThreadContext context, IRubyObject recv) {
        throw notImplementedError(context, "Process#maxgroups not yet implemented");
    }

    @Deprecated
    public static IRubyObject getpriority(IRubyObject recv, IRubyObject arg1, IRubyObject arg2) {
        return getpriority(((RubyBasicObject) recv).getCurrentContext(), recv, arg1, arg2);
    }
    @JRubyMethod(name = "getpriority", module = true, visibility = PRIVATE)
    public static IRubyObject getpriority(ThreadContext context, IRubyObject recv, IRubyObject arg1, IRubyObject arg2) {
        int which = toInt(context, arg1);
        int who = toInt(context, arg2);
        int result = checkErrno(context, context.runtime.getPosix().getpriority(which, who));

        return asFixnum(context, result);
    }

    @Deprecated
    public static IRubyObject getpriority(Ruby runtime, IRubyObject arg1, IRubyObject arg2) {
        return getpriority(runtime.getCurrentContext(), null, arg1, arg2);
    }

    @Deprecated
    public static IRubyObject uid(IRubyObject recv) {
        return uid(((RubyBasicObject) recv).getCurrentContext(), recv);
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

    @Deprecated(since = "10.0")
    public static IRubyObject initgroups(IRubyObject recv, IRubyObject arg1, IRubyObject arg2) {
        return initgroups(((RubyBasicObject) recv).getCurrentContext(), recv, arg1, arg2);
    }

    @JRubyMethod(name = "initgroups", module = true, visibility = PRIVATE)
    public static IRubyObject initgroups(ThreadContext context, IRubyObject recv, IRubyObject arg1, IRubyObject arg2) {
        throw notImplementedError(context, "Process#initgroups not yet implemented");
    }

    @Deprecated(since = "10.0")
    public static IRubyObject maxgroups_set(IRubyObject recv, IRubyObject arg) {
        return maxgroups_set(((RubyBasicObject) recv).getCurrentContext(), recv, arg);
    }

    @JRubyMethod(name = "maxgroups=", module = true, visibility = PRIVATE)
    public static IRubyObject maxgroups_set(ThreadContext context, IRubyObject recv, IRubyObject arg) {
        throw notImplementedError(context, "Process#maxgroups_set not yet implemented");
    }

    @Deprecated
    public static IRubyObject ppid(IRubyObject recv) {
        return ppid(((RubyBasicObject) recv).getCurrentContext(), recv);
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
        return gid_set(((RubyBasicObject) recv).getCurrentContext(), recv, arg);
    }
    @JRubyMethod(name = "gid=", module = true, visibility = PRIVATE)
    public static IRubyObject gid_set(ThreadContext context, IRubyObject recv, IRubyObject arg) {
        int result = checkErrno(context, context.runtime.getPosix().setgid(toInt(context, arg)));

        return asFixnum(context, result);

    }
    @Deprecated
    public static IRubyObject gid_set(Ruby runtime, IRubyObject arg) {
        return gid_set(runtime.getCurrentContext(), null, arg);
    }

    @Deprecated
    public static IRubyObject wait2(IRubyObject recv, IRubyObject[] args) {
        return waitpid2(((RubyBasicObject) recv).getCurrentContext(), recv, args);
    }
    @JRubyMethod(name = "wait2", rest = true, module = true, visibility = PRIVATE)
    public static IRubyObject wait2(ThreadContext context, IRubyObject recv, IRubyObject[] args) {
        return waitpid2(context.runtime, args);
    }

    @Deprecated
    public static IRubyObject euid_set(IRubyObject recv, IRubyObject arg) {
        return euid_set(((RubyBasicObject) recv).getCurrentContext(), arg);
    }
    @JRubyMethod(name = "euid=", module = true, visibility = PRIVATE)
    public static IRubyObject euid_set(ThreadContext context, IRubyObject recv, IRubyObject arg) {
        return euid_set(context, arg);
    }

    @Deprecated(since = "10.0")
    public static IRubyObject euid_set(Ruby runtime, IRubyObject arg) {
        return euid_set(runtime.getCurrentContext(), arg);
    }


    public static IRubyObject euid_set(ThreadContext context, IRubyObject arg) {
        int uid;
        if (arg instanceof RubyInteger || arg.checkStringType().isNil()) {
            uid = toInt(context, arg);
        } else {
            Passwd password = context.runtime.getPosix().getpwnam(arg.asJavaString());
            if (password == null) {
                throw argumentError(context, "can't find user for " + arg.inspect(context));
            }
            uid = (int)password.getUID();
        }
        checkErrno(context, context.runtime.getPosix().seteuid(uid));
        return asFixnum(context, 0);
    }

    @Deprecated
    public static IRubyObject setpriority(IRubyObject recv, IRubyObject arg1, IRubyObject arg2, IRubyObject arg3) {
        return setpriority(((RubyBasicObject) recv).getCurrentContext(), recv, arg1, arg2, arg3);
    }
    @JRubyMethod(name = "setpriority", module = true, visibility = PRIVATE)
    public static IRubyObject setpriority(ThreadContext context, IRubyObject recv, IRubyObject arg1, IRubyObject arg2, IRubyObject arg3) {
        int which = toInt(context, arg1);
        int who = toInt(context, arg2);
        int prio = toInt(context, arg3);
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
        return setpgid(((RubyBasicObject) recv).getCurrentContext(), recv, arg1, arg2);
    }
    @JRubyMethod(name = "setpgid", module = true, visibility = PRIVATE)
    public static IRubyObject setpgid(ThreadContext context, IRubyObject recv, IRubyObject arg1, IRubyObject arg2) {
        int pid = toInt(context, arg1);
        int gid = toInt(context, arg2);
        return asFixnum(context, checkErrno(context, context.runtime.getPosix().setpgid(pid, gid)));
    }

    @Deprecated
    public static IRubyObject setpgid(Ruby runtime, IRubyObject arg1, IRubyObject arg2) {
        return setpgid(runtime.getCurrentContext(), null, arg1, arg2);
    }

    @Deprecated
    public static IRubyObject getpgid(IRubyObject recv, IRubyObject arg) {
        return getpgid(((RubyBasicObject) recv).getCurrentContext(), recv, arg);
    }
    @JRubyMethod(name = "getpgid", module = true, visibility = PRIVATE)
    public static IRubyObject getpgid(ThreadContext context, IRubyObject recv, IRubyObject arg) {
        int pgid = toInt(context, arg);
        return asFixnum(context, checkErrno(context, context.runtime.getPosix().getpgid(pgid)));

    }
    @Deprecated
    public static IRubyObject getpgid(Ruby runtime, IRubyObject arg) {
        return getpgid(runtime.getCurrentContext(), null, arg);
    }

    @Deprecated
    public static IRubyObject getrlimit(IRubyObject recv, IRubyObject arg) {
        return getrlimit(((RubyBasicObject) recv).getCurrentContext(), arg);
    }
    @JRubyMethod(name = "getrlimit", module = true, visibility = PRIVATE)
    public static IRubyObject getrlimit(ThreadContext context, IRubyObject recv, IRubyObject arg) {
        return getrlimit(context, arg);
    }

    @Deprecated(since = "10.0")
    public static IRubyObject getrlimit(Ruby runtime, IRubyObject arg) {
        return getrlimit(runtime.getCurrentContext(), arg);
    }

    public static IRubyObject getrlimit(ThreadContext context, IRubyObject arg) {
        if (Platform.IS_WINDOWS) {
            throw notImplementedError(context, "Process#getrlimit is not implemented on Windows");
        }

        if (!context.runtime.getPosix().isNative()) {
            warn(context, "Process#getrlimit not supported on this platform");
            RubyFixnum max = asFixnum(context, Long.MAX_VALUE);
            return newArray(context, max, max);
        }

        RLimit rlimit = context.runtime.getPosix().getrlimit(rlimitResourceType(context, arg));

        return newArray(context, asFixnum(context, rlimit.rlimCur()), asFixnum(context, rlimit.rlimMax()));
    }

    @Deprecated
    public static IRubyObject egid(IRubyObject recv) {
        return egid(((RubyBasicObject) recv).getCurrentContext(), recv);
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
        return kill(((RubyBasicObject) recv).getCurrentContext(), recv, args);
    }
    @JRubyMethod(name = "kill", rest = true, module = true, visibility = PRIVATE)
    public static IRubyObject kill(ThreadContext context, IRubyObject recv, IRubyObject[] args) {
        if (args.length < 2) throw argumentError(context, "wrong number of arguments -- kill(sig, pid...)");

        int signal = switch(args[0]) {
            case RubyFixnum fixnum -> fixnum.asInt(context);
            case RubySymbol sym -> parseSignalString(context, sym.idString());
            case RubyString str -> parseSignalString(context, str.asJavaString());
            default -> parseSignalString(context, args[0].checkStringType().toString());
        };

        boolean processGroupKill = signal < 0;

        if (processGroupKill) {
            if (Platform.IS_WINDOWS) {
                throw context.runtime.newErrnoEINVALError("group signals not implemented in windows");
            }
            signal = -signal;
        }

        if (Platform.IS_WINDOWS) {
            for (int i = 1; i < args.length; i++) {
                int pid = toInt(context, args[i]);
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
                    throw notImplementedError(context, "this signal not yet implemented in windows");
                }
            }
        } else {
            POSIX posix = context.runtime.getPosix();
            for (int i = 1; i < args.length; i++) {
                int pid = toInt(context, args[i]);

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
        final long pid = toLong(context, arg);

        BlockCallback callback = (ctx, args, block) -> {
            // push a dummy frame to avoid AIOOB if an exception fires
            ctx.pushFrame();

            while (waitpid(ctx.runtime, pid, 0) == 0) {}

            return last_status(ctx, recv);
        };

        return RubyThread.startWaiterThread(
                context.runtime,
                pid,
                CallBlock.newCallClosure(context, recv, Signature.NO_ARGUMENTS, callback));
    }

    @Deprecated
    public static IRubyObject times(IRubyObject recv, Block unusedBlock) {
        return times(((RubyBasicObject) recv).getCurrentContext(), recv, unusedBlock);
    }
    @JRubyMethod(module = true, visibility = PRIVATE)
    public static IRubyObject times(ThreadContext context, IRubyObject recv, Block unusedBlock) {
        return times(context);
    }

    @Deprecated(since = "10.0")
    public static IRubyObject times(Ruby runtime) {
        return times(runtime.getCurrentContext());
    }

    public static IRubyObject times(ThreadContext context) {
        Times tms = context.runtime.getPosix().times();
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

        long hz = context.runtime.getPosix().sysconf(Sysconf._SC_CLK_TCK);
        if (hz == -1) {
            hz = 60; //https://github.com/ruby/ruby/blob/trunk/process.c#L6616
        }

        return newStruct(context, (RubyClass) context.runtime.getTmsStruct(),
                new IRubyObject[] {
                        asFloat(context, utime / (double) hz),
                        asFloat(context, stime / (double) hz),
                        asFloat(context, cutime / (double) hz),
                        asFloat(context, cstime / (double) hz)
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
        if (_clock_id instanceof RubySymbol clock_id) {
            if (clock_id.idString().equals(CLOCK_MONOTONIC)) {
                return System.nanoTime();
            } else if (clock_id.idString().equals(CLOCK_REALTIME)) {
                POSIX posix = context.runtime.getPosix();
                long nanos;
                if (posix.isNative()) {
                    Timeval tv = posix.allocateTimeval();
                    posix.gettimeofday(tv);
                    nanos = tv.sec() * 1_000_000_000 + tv.usec() * 1000;
                } else {
                    nanos = System.currentTimeMillis() * 1000000;
                }
                return nanos;
            }
        }

        throw clockError(context, "gettime", _clock_id);
    }

    /**
     * Get the time resolution in nanoseconds corresponding to the requested clock.
     */
    private static long getResolutionForClock(ThreadContext context, IRubyObject _clock_id) throws RaiseException {
        if (_clock_id instanceof RubySymbol) {
            RubySymbol clock_id = (RubySymbol) _clock_id;
            if (clock_id.idString().equals(CLOCK_MONOTONIC)) {
                return 1;
            } else if (clock_id.idString().equals(CLOCK_REALTIME)) {
                return 1000000;
            }
        }

        throw clockError(context, "getres", _clock_id);
    }

    private static RaiseException clockError(ThreadContext context, String clockMethod, IRubyObject _clock_id) {
        return context.runtime.newErrnoEINVALError("Process.clock_" + clockMethod + "(" + _clock_id.inspect().toString() + ")");
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
        return pid(((RubyBasicObject) recv).getCurrentContext());
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
        throw notImplementedError(context, "fork is not available on this platform");
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
        return PopenExecutor.nativePopenAvailable(context.runtime) ?
                PopenExecutor.spawn(context, args) :
                asFixnum(context, ShellLauncher.runExternalWithoutWait(context.runtime, args));
    }

    @Deprecated(since = "10.0")
    public static IRubyObject exit(IRubyObject recv, IRubyObject[] args) {
        return exit(((RubyBasicObject) recv).getCurrentContext(), recv, args);
    }

    @JRubyMethod(name = "exit", optional = 1, checkArity = false, module = true, visibility = PRIVATE)
    public static IRubyObject exit(ThreadContext context, IRubyObject recv, IRubyObject[] args) {
        return RubyKernel.exit(context, recv, args);
    }

    @Deprecated(since = "10.0")
    public static IRubyObject setproctitle(IRubyObject recv, IRubyObject name) {
        return setproctitle(((RubyBasicObject) recv).getCurrentContext(), recv, name);
    }

    @JRubyMethod(name = "setproctitle", module = true, visibility = PRIVATE)
    public static IRubyObject setproctitle(ThreadContext context, IRubyObject recv, IRubyObject name) {
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
        return waitpid2(((RubyBasicObject) recv).getCurrentContext().runtime, args);
    }
}
