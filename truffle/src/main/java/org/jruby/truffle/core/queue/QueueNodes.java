/*
 * Copyright (c) 2015, 2017 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.core.queue;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.CreateCast;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.NodeChildren;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.profiles.BranchProfile;
import org.jruby.truffle.Layouts;
import org.jruby.truffle.builtins.CoreClass;
import org.jruby.truffle.builtins.CoreMethod;
import org.jruby.truffle.builtins.CoreMethodArrayArgumentsNode;
import org.jruby.truffle.builtins.CoreMethodNode;
import org.jruby.truffle.builtins.NonStandard;
import org.jruby.truffle.core.cast.BooleanCastWithDefaultNodeGen;
import org.jruby.truffle.core.thread.ThreadManager.BlockingAction;
import org.jruby.truffle.language.RubyNode;
import org.jruby.truffle.language.Visibility;
import org.jruby.truffle.language.control.RaiseException;
import org.jruby.truffle.language.objects.AllocateObjectNode;
import org.jruby.truffle.language.objects.shared.PropagateSharingNode;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

@CoreClass("Queue")
public abstract class QueueNodes {

    @CoreMethod(names = "allocate", constructor = true)
    public abstract static class AllocateNode extends CoreMethodArrayArgumentsNode {

        @Child private AllocateObjectNode allocateNode = AllocateObjectNode.create();

        @Specialization
        public DynamicObject allocate(DynamicObject rubyClass) {
            return allocateNode.allocate(rubyClass, getContext().getNativePlatform().createLinkedBlockingQueueLocksConditions());
        }

    }

    @CoreMethod(names = { "push", "<<", "enq" }, required = 1)
    public abstract static class PushNode extends CoreMethodArrayArgumentsNode {

        @Child private PropagateSharingNode propagateSharingNode = PropagateSharingNode.create();

        @Specialization
        public DynamicObject push(DynamicObject self, final Object value) {
            final BlockingQueue<Object> queue = Layouts.QUEUE.getQueue(self);

            propagateSharingNode.propagate(self, value);
            doPush(value, queue);

            return self;
        }

        @TruffleBoundary
        private void doPush(final Object value, final BlockingQueue<Object> queue) {
            queue.add(value);
        }

    }

    @CoreMethod(names = { "pop", "shift", "deq" }, optional = 1)
    @NodeChildren({
            @NodeChild(type = RubyNode.class, value = "queue"),
            @NodeChild(type = RubyNode.class, value = "nonBlocking")
    })
    public abstract static class PopNode extends CoreMethodNode {

        @CreateCast("nonBlocking")
        public RubyNode coerceToBoolean(RubyNode nonBlocking) {
            return BooleanCastWithDefaultNodeGen.create(false, nonBlocking);
        }

        @Specialization(guards = "!nonBlocking")
        public Object popBlocking(DynamicObject self, boolean nonBlocking) {
            final BlockingQueue<Object> queue = Layouts.QUEUE.getQueue(self);

            return doPop(queue);
        }

        @TruffleBoundary
        private Object doPop(final BlockingQueue<Object> queue) {
            return getContext().getThreadManager().runUntilResult(this, () -> queue.take());
        }

        @Specialization(guards = "nonBlocking")
        public Object popNonBlock(DynamicObject self, boolean nonBlocking,
                @Cached("create()") BranchProfile errorProfile) {
            final BlockingQueue<Object> queue = Layouts.QUEUE.getQueue(self);

            final Object value = doPoll(queue);
            if (value == null) {
                errorProfile.enter();
                throw new RaiseException(coreExceptions().threadError("queue empty", this));
            }

            return value;
        }

        @TruffleBoundary
        private Object doPoll(final BlockingQueue<Object> queue) {
            return queue.poll();
        }

    }

    @NonStandard
    @CoreMethod(names = "receive_timeout", required = 1, visibility = Visibility.PRIVATE, lowerFixnum = 1)
    @NodeChildren({
            @NodeChild(type = RubyNode.class, value = "queue"),
            @NodeChild(type = RubyNode.class, value = "duration")
    })
    public abstract static class ReceiveTimeoutNode extends CoreMethodNode {

        @Specialization
        public Object receiveTimeout(DynamicObject self, int duration) {
            return receiveTimeout(self, (double) duration);
        }

        @Specialization
        public Object receiveTimeout(DynamicObject self, double duration) {
            final BlockingQueue<Object> queue = Layouts.QUEUE.getQueue(self);

            final long durationInMillis = (long) (duration * 1000.0);
            final long start = System.currentTimeMillis();

            return getContext().getThreadManager().runUntilResult(this, () -> {
                long now = System.currentTimeMillis();
                long waited = now - start;
                if (waited >= durationInMillis) {
                    // Try again to make sure we at least tried once
                    final Object result = queue.poll();
                    if (result == null) {
                        return false;
                    } else {
                        return result;
                    }
                }

                final Object result = queue.poll(durationInMillis, TimeUnit.MILLISECONDS);
                if (result == null) {
                    return false;
                } else {
                    return result;
                }
            });
        }

    }

    @CoreMethod(names = "empty?")
    public abstract static class EmptyNode extends CoreMethodArrayArgumentsNode {

        @TruffleBoundary
        @Specialization
        public boolean empty(DynamicObject self) {
            final BlockingQueue<Object> queue = Layouts.QUEUE.getQueue(self);
            return queue.isEmpty();
        }

    }

    @CoreMethod(names = { "size", "length" })
    public abstract static class SizeNode extends CoreMethodArrayArgumentsNode {

        @TruffleBoundary
        @Specialization
        public int size(DynamicObject self) {
            final BlockingQueue<Object> queue = Layouts.QUEUE.getQueue(self);
            return queue.size();
        }

    }

    @CoreMethod(names = "clear")
    public abstract static class ClearNode extends CoreMethodArrayArgumentsNode {

        @TruffleBoundary
        @Specialization
        public DynamicObject clear(DynamicObject self) {
            final BlockingQueue<Object> queue = Layouts.QUEUE.getQueue(self);
            queue.clear();
            return self;
        }

    }

    @CoreMethod(names = "marshal_dump")
    public abstract static class MarshalDumpNode extends CoreMethodArrayArgumentsNode {

        @Specialization
        public Object marshal_dump(DynamicObject self) {
            throw new RaiseException(coreExceptions().typeErrorCantDump(self, this));
        }

    }

    @CoreMethod(names = "num_waiting")
    public abstract static class NumWaitingNode extends CoreMethodArrayArgumentsNode {

        @Specialization
        public int num_waiting(DynamicObject self) {
            final LinkedBlockingQueueLocksConditions<Object> queue = Layouts.QUEUE.getQueue(self);

            final ReentrantLock lock = queue.getLock();

            getContext().getThreadManager().runUntilResult(this, new BlockingAction<Boolean>() {
                @Override
                public Boolean block() throws InterruptedException {
                    lock.lockInterruptibly();
                    return SUCCESS;
                }
            });
            try {
                return lock.getWaitQueueLength(queue.getNotEmptyCondition());
            } finally {
                lock.unlock();
            }
        }

    }

}
