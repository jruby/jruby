/*
 * Copyright (c) 2015, 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 *
 * Some of the code in this class is transliterated from C++ code in Rubinius.
 *
 * Copyright (c) 2007-2014, Evan Phoenix and contributors
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * * Redistributions of source code must retain the above copyright notice, this
 *   list of conditions and the following disclaimer.
 * * Redistributions in binary form must reproduce the above copyright notice
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution.
 * * Neither the name of Rubinius nor the names of its contributors
 *   may be used to endorse or promote products derived from this software
 *   without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.jruby.truffle.core;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.TruffleOptions;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.api.source.SourceSection;
import jnr.constants.platform.Sysconf;
import jnr.posix.Passwd;
import jnr.posix.Times;
import org.jcodings.specific.UTF8Encoding;
import org.jruby.truffle.Layouts;
import org.jruby.truffle.RubyContext;
import org.jruby.truffle.builtins.Primitive;
import org.jruby.truffle.builtins.PrimitiveArrayArgumentsNode;
import org.jruby.truffle.core.basicobject.BasicObjectNodes.ReferenceEqualNode;
import org.jruby.truffle.core.cast.NameToJavaStringNode;
import org.jruby.truffle.core.kernel.KernelNodes;
import org.jruby.truffle.core.kernel.KernelNodesFactory;
import org.jruby.truffle.core.proc.ProcSignalHandler;
import org.jruby.truffle.core.string.StringOperations;
import org.jruby.truffle.language.RubyGuards;
import org.jruby.truffle.language.control.ExitException;
import org.jruby.truffle.language.control.RaiseException;
import org.jruby.truffle.language.control.ThrowException;
import org.jruby.truffle.language.dispatch.CallDispatchHeadNode;
import org.jruby.truffle.language.dispatch.DispatchHeadNodeFactory;
import org.jruby.truffle.language.methods.InternalMethod;
import org.jruby.truffle.language.methods.LookupMethodNode;
import org.jruby.truffle.language.methods.LookupMethodNodeGen;
import org.jruby.truffle.language.objects.IsANode;
import org.jruby.truffle.language.objects.IsANodeGen;
import org.jruby.truffle.language.objects.LogicalClassNode;
import org.jruby.truffle.language.objects.LogicalClassNodeGen;
import org.jruby.truffle.language.objects.shared.SharedObjects;
import org.jruby.truffle.language.yield.YieldNode;
import org.jruby.truffle.platform.UnsafeGroup;
import org.jruby.truffle.platform.signal.Signal;
import org.jruby.truffle.platform.signal.SignalHandler;
import org.jruby.truffle.platform.signal.SignalManager;
import org.jruby.util.io.PosixShim;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.util.ArrayList;
import java.util.List;

import static jnr.constants.platform.Errno.ECHILD;
import static jnr.constants.platform.Errno.EINTR;
import static jnr.constants.platform.WaitFlags.WNOHANG;

public abstract class VMPrimitiveNodes {

    @Primitive(name = "vm_catch", needsSelf = false)
    public abstract static class CatchNode extends PrimitiveArrayArgumentsNode {

        @Child private YieldNode dispatchNode;

        public CatchNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            dispatchNode = new YieldNode(context);
        }

        @Specialization
        public Object doCatch(VirtualFrame frame, Object tag, DynamicObject block,
                @Cached("create()") BranchProfile catchProfile,
                @Cached("createBinaryProfile()") ConditionProfile matchProfile,
                @Cached("create()") ReferenceEqualNode referenceEqualNode) {
            try {
                return dispatchNode.dispatch(frame, block, tag);
            } catch (ThrowException e) {
                catchProfile.enter();
                if (matchProfile.profile(referenceEqualNode.executeReferenceEqual(e.getTag(), tag))) {
                    return e.getValue();
                } else {
                    throw e;
                }
            }
        }
    }

    @Primitive(name = "vm_gc_start", needsSelf = false)
    public static abstract class VMGCStartPrimitiveNode extends PrimitiveArrayArgumentsNode {

        @TruffleBoundary
        @Specialization
        public DynamicObject vmGCStart() {
            System.gc();
            return nil();
        }

    }

    // The hard #exit!
    @Primitive(name = "vm_exit", needsSelf = false, unsafe = UnsafeGroup.EXIT)
    public static abstract class VMExitPrimitiveNode extends PrimitiveArrayArgumentsNode {

        @Specialization
        public Object vmExit(int status) {
            throw new ExitException(status);
        }

        @Fallback
        public Object vmExit(Object status) {
            return null; // Primitive failure
        }

    }

    @Primitive(name = "vm_extended_modules", needsSelf = false)
    public static abstract class VMExtendedModulesNode extends PrimitiveArrayArgumentsNode {

        @Child private CallDispatchHeadNode newArrayNode;
        @Child private CallDispatchHeadNode arrayAppendNode;

        public VMExtendedModulesNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            newArrayNode = DispatchHeadNodeFactory.createMethodCall(context);
            arrayAppendNode = DispatchHeadNodeFactory.createMethodCall(context);
        }

        @Specialization
        public Object vmExtendedModules(VirtualFrame frame, Object object) {
            final DynamicObject metaClass = coreLibrary().getMetaClass(object);

            if (Layouts.CLASS.getIsSingleton(metaClass)) {
                final Object ret = newArrayNode.call(frame, coreLibrary().getArrayClass(), "new");

                for (DynamicObject included : Layouts.MODULE.getFields(metaClass).prependedAndIncludedModules()) {
                    arrayAppendNode.call(frame, ret, "<<", included);
                }

                return ret;
            }

            return nil();
        }

    }

    @Primitive(name = "vm_get_module_name", needsSelf = false)
    public static abstract class VMGetModuleNamePrimitiveNode extends PrimitiveArrayArgumentsNode {

        @Specialization
        public DynamicObject vmGetModuleName(DynamicObject module) {
            return createString(StringOperations.encodeRope(Layouts.MODULE.getFields(module).getName(), UTF8Encoding.INSTANCE));
        }

    }

    @Primitive(name = "vm_get_user_home", needsSelf = false, unsafe = UnsafeGroup.IO)
    public abstract static class VMGetUserHomePrimitiveNode extends PrimitiveArrayArgumentsNode {

        @TruffleBoundary
        @Specialization(guards = "isRubyString(username)")
        public DynamicObject vmGetUserHome(DynamicObject username) {
            // TODO BJF 30-APR-2015 Review the more robust getHomeDirectoryPath implementation
            final Passwd passwd = posix().getpwnam(username.toString());
            if (passwd == null) {
                throw new RaiseException(coreExceptions().argumentError("user " + username.toString() + " does not exist", this));
            }
            return createString(StringOperations.encodeRope(passwd.getHome(), UTF8Encoding.INSTANCE));
        }

    }

    @Primitive(name = "vm_object_class", needsSelf = false)
    public static abstract class VMObjectClassPrimitiveNode extends PrimitiveArrayArgumentsNode {

        @Child private LogicalClassNode classNode;

        public VMObjectClassPrimitiveNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            classNode = LogicalClassNodeGen.create(context, sourceSection, null);
        }

        @Specialization
        public DynamicObject vmObjectClass(VirtualFrame frame, Object object) {
            return classNode.executeLogicalClass(object);
        }

    }

    @Primitive(name = "vm_object_equal", needsSelf = false)
    public static abstract class VMObjectEqualPrimitiveNode extends PrimitiveArrayArgumentsNode {

        @Specialization
        public boolean vmObjectEqual(VirtualFrame frame, Object a, Object b,
                @Cached("create()") ReferenceEqualNode referenceEqualNode) {
            return referenceEqualNode.executeReferenceEqual(a, b);
        }

    }

    @Primitive(name = "vm_object_kind_of", needsSelf = false)
    public static abstract class VMObjectKindOfPrimitiveNode extends PrimitiveArrayArgumentsNode {

        @Child private IsANode isANode;

        public VMObjectKindOfPrimitiveNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            isANode = IsANodeGen.create(context, sourceSection, null, null);
        }

        @Specialization
        public boolean vmObjectKindOf(Object object, DynamicObject rubyClass) {
            return isANode.executeIsA(object, rubyClass);
        }

    }

    @Primitive(name = "vm_method_is_basic", needsSelf = false)
    public static abstract class VMMethodIsBasicNode extends PrimitiveArrayArgumentsNode {

        public VMMethodIsBasicNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public boolean vmMethodIsBasic(VirtualFrame frame, DynamicObject method) {
            return Layouts.METHOD.getMethod(method).isBuiltIn();
        }

    }

    @Primitive(name = "vm_method_lookup", needsSelf = false)
    public static abstract class VMMethodLookupNode extends PrimitiveArrayArgumentsNode {

        @Child NameToJavaStringNode nameToJavaStringNode;
        @Child LookupMethodNode lookupMethodNode;

        public VMMethodLookupNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            nameToJavaStringNode = NameToJavaStringNode.create();
            lookupMethodNode = LookupMethodNodeGen.create(context, sourceSection, true, false, null, null);
        }

        @Specialization
        public DynamicObject vmMethodLookup(VirtualFrame frame, Object self, Object name) {
            // TODO BJF Sep 14, 2016 Handle private
            final String normalizedName = nameToJavaStringNode.executeToJavaString(frame, name);
            InternalMethod method = lookupMethodNode.executeLookupMethod(frame, self, normalizedName);
            if (method == null) {
                return nil();
            }
            return Layouts.METHOD.createMethod(coreLibrary().getMethodFactory(), self, method);
        }

    }

    @Primitive(name = "vm_object_respond_to", needsSelf = false)
    public static abstract class VMObjectRespondToPrimitiveNode extends PrimitiveArrayArgumentsNode {

        @Child private KernelNodes.RespondToNode respondToNode;

        public VMObjectRespondToPrimitiveNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            respondToNode = KernelNodesFactory.RespondToNodeFactory.create(context, sourceSection, null, null, null);
        }

        @Specialization
        public boolean vmObjectRespondTo(VirtualFrame frame, Object object, Object name, boolean includePrivate) {
            return respondToNode.executeDoesRespondTo(frame, object, name, includePrivate);
        }

    }


    @Primitive(name = "vm_object_singleton_class", needsSelf = false)
    public static abstract class VMObjectSingletonClassPrimitiveNode extends PrimitiveArrayArgumentsNode {

        @Child private KernelNodes.SingletonClassMethodNode singletonClassNode;

        public VMObjectSingletonClassPrimitiveNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            singletonClassNode = KernelNodesFactory.SingletonClassMethodNodeFactory.create(context, sourceSection, null);
        }

        @Specialization
        public Object vmObjectClass(Object object) {
            return singletonClassNode.singletonClass(object);
        }

    }

    @Primitive(name = "vm_raise_exception", needsSelf = false)
    public static abstract class VMRaiseExceptionPrimitiveNode extends PrimitiveArrayArgumentsNode {
        public VMRaiseExceptionPrimitiveNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization(guards = "isRubyException(exception)")
        public DynamicObject vmRaiseException(DynamicObject exception) {
            throw new RaiseException(exception);
        }
    }

    @Primitive(name = "vm_set_module_name", needsSelf = false)
    public static abstract class VMSetModuleNamePrimitiveNode extends PrimitiveArrayArgumentsNode {

        @Specialization
        public Object vmSetModuleName(Object object) {
            throw new UnsupportedOperationException("vm_set_module_name");
        }

    }

    @Primitive(name = "vm_singleton_class_object", needsSelf = false)
    public static abstract class VMObjectSingletonClassObjectPrimitiveNode extends PrimitiveArrayArgumentsNode {

        @Specialization
        public Object vmSingletonClassObject(Object object) {
            return RubyGuards.isRubyClass(object) && Layouts.CLASS.getIsSingleton((DynamicObject) object);
        }

    }

    @Primitive(name = "vm_throw", needsSelf = false)
    public abstract static class ThrowNode extends PrimitiveArrayArgumentsNode {

        @Specialization
        public Object doThrow(Object tag, Object value) {
            throw new ThrowException(tag, value);
        }

    }

    @Primitive(name = "vm_time", needsSelf = false)
    public abstract static class TimeNode extends PrimitiveArrayArgumentsNode {

        @Specialization
        public long time() {
            return System.currentTimeMillis() / 1000;
        }

    }

    @Primitive(name = "vm_times", needsSelf = false)
    public abstract static class TimesNode extends PrimitiveArrayArgumentsNode {

        @TruffleBoundary
        @Specialization
        public DynamicObject times() {
            // Copied from org/jruby/RubyProcess.java - see copyright and license information there

            Times tms = posix().times();
            double utime = 0.0d, stime = 0.0d, cutime = 0.0d, cstime = 0.0d;
            if (tms == null) {
                ThreadMXBean bean = ManagementFactory.getThreadMXBean();
                if (bean.isCurrentThreadCpuTimeSupported()) {
                    cutime = utime = bean.getCurrentThreadUserTime();
                    cstime = stime = bean.getCurrentThreadCpuTime() - bean.getCurrentThreadUserTime();
                }
            } else {
                utime = (double) tms.utime();
                stime = (double) tms.stime();
                cutime = (double) tms.cutime();
                cstime = (double) tms.cstime();
            }

            long hz = posix().sysconf(Sysconf._SC_CLK_TCK);
            if (hz == -1) {
                hz = 60; //https://github.com/ruby/ruby/blob/trunk/process.c#L6616
            }

            utime /= hz;
            stime /= hz;
            cutime /= hz;
            cstime /= hz;

            // TODO CS 24-Mar-15 what are these?
            final double tutime = 0;
            final double tstime = 0;

            return createArray(new double[] {
                    utime,
                    stime,
                    cutime,
                    cstime,
                    tutime,
                    tstime
            }, 6);
        }

    }

    @Primitive(name = "vm_watch_signal", needsSelf = false, unsafe = UnsafeGroup.SIGNALS)
    public static abstract class VMWatchSignalPrimitiveNode extends PrimitiveArrayArgumentsNode {

        @Specialization(guards = { "isRubyString(signalName)", "isRubyString(action)" })
        public boolean watchSignal(DynamicObject signalName, DynamicObject action) {
            if (!action.toString().equals("DEFAULT")) {
                throw new UnsupportedOperationException();
            }

            return handleDefault(signalName);
        }

        @Specialization(guards = { "isRubyString(signalName)", "isNil(nil)" })
        public boolean watchSignal(DynamicObject signalName, Object nil) {
            return handle(signalName, SignalManager.IGNORE_HANDLER);
        }

        @Specialization(guards = { "isRubyString(signalName)", "isRubyProc(proc)" })
        public boolean watchSignalProc(DynamicObject signalName, DynamicObject proc) {
            return handle(signalName, new ProcSignalHandler(getContext(), proc));
        }

        @TruffleBoundary
        private boolean handleDefault(DynamicObject signalName) {
            // We can't work with signals with AOT.
            if (TruffleOptions.AOT) {
                return true;
            }

            Signal signal = getContext().getNativePlatform().getSignalManager().createSignal(signalName.toString());
            try {
                getContext().getNativePlatform().getSignalManager().watchDefaultForSignal(signal);
            } catch (IllegalArgumentException e) {
                throw new RaiseException(coreExceptions().argumentError(e.getMessage(), this));
            }
            return true;
        }

        @TruffleBoundary
        private boolean handle(DynamicObject signalName, SignalHandler newHandler) {
            // We can't work with signals with AOT.
            if (TruffleOptions.AOT) {
                return true;
            }

            Signal signal = getContext().getNativePlatform().getSignalManager().createSignal(signalName.toString());
            try {
                getContext().getNativePlatform().getSignalManager().watchSignal(signal, newHandler);
            } catch (IllegalArgumentException e) {
                throw new RaiseException(coreExceptions().argumentError(e.getMessage(), this));
            }
            return true;
        }

    }

    @Primitive(name = "vm_get_config_item", needsSelf = false)
    public abstract static class VMGetConfigItemPrimitiveNode extends PrimitiveArrayArgumentsNode {

        @TruffleBoundary
        @Specialization(guards = "isRubyString(key)")
        public Object get(DynamicObject key) {
            // Sharing: we do not need to share here as it's only called by the main thread
            assert getContext().getThreadManager().getCurrentThread() == getContext().getThreadManager().getRootThread();

            final Object value = getContext().getNativePlatform().getRubiniusConfiguration().get(key.toString());

            if (value == null) {
                return nil();
            } else {
                return value;
            }
        }

    }

    @Primitive(name = "vm_get_config_section", needsSelf = false)
    public abstract static class VMGetConfigSectionPrimitiveNode extends PrimitiveArrayArgumentsNode {

        @TruffleBoundary
        @Specialization(guards = "isRubyString(section)")
        public DynamicObject getSection(DynamicObject section) {
            final List<DynamicObject> sectionKeyValues = new ArrayList<>();

            for (String key : getContext().getNativePlatform().getRubiniusConfiguration().getSection(section.toString())) {
                Object value = getContext().getNativePlatform().getRubiniusConfiguration().get(key);
                final String stringValue;
                if (RubyGuards.isRubyBignum(value)) {
                    stringValue = Layouts.BIGNUM.getValue((DynamicObject) value).toString();
                } else {
                    // This toString() is fine as we only have boolean, int, long and RubyString in config.
                    stringValue = value.toString();
                }

                Object[] objects = new Object[]{
                        createString(StringOperations.encodeRope(key, UTF8Encoding.INSTANCE)),
                        createString(StringOperations.encodeRope(stringValue, UTF8Encoding.INSTANCE)) };
                sectionKeyValues.add(createArray(objects, objects.length));
            }

            Object[] objects = sectionKeyValues.toArray();
            return createArray(objects, objects.length);
        }

    }

    @Primitive(name = "vm_wait_pid", needsSelf = false, unsafe = UnsafeGroup.PROCESSES)
    public abstract static class VMWaitPidPrimitiveNode extends PrimitiveArrayArgumentsNode {

        @TruffleBoundary
        @Specialization
        public Object waitPID(int input_pid, boolean no_hang) {
            // Transliterated from Rubinius C++ - not tidied up significantly to make merging changes easier

            int options = 0;
            final int[] statusReference = new int[]{ 0 };
            int pid;

            if (no_hang) {
                options |= WNOHANG.intValue();
            }

            final int finalOptions = options;

            pid = getContext().getThreadManager().runUntilResult(this, () -> {
                int result = posix().waitpid(input_pid, statusReference, finalOptions);
                if (result == -1 && posix().errno() == EINTR.intValue()) {
                    throw new InterruptedException();
                }
                return result;
            });

            final int errno = posix().errno();

            if (pid == -1) {
                if (errno == ECHILD.intValue()) {
                    return false;
                }

                // TODO handle other errnos?
                return false;
            }

            if (no_hang && pid == 0) {
                return nil();
            }

            Object output = nil();
            Object termsig = nil();
            Object stopsig = nil();

            final int status = statusReference[0];

            if (PosixShim.WAIT_MACROS.WIFEXITED(status)) {
                output = PosixShim.WAIT_MACROS.WEXITSTATUS(status);
            } else if (PosixShim.WAIT_MACROS.WIFSIGNALED(status)) {
                termsig = PosixShim.WAIT_MACROS.WTERMSIG(status);
            } else if (PosixShim.WAIT_MACROS.WIFSTOPPED(status)) {
                stopsig = PosixShim.WAIT_MACROS.WSTOPSIG(status);
            }

            Object[] objects = new Object[]{ output, termsig, stopsig, pid };
            return createArray(objects, objects.length);
        }

    }

    @Primitive(name = "vm_set_class", needsSelf = false)
    public abstract static class VMSetClassPrimitiveNode extends PrimitiveArrayArgumentsNode {

        @Specialization(guards = "isRubyClass(newClass)")
        public DynamicObject setClass(DynamicObject object, DynamicObject newClass) {
            SharedObjects.propagate(object, newClass);
            synchronized (object) {
                Layouts.BASIC_OBJECT.setLogicalClass(object, newClass);
                Layouts.BASIC_OBJECT.setMetaClass(object, newClass);
            }
            return object;
        }

    }

    @Primitive(name = "vm_set_process_title", needsSelf = false)
    public abstract static class VMSetProcessTitleNode extends PrimitiveArrayArgumentsNode {

        @TruffleBoundary
        @Specialization(guards = "isRubyString(name)")
        protected Object writeProgramName(DynamicObject name) {
            if (getContext().getNativePlatform().getProcessName().canSet()) {
                getContext().getNativePlatform().getProcessName().set(name.toString());
            }

            return name;
        }

    }


}
