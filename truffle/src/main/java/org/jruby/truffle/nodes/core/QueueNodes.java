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
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
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
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.control.RaiseException;
import org.jruby.truffle.runtime.core.RubyBasicObject;
import org.jruby.truffle.runtime.subsystems.ThreadManager.BlockingAction;
import org.jruby.util.unsafe.UnsafeHolder;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

@CoreClass(name = "Queue")
public abstract class QueueNodes {

    @org.jruby.truffle.om.dsl.api.Layout
    public interface QueueLayout extends BasicObjectNodes.BasicObjectLayout {

        DynamicObjectFactory createQueueShape(RubyBasicObject logicalClass, RubyBasicObject metaClass);

        DynamicObject createQueue(DynamicObjectFactory factory, LinkedBlockingQueue queue);

        boolean isQueue(DynamicObject object);

        LinkedBlockingQueue getQueue(DynamicObject object);

    }

    public static final QueueLayout QUEUE_LAYOUT = QueueLayoutImpl.INSTANCE;

    public static class QueueAllocator implements Allocator {
        @Override
        public RubyBasicObject allocate(RubyContext context, RubyBasicObject rubyClass, Node currentNode) {
            return BasicObjectNodes.createRubyBasicObject(rubyClass, QUEUE_LAYOUT.createQueue(ModuleNodes.getModel(rubyClass).factory, new LinkedBlockingQueue()));
        }
    }

    @SuppressWarnings("unchecked")
    private static BlockingQueue getQueue(RubyBasicObject queue) {
        return QUEUE_LAYOUT.getQueue(BasicObjectNodes.getDynamicObject(queue));
    }

    @CoreMethod(names = { "push", "<<", "enq" }, required = 1)
    public abstract static class PushNode extends CoreMethodArrayArgumentsNode {

        public PushNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public RubyBasicObject push(RubyBasicObject self, final Object value) {
            final BlockingQueue<Object> queue = getQueue(self);

            queue.add(value);
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

            return getContext().getThreadManager().runUntilResult(new BlockingAction<Object>() {
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

    @RubiniusOnly
    @CoreMethod(names = "receive_timeout", required = 1, visibility = Visibility.PRIVATE)
    @NodeChildren({
            @NodeChild(type = RubyNode.class, value = "queue"),
            @NodeChild(type = RubyNode.class, value = "duration")
    })
    public abstract static class ReceiveTimeoutNode extends CoreMethodNode {

        public ReceiveTimeoutNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public Object receiveTimeout(RubyBasicObject self, int duration) {
            return receiveTimeout(self, (double) duration);
        }

        @Specialization
        public Object receiveTimeout(RubyBasicObject self, double duration) {
            final BlockingQueue<Object> queue = getQueue(self);

            final long durationInMillis = (long) (duration * 1000.0);
            final long start = System.currentTimeMillis();

            return getContext().getThreadManager().runUntilResult(new BlockingAction<Object>() {
                @Override
                public Object block() throws InterruptedException {
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
                }
            });
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

    @CoreMethod(names = "marshal_dump")
    public abstract static class MarshalDumpNode extends CoreMethodArrayArgumentsNode {

        public MarshalDumpNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        @TruffleBoundary
        public Object marshal_dump(RubyBasicObject self) {
            throw new RaiseException(getContext().getCoreLibrary().typeErrorCantDump(self, this));
        }

    }

    @CoreMethod(names = "num_waiting")
    public abstract static class NumWaitingNode extends CoreMethodArrayArgumentsNode {

        private static final long LOCK_FIELD_OFFSET = UnsafeHolder.fieldOffset(LinkedBlockingQueue.class, "takeLock");
        private static final long NOT_EMPTY_CONDITION_FIELD_OFFSET = UnsafeHolder.fieldOffset(LinkedBlockingQueue.class, "notEmpty");

        public NumWaitingNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @SuppressWarnings("restriction")
        @Specialization
        public int num_waiting(RubyBasicObject self) {
            final BlockingQueue<Object> queue = getQueue(self);

            final LinkedBlockingQueue<Object> linkedBlockingQueue = (LinkedBlockingQueue<Object>) queue;
            final ReentrantLock lock = (ReentrantLock) UnsafeHolder.U.getObject(linkedBlockingQueue, LOCK_FIELD_OFFSET);
            final Condition notEmptyCondition = (Condition) UnsafeHolder.U.getObject(linkedBlockingQueue, NOT_EMPTY_CONDITION_FIELD_OFFSET);

            getContext().getThreadManager().runUntilResult(new BlockingAction<Boolean>() {
                @Override
                public Boolean block() throws InterruptedException {
                    lock.lockInterruptibly();
                    return SUCCESS;
                }
            });
            try {
                return lock.getWaitQueueLength(notEmptyCondition);
            } finally {
                lock.unlock();
            }
        }

    }

}
