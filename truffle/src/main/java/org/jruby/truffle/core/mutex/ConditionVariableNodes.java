/*
 * Copyright (c) 2015, 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.core.mutex;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.CreateCast;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.NodeChildren;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.source.SourceSection;
import org.jruby.truffle.Layouts;
import org.jruby.truffle.RubyContext;
import org.jruby.truffle.builtins.CoreClass;
import org.jruby.truffle.builtins.CoreMethod;
import org.jruby.truffle.builtins.CoreMethodArrayArgumentsNode;
import org.jruby.truffle.builtins.CoreMethodNode;
import org.jruby.truffle.builtins.UnaryCoreMethodNode;
import org.jruby.truffle.core.cast.DurationToMillisecondsNodeGen;
import org.jruby.truffle.core.thread.ThreadManager.BlockingAction;
import org.jruby.truffle.language.RubyNode;
import org.jruby.truffle.language.control.RaiseException;
import org.jruby.truffle.language.objects.AllocateObjectNode;

import java.util.concurrent.locks.ReentrantLock;

@CoreClass("ConditionVariable")
public abstract class ConditionVariableNodes {

    private static ConditionVariableObject getCondition(DynamicObject conditionVariable) {
        return Layouts.CONDITION_VARIABLE.getCondition(conditionVariable);
    }

    @CoreMethod(names = "allocate", constructor = true)
    public abstract static class AllocateNode extends CoreMethodArrayArgumentsNode {

        @Child private AllocateObjectNode allocateNode;

        public AllocateNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            allocateNode = AllocateObjectNode.create();
        }

        @Specialization
        public DynamicObject allocate(DynamicObject rubyClass) {
            return allocateNode.allocate(rubyClass, new ConditionVariableObject());
        }

    }

    @NodeChildren({
            @NodeChild(value = "conditionVariable", type = RubyNode.class),
            @NodeChild(value = "mutex", type = RubyNode.class),
            @NodeChild(value = "duration", type = RubyNode.class)
    })
    @CoreMethod(names = "wait", required = 1, optional = 1)
    public abstract static class WaitNode extends CoreMethodNode {

        @CreateCast("duration")
        public RubyNode coerceDuration(RubyNode duration) {
            return DurationToMillisecondsNodeGen.create(true, duration);
        }

        @Specialization(guards = "isRubyMutex(mutex)")
        public DynamicObject wait(DynamicObject conditionVariable, DynamicObject mutex, long timeoutInMillis) {
            final ReentrantLock lock = Layouts.MUTEX.getLock(mutex);
            final DynamicObject thread = getContext().getThreadManager().getCurrentThread();
            final ConditionVariableObject condition = getCondition(conditionVariable);

            doWait(timeoutInMillis, lock, thread, condition);

            return conditionVariable;
        }

        @TruffleBoundary
        private void doWait(
                final long durationInMillis,
                ReentrantLock lock,
                DynamicObject thread,
                Object condition) {

            final long start = System.currentTimeMillis();
            boolean doLock = false;

            try {
                // First acquire the condition monitor, so we only release the Mutex
                // when we own the condition monitor blocking notify/notifyAll calls until
                // we are in wait(), ready to be notified.
                synchronized (condition) {
                    MutexOperations.unlock(lock, thread, this);
                    // successfully unlocked, do lock later
                    doLock = true;
                    getContext().getThreadManager().
                            runUntilResult(this, new BlockingAction<Boolean>() {
                                @Override
                                public Boolean block() throws InterruptedException {
                                    long now = System.currentTimeMillis();
                                    long slept = now - start;

                                    if (slept >= durationInMillis) {
                                        return SUCCESS;
                                    }

                                    condition.wait(durationInMillis - slept);
                                    return SUCCESS;
                                }
                            });
                }
            } finally {
                // Has to lock again *after* condition lock is released, otherwise it would be
                // locked in reverse order condition > mutex opposed to normal locking order
                // mutex > condition (as in signal, broadcast), which would lead to deadlock.
                if (doLock) {
                    MutexOperations.lock(lock, thread, this);
                }
            }
        }
    }

    @CoreMethod(names = "signal")
    public abstract static class SignalNode extends UnaryCoreMethodNode {

        @TruffleBoundary
        @Specialization
        public DynamicObject doSignal(DynamicObject conditionVariable) {
            final ConditionVariableObject condition = getCondition(conditionVariable);
            synchronized (condition) {
                condition.notify();
            }
            return conditionVariable;
        }

    }

    @CoreMethod(names = "broadcast")
    public abstract static class BroadcastNode extends UnaryCoreMethodNode {

        @TruffleBoundary
        @Specialization
        public DynamicObject doBroadcast(DynamicObject conditionVariable) {
            final ConditionVariableObject condition = getCondition(conditionVariable);
            synchronized (condition) {
                condition.notifyAll();
            }
            return conditionVariable;
        }

    }

    @CoreMethod(names = "marshal_dump")
    public abstract static class MarshalDumpNode extends CoreMethodArrayArgumentsNode {

        @Specialization
        public Object marshal_dump(DynamicObject self) {
            throw new RaiseException(coreExceptions().typeErrorCantDump(self, this));
        }

    }

}
