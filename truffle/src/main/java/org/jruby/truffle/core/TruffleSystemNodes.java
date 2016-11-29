/*
 * Copyright (c) 2013, 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.core;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.object.DynamicObject;
import org.jcodings.Encoding;
import org.jcodings.specific.UTF8Encoding;
import org.jruby.ext.rbconfig.RbConfigLibrary;
import org.jruby.truffle.builtins.CoreClass;
import org.jruby.truffle.builtins.CoreMethod;
import org.jruby.truffle.builtins.CoreMethodNode;
import org.jruby.truffle.builtins.YieldingCoreMethodNode;
import org.jruby.truffle.core.string.StringOperations;
import org.jruby.util.unsafe.UnsafeHolder;

import java.util.Set;

@CoreClass("Truffle::System")
public abstract class TruffleSystemNodes {

    @CoreMethod(names = "initial_environment_variables", onSingleton = true)
    public abstract static class InitEnvVarsNode extends CoreMethodNode {

        @TruffleBoundary
        @Specialization
        public DynamicObject envVars() {
            final Set<String> variables = System.getenv().keySet();
            final int size = variables.size();
            final Encoding localeEncoding = getContext().getEncodingManager().getLocaleEncoding();
            final Object[] store = new Object[size];
            int i = 0;
            for (String variable : variables) {
                store[i++] = createString(StringOperations.encodeRope(variable, localeEncoding));
            }
            return createArray(store, size);
        }

    }

    @CoreMethod(names = "host_cpu", onSingleton = true)
    public abstract static class HostCPUNode extends CoreMethodNode {

        @Specialization
        public DynamicObject hostCPU() {
            return createString(StringOperations.encodeRope(RbConfigLibrary.getArchitecture(), UTF8Encoding.INSTANCE));
        }

    }

    @CoreMethod(names = "host_os", onSingleton = true)
    public abstract static class HostOSNode extends CoreMethodNode {

        @TruffleBoundary
        @Specialization
        public DynamicObject hostOS() {
            return createString(StringOperations.encodeRope(RbConfigLibrary.getOSName(), UTF8Encoding.INSTANCE));
        }

    }

    @CoreMethod(names = "synchronized", isModuleFunction = true, required = 1, needsBlock = true)
    public abstract static class SynchronizedPrimitiveNode extends YieldingCoreMethodNode {

        // We must not allow to synchronize on boxed primitives.
        @Specialization
        public Object synchronize(VirtualFrame frame, DynamicObject self, DynamicObject block) {
            synchronized (self) {
                return yield(frame, block);
            }
        }
    }

    @CoreMethod(names = "full_memory_barrier", isModuleFunction = true)
    public abstract static class FullMemoryBarrierPrimitiveNode extends CoreMethodNode {

        @Specialization
        public Object fullMemoryBarrier() {
            if (UnsafeHolder.SUPPORTS_FENCES) {
                UnsafeHolder.fullFence();
            } else {
                throw new UnsupportedOperationException();
            }
            return nil();
        }
    }

}
