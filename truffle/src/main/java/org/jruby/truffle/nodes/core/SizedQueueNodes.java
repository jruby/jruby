/*
 * Copyright (c) 2015 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.nodes.core;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.CreateCast;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.NodeChildren;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.object.*;
import com.oracle.truffle.api.source.SourceSection;
import org.jruby.runtime.Visibility;
import org.jruby.truffle.nodes.RubyNode;
import org.jruby.truffle.nodes.cast.BooleanCastWithDefaultNodeGen;
import org.jruby.truffle.nodes.objects.Allocator;
import org.jruby.truffle.om.dsl.api.Nullable;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.control.RaiseException;
import org.jruby.truffle.runtime.core.RubyBasicObject;
import org.jruby.truffle.runtime.subsystems.ThreadManager.BlockingActionWithoutGlobalLock;
import org.jruby.util.unsafe.UnsafeHolder;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * We do not reuse much of class Queue since we need to be able to replace the queue in this case
 * and methods are small anyway.
 */
@CoreClass(name = "SizedQueue")
public abstract class SizedQueueNodes {

    @org.jruby.truffle.om.dsl.api.Layout
    public interface SizedQueueLayout extends BasicObjectNodes.BasicObjectLayout {

        DynamicObject createSizedQueue(RubyBasicObject logicalClass, RubyBasicObject metaClass, @Nullable BlockingQueue queue);

        boolean isSizedQueue(DynamicObject object);

        @Nullable
        BlockingQueue getQueue(DynamicObject object);

        @Nullable
        void setQueue(DynamicObject object, BlockingQueue queue);

    }

    public static final SizedQueueLayout SIZED_QUEUE_LAYOUT = SizedQueueLayoutImpl.INSTANCE;

    public static class SizedQueueAllocator implements Allocator {
        @Override
        public RubyBasicObject allocate(RubyContext context, RubyBasicObject rubyClass, Node currentNode) {
            return BasicObjectNodes.createRubyBasicObject(rubyClass, SIZED_QUEUE_LAYOUT.createSizedQueue(rubyClass, rubyClass, null));
        }
    }

    @SuppressWarnings("unchecked")
    private static BlockingQueue<Object> getQueue(RubyBasicObject sizedQueue) {
        return SIZED_QUEUE_LAYOUT.getQueue(BasicObjectNodes.getDynamicObject(sizedQueue));
    }

    private static void setQueue(RubyBasicObject sizedQueue, BlockingQueue<Object> value) {
        // TODO (eregon, 12 July 2015): CAS when swapping the queue?
        SIZED_QUEUE_LAYOUT.setQueue(BasicObjectNodes.getDynamicObject(sizedQueue), value);
    }

    @CoreMethod(names = "initialize", visibility = Visibility.PRIVATE, required = 1)
    public abstract static class InitializeNode extends CoreMethodArrayArgumentsNode {

        public InitializeNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public RubyBasicObject initialize(RubyBasicObject self, int capacity) {
            if (capacity <= 0) {
                CompilerDirectives.transferToInterpreter();
                throw new RaiseException(getContext().getCoreLibrary().argumentError("queue size must be positive", this));
            }

            final BlockingQueue<Object> blockingQueue = new ArrayBlockingQueue<Object>(capacity);
            setQueue(self, blockingQueue);
            return self;
        }

    }

    @CoreMethod(names = "max=", required = 1)
    public abstract static class SetMaxNode extends CoreMethodArrayArgumentsNode {

        public SetMaxNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public int setMax(RubyBasicObject self, int newCapacity) {
            if (newCapacity <= 0) {
                CompilerDirectives.transferToInterpreter();
                throw new RaiseException(getContext().getCoreLibrary().argumentError("queue size must be positive", this));
            }

            final BlockingQueue<Object> oldQueue = getQueue(self);
            final BlockingQueue<Object> newQueue = new ArrayBlockingQueue<Object>(newCapacity);

            // TODO (eregon, 12 July 2015): racy and what to do if the new capacity is lower?
            Object element;
            while ((element = oldQueue.poll()) != null) {
                newQueue.add(element);
            }
            setQueue(self, newQueue);

            return newCapacity;
        }

    }

    @CoreMethod(names = "max")
    public abstract static class MaxNode extends CoreMethodArrayArgumentsNode {

        public MaxNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public int max(RubyBasicObject self) {
            final BlockingQueue<Object> queue = getQueue(self);

            // TODO (eregon, 12 July 2015): We could be more accurate here and remember the capacity ourselves
            return queue.size() + queue.remainingCapacity();
        }

    }

    @CoreMethod(names = { "push", "<<", "enq" }, required = 1, optional = 1)
    @NodeChildren({
            @NodeChild(type = RubyNode.class, value = "queue"),
            @NodeChild(type = RubyNode.class, value = "value"),
            @NodeChild(type = RubyNode.class, value = "nonBlocking")
    })
    public abstract static class PushNode extends CoreMethodNode {

        public PushNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @CreateCast("nonBlocking")
        public RubyNode coerceToBoolean(RubyNode nonBlocking) {
            return BooleanCastWithDefaultNodeGen.create(getContext(), getSourceSection(), false, nonBlocking);
        }

        @Specialization(guards = "!nonBlocking")
        public RubyBasicObject pushBlocking(RubyBasicObject self, final Object value, boolean nonBlocking) {
            final BlockingQueue<Object> queue = getQueue(self);

            getContext().getThreadManager().runUntilResult(new BlockingActionWithoutGlobalLock<Boolean>() {
                @Override
                public Boolean block() throws InterruptedException {
                    queue.put(value);
                    return SUCCESS;
                }
            });

            return self;
        }

        @Specialization(guards = "nonBlocking")
        public RubyBasicObject pushNonBlock(RubyBasicObject self, final Object value, boolean nonBlocking) {
            final BlockingQueue<Object> queue = getQueue(self);

            final boolean pushed = queue.offer(value);
            if (!pushed) {
                CompilerDirectives.transferToInterpreter();
                throw new RaiseException(getContext().getCoreLibrary().threadError("queue full", this));
            }

            return self;
        }

    }

    @CoreMethod(names = { "pop", "shift", "deq" }, optional = 1)
    @NodeChildren({
            @NodeChild(type = RubyNode.class, value = "queue"),
            @NodeChild(type = RubyNode.class, value = "nonBlocking")
    })
    public abstract static class PopNode extends CoreMethodNode {

        public PopNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @CreateCast("nonBlocking")
        public RubyNode coerceToBoolean(RubyNode nonBlocking) {
            return BooleanCastWithDefaultNodeGen.create(getContext(), getSourceSection(), false, nonBlocking);
        }

        @Specialization(guards = "!nonBlocking")
        public Object popBlocking(RubyBasicObject self, boolean nonBlocking) {
            final BlockingQueue<Object> queue = getQueue(self);

            return getContext().getThreadManager().runUntilResult(new BlockingActionWithoutGlobalLock<Object>() {
                @Override
                public Object block() throws InterruptedException {
                    return queue.take();
                }
            });
        }

        @Specialization(guards = "nonBlocking")
        public Object popNonBlock(RubyBasicObject self, boolean nonBlocking) {
            final BlockingQueue<Object> queue = getQueue(self);

            final Object value = queue.poll();
            if (value == null) {
                CompilerDirectives.transferToInterpreter();
                throw new RaiseException(getContext().getCoreLibrary().threadError("queue empty", this));
            }

            return value;
        }

    }

    @CoreMethod(names = "empty?")
    public abstract static class EmptyNode extends CoreMethodArrayArgumentsNode {

        public EmptyNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public boolean empty(RubyBasicObject self) {
            final BlockingQueue<Object> queue = getQueue(self);
            return queue.isEmpty();
        }

    }

    @CoreMethod(names = { "size", "length" })
    public abstract static class SizeNode extends CoreMethodArrayArgumentsNode {

        public SizeNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public int size(RubyBasicObject self) {
            final BlockingQueue<Object> queue = getQueue(self);
            return queue.size();
        }

    }

    @CoreMethod(names = "clear")
    public abstract static class ClearNode extends CoreMethodArrayArgumentsNode {

        public ClearNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public RubyBasicObject clear(RubyBasicObject self) {
            final BlockingQueue<Object> queue = getQueue(self);
            queue.clear();
            return self;
        }

    }

    @CoreMethod(names = "num_waiting")
    public abstract static class NumWaitingNode extends CoreMethodArrayArgumentsNode {

        private static final long LOCK_FIELD_OFFSET = UnsafeHolder.fieldOffset(ArrayBlockingQueue.class, "lock");
        private static final long NOT_EMPTY_CONDITION_FIELD_OFFSET = UnsafeHolder.fieldOffset(ArrayBlockingQueue.class, "notEmpty");
        private static final long NOT_FULL_CONDITION_FIELD_OFFSET = UnsafeHolder.fieldOffset(ArrayBlockingQueue.class, "notFull");

        public NumWaitingNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @SuppressWarnings("restriction")
        @Specialization
        public int num_waiting(RubyBasicObject self) {
            final BlockingQueue<Object> queue = getQueue(self);

            final ArrayBlockingQueue<Object> arrayBlockingQueue = (ArrayBlockingQueue<Object>) queue;
            final ReentrantLock lock = (ReentrantLock) UnsafeHolder.U.getObject(arrayBlockingQueue, LOCK_FIELD_OFFSET);
            final Condition notEmptyCondition = (Condition) UnsafeHolder.U.getObject(arrayBlockingQueue, NOT_EMPTY_CONDITION_FIELD_OFFSET);
            final Condition notFullCondition = (Condition) UnsafeHolder.U.getObject(arrayBlockingQueue, NOT_FULL_CONDITION_FIELD_OFFSET);

            getContext().getThreadManager().runUntilResult(new BlockingActionWithoutGlobalLock<Boolean>() {
                @Override
                public Boolean block() throws InterruptedException {
                    lock.lockInterruptibly();
                    return SUCCESS;
                }
            });
            try {
                return lock.getWaitQueueLength(notEmptyCondition) + lock.getWaitQueueLength(notFullCondition);
            } finally {
                lock.unlock();
            }
        }

    }

}
