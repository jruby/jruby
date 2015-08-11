/*
 * Copyright (c) 2015 Oracle and/or its affiliates. All rights reserved. This
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
package org.jruby.truffle.nodes.rubinius;

import static jnr.constants.platform.Errno.ECHILD;
import static jnr.constants.platform.Errno.EINTR;
import static jnr.constants.platform.WaitFlags.WNOHANG;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.util.ArrayList;
import java.util.List;

import jnr.constants.platform.Sysconf;
import jnr.posix.Passwd;
import jnr.posix.Times;

import org.jruby.truffle.nodes.RubyGuards;
import org.jruby.truffle.nodes.RubyNode;
import org.jruby.truffle.nodes.core.BasicObjectNodes;
import org.jruby.truffle.nodes.core.BasicObjectNodes.ReferenceEqualNode;
import org.jruby.truffle.nodes.core.BasicObjectNodesFactory;
import org.jruby.truffle.nodes.core.BasicObjectNodesFactory.ReferenceEqualNodeFactory;
import org.jruby.truffle.nodes.core.BignumNodes;
import org.jruby.truffle.nodes.core.KernelNodes;
import org.jruby.truffle.nodes.core.KernelNodesFactory;
import org.jruby.truffle.nodes.core.ModuleNodes;
import org.jruby.truffle.nodes.core.array.ArrayNodes;
import org.jruby.truffle.nodes.exceptions.ClearExceptionVariableNode;
import org.jruby.truffle.nodes.objects.ClassNode;
import org.jruby.truffle.nodes.objects.ClassNodeGen;
import org.jruby.truffle.nodes.yield.YieldDispatchHeadNode;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.control.RaiseException;
import org.jruby.truffle.runtime.control.ThrowException;
import com.oracle.truffle.api.object.DynamicObject;
import org.jruby.truffle.runtime.signal.ProcSignalHandler;
import org.jruby.truffle.runtime.signal.SignalOperations;
import org.jruby.truffle.runtime.subsystems.ThreadManager;
import org.jruby.util.io.PosixShim;

import sun.misc.Signal;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.source.SourceSection;

/**
 * Rubinius primitives associated with the VM.
 */
public abstract class VMPrimitiveNodes {

    @RubiniusPrimitive(name = "vm_catch", needsSelf = false)
    public abstract static class CatchNode extends RubiniusPrimitiveNode {

        @Child private YieldDispatchHeadNode dispatchNode;
        @Child private BasicObjectNodes.ReferenceEqualNode referenceEqualNode;
        @Child private ClearExceptionVariableNode clearExceptionVariableNode;

        public CatchNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            dispatchNode = new YieldDispatchHeadNode(context);
        }

        private boolean areSame(VirtualFrame frame, Object left, Object right) {
            if (referenceEqualNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                referenceEqualNode = insert(BasicObjectNodesFactory.ReferenceEqualNodeFactory.create(getContext(), getSourceSection(), null, null));
            }
            return referenceEqualNode.executeReferenceEqual(frame, left, right);
        }

        @Specialization(guards = "isRubyProc(block)")
        public Object doCatch(VirtualFrame frame, Object tag, DynamicObject block) {
            CompilerDirectives.transferToInterpreter();

            try {
                return dispatchNode.dispatch(frame, block, tag);
            } catch (ThrowException e) {
                if (areSame(frame, e.getTag(), tag)) {
                    if (clearExceptionVariableNode == null) {
                        CompilerDirectives.transferToInterpreter();
                        clearExceptionVariableNode = insert(new ClearExceptionVariableNode(getContext(), getSourceSection()));
                    }
                    clearExceptionVariableNode.execute(frame);
                    return e.getValue();
                } else {
                    throw e;
                }
            }
        }
    }

    @RubiniusPrimitive(name = "vm_gc_start", needsSelf = false)
    public static abstract class VMGCStartPrimitiveNode extends RubiniusPrimitiveNode {

        public VMGCStartPrimitiveNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public DynamicObject vmGCStart() {
            System.gc();
            return nil();
        }

    }

    @RubiniusPrimitive(name = "vm_get_module_name", needsSelf = false)
    public static abstract class VMGetModuleNamePrimitiveNode extends RubiniusPrimitiveNode {

        public VMGetModuleNamePrimitiveNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public DynamicObject vmGetModuleName(DynamicObject module) {
            return createString(ModuleNodes.getModel(module).getName());
        }

    }

    @RubiniusPrimitive(name = "vm_get_user_home", needsSelf = false)
    public abstract static class VMGetUserHomePrimitiveNode extends RubiniusPrimitiveNode {

        public VMGetUserHomePrimitiveNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization(guards = "isRubyString(username)")
        public DynamicObject vmGetUserHome(DynamicObject username) {
            CompilerDirectives.transferToInterpreter();
            // TODO BJF 30-APR-2015 Review the more robust getHomeDirectoryPath implementation
            final Passwd passwd = getContext().getPosix().getpwnam(username.toString());
            if (passwd == null) {
                CompilerDirectives.transferToInterpreter();
                throw new RaiseException(getContext().getCoreLibrary().argumentError("user " + username.toString() + " does not exist", this));
            }
            return createString(passwd.getHome());
        }

    }

    @RubiniusPrimitive(name = "vm_object_class", needsSelf = false)
    public static abstract class VMObjectClassPrimitiveNode extends RubiniusPrimitiveNode {

        @Child private ClassNode classNode;

        public VMObjectClassPrimitiveNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            classNode = ClassNodeGen.create(context, sourceSection, null);
        }

        @Specialization
        public DynamicObject vmObjectClass(VirtualFrame frame, Object object) {
            return classNode.executeGetClass(frame, object);
        }

    }

    @RubiniusPrimitive(name = "vm_object_equal", needsSelf = false)
    public static abstract class VMObjectEqualPrimitiveNode extends RubiniusPrimitiveNode {

        @Child ReferenceEqualNode referenceEqualNode;

        public VMObjectEqualPrimitiveNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            referenceEqualNode = ReferenceEqualNodeFactory.create(context, sourceSection, null, null);
        }

        @Specialization
        public boolean vmObjectEqual(VirtualFrame frame, Object a, Object b) {
            return referenceEqualNode.executeReferenceEqual(frame, a, b);
        }

    }

    @RubiniusPrimitive(name = "vm_object_kind_of", needsSelf = false)
    public static abstract class VMObjectKindOfPrimitiveNode extends RubiniusPrimitiveNode {

        @Child private KernelNodes.IsANode isANode;

        public VMObjectKindOfPrimitiveNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            isANode = KernelNodesFactory.IsANodeFactory.create(context, sourceSection, new RubyNode[] { null, null });
        }

        @Specialization(guards = "isRubyModule(rubyClass)")
        public boolean vmObjectKindOf(VirtualFrame frame, Object object, DynamicObject rubyClass) {
            return isANode.executeIsA(frame, object, rubyClass);
        }

    }

    @RubiniusPrimitive(name = "vm_object_respond_to", needsSelf = false)
    public static abstract class VMObjectRespondToPrimitiveNode extends RubiniusPrimitiveNode {

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

    @RubiniusPrimitive(name = "vm_object_singleton_class", needsSelf = false)
    public static abstract class VMObjectSingletonClassPrimitiveNode extends RubiniusPrimitiveNode {

        @Child private KernelNodes.SingletonClassMethodNode singletonClassNode;

        public VMObjectSingletonClassPrimitiveNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            singletonClassNode = KernelNodesFactory.SingletonClassMethodNodeFactory.create(context, sourceSection, new RubyNode[] { null });
        }

        @Specialization
        public Object vmObjectClass(VirtualFrame frame, Object object) {
            return singletonClassNode.singletonClass(frame, object);
        }

    }

    @RubiniusPrimitive(name = "vm_raise_exception", needsSelf = false)
    public static abstract class VMRaiseExceptionPrimitiveNode extends RubiniusPrimitiveNode {
        public VMRaiseExceptionPrimitiveNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization(guards = "isRubyException(exception)")
        public DynamicObject vmRaiseException(DynamicObject exception) {
            throw new RaiseException(exception);
        }
    }

    @RubiniusPrimitive(name = "vm_set_module_name", needsSelf = false)
    public static abstract class VMSetModuleNamePrimitiveNode extends RubiniusPrimitiveNode {

        public VMSetModuleNamePrimitiveNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public Object vmSetModuleName(Object object) {
            throw new UnsupportedOperationException("vm_set_module_name");
        }

    }

    @RubiniusPrimitive(name = "vm_singleton_class_object", needsSelf = false)
    public static abstract class VMObjectSingletonClassObjectPrimitiveNode extends RubiniusPrimitiveNode {

        public VMObjectSingletonClassObjectPrimitiveNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public Object vmSingletonClassObject(Object object) {
            return RubyGuards.isRubyClass(object) && ModuleNodes.getModel((DynamicObject) object).isSingleton();
        }

    }

    @RubiniusPrimitive(name = "vm_throw", needsSelf = false)
    public abstract static class ThrowNode extends RubiniusPrimitiveNode {

        public ThrowNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public Object doThrow(Object tag, Object value) {
            throw new ThrowException(tag, value);
        }

    }

    @RubiniusPrimitive(name = "vm_time", needsSelf = false)
    public abstract static class TimeNode extends RubiniusPrimitiveNode {

        public TimeNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public long time() {
            return System.currentTimeMillis() / 1000;
        }

    }

    @RubiniusPrimitive(name = "vm_times", needsSelf = false)
    public abstract static class TimesNode extends RubiniusPrimitiveNode {

        public TimesNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @TruffleBoundary
        @Specialization
        public DynamicObject times() {
            // Copied from org/jruby/RubyProcess.java - see copyright and license information there

            Times tms = posix().times();
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

            return createArray(new double[]{
                    utime,
                    stime,
                    cutime,
                    cstime,
                    tutime,
                    tstime
            }, 6);
        }

    }

    @SuppressWarnings("restriction")
    @RubiniusPrimitive(name = "vm_watch_signal", needsSelf = false)
    public static abstract class VMWatchSignalPrimitiveNode extends RubiniusPrimitiveNode {

        public VMWatchSignalPrimitiveNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization(guards = {"isRubyString(signalName)", "isRubyString(action)"})
        public boolean watchSignal(DynamicObject signalName, DynamicObject action) {
            if (!action.toString().equals("DEFAULT")) {
                throw new UnsupportedOperationException();
            }

            Signal signal = new Signal(signalName.toString());

            SignalOperations.watchDefaultForSignal(signal);
            return true;
        }

        @Specialization(guards = {"isRubyString(signalName)", "isNil(nil)"})
        public boolean watchSignal(DynamicObject signalName, Object nil) {
            Signal signal = new Signal(signalName.toString());

            SignalOperations.watchSignal(signal, SignalOperations.IGNORE_HANDLER);
            return true;
        }

        @Specialization(guards = {"isRubyString(signalName)", "isRubyProc(proc)"})
        public boolean watchSignalProc(DynamicObject signalName, DynamicObject proc) {
            Signal signal = new Signal(signalName.toString());

            SignalOperations.watchSignal(signal, new ProcSignalHandler(getContext(), proc));
            return true;
        }

    }

    @RubiniusPrimitive(name = "vm_get_config_item", needsSelf = false)
    public abstract static class VMGetConfigItemPrimitiveNode extends RubiniusPrimitiveNode {

        public VMGetConfigItemPrimitiveNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @TruffleBoundary
        @Specialization(guards = "isRubyString(key)")
        public Object get(DynamicObject key) {
            final Object value = getContext().getRubiniusConfiguration().get(key.toString());

            if (value == null) {
                return nil();
            } else {
                return value;
            }
        }

    }

    @RubiniusPrimitive(name = "vm_get_config_section", needsSelf = false)
    public abstract static class VMGetConfigSectionPrimitiveNode extends RubiniusPrimitiveNode {

        public VMGetConfigSectionPrimitiveNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @TruffleBoundary
        @Specialization(guards = "isRubyString(section)")
        public DynamicObject getSection(DynamicObject section) {
            final List<DynamicObject> sectionKeyValues = new ArrayList<>();

            for (String key : getContext().getRubiniusConfiguration().getSection(section.toString())) {
                Object value = getContext().getRubiniusConfiguration().get(key);
                final String stringValue;
                if (RubyGuards.isRubyBignum(value)) {
                    stringValue = BignumNodes.getBigIntegerValue((DynamicObject) value).toString();
                } else {
                    // This toString() is fine as we only have boolean, int, long and RubyString in config.
                    stringValue = value.toString();
                }

                sectionKeyValues.add(ArrayNodes.fromObjects(getContext().getCoreLibrary().getArrayClass(),
                        createString(key),
                        createString(stringValue)));
            }

            return ArrayNodes.fromObjects(getContext().getCoreLibrary().getArrayClass(), sectionKeyValues.toArray());
        }

    }

    @RubiniusPrimitive(name = "vm_wait_pid", needsSelf = false)
    public abstract static class VMWaitPidPrimitiveNode extends RubiniusPrimitiveNode {

        public VMWaitPidPrimitiveNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @TruffleBoundary
        @Specialization
        public Object waitPID(final int input_pid, boolean no_hang) {
            // Transliterated from Rubinius C++ - not tidied up significantly to make merging changes easier

            int options = 0;
            final int[] statusReference = new int[] { 0 };
            int pid;

            if (no_hang) {
                options |= WNOHANG.intValue();
            }

            final int finalOptions = options;

            // retry:
            pid = getContext().getThreadManager().runUntilResult(new ThreadManager.BlockingAction<Integer>() {
                @Override
                public Integer block() throws InterruptedException {
                    return posix().waitpid(input_pid, statusReference, finalOptions);
                }
            });

            final int errno = posix().errno();

            if (pid == -1) {
                if (errno == ECHILD.intValue()) {
                    return false;
                }
                if (errno == EINTR.intValue()) {
                    throw new UnsupportedOperationException();
                    //if(!state->check_async(calling_environment)) return NULL;
                    //goto retry;
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

            return ArrayNodes.fromObjects(getContext().getCoreLibrary().getArrayClass(), output, termsig, stopsig, pid);
        }

    }

    @RubiniusPrimitive(name = "vm_set_class", needsSelf = false)
    public abstract static class VMSetClassPrimitiveNode extends RubiniusPrimitiveNode {

        public VMSetClassPrimitiveNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization(guards = "isRubyClass(newClass)")
        public DynamicObject setClass(DynamicObject object, DynamicObject newClass) {
            BasicObjectNodes.BASIC_OBJECT_LAYOUT.setLogicalClass(object, newClass);
            BasicObjectNodes.BASIC_OBJECT_LAYOUT.setMetaClass(object, newClass);
            return object;
        }

    }

}
