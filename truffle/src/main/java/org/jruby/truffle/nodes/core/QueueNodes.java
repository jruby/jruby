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
import org.jruby.truffle.nodes.RubyNode;
import org.jruby.truffle.nodes.cast.BooleanCastWithDefaultNodeGen;
import org.jruby.truffle.nodes.objects.Allocator;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.control.RaiseException;
import org.jruby.truffle.runtime.core.RubyBasicObject;
import org.jruby.truffle.runtime.core.RubyClass;
import org.jruby.truffle.runtime.object.BasicObjectType;
import org.jruby.truffle.runtime.subsystems.ThreadManager.BlockingActionWithoutGlobalLock;
import org.jruby.util.unsafe.UnsafeHolder;

import java.util.EnumSet;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

@CoreClass(name = "Queue")
public abstract class QueueNodes {

    private static class QueueType extends BasicObjectType {
    }

    public static final QueueType QUEUE_TYPE = new QueueType();

    private static final HiddenKey QUEUE_IDENTIFIER = new HiddenKey("queue");
    private static final Property QUEUE_PROPERTY;
    private static final DynamicObjectFactory QUEUE_FACTORY;

    static {
        Shape.Allocator allocator = RubyBasicObject.LAYOUT.createAllocator();
        QUEUE_PROPERTY = Property.create(QUEUE_IDENTIFIER, allocator.locationForType(LinkedBlockingQueue.class, EnumSet.of(LocationModifier.Final, LocationModifier.NonNull)), 0);
        Shape shape = RubyBasicObject.LAYOUT.createShape(QUEUE_TYPE).addProperty(QUEUE_PROPERTY);
        QUEUE_FACTORY = shape.createFactory();
    }

    public static class QueueAllocator implements Allocator {
        @Override
        public RubyBasicObject allocate(RubyContext context, RubyClass rubyClass, Node currentNode) {
            return new RubyBasicObject(rubyClass, QUEUE_FACTORY.newInstance(new LinkedBlockingQueue<Object>()));
        }
    }

    @SuppressWarnings("unchecked")
    private static BlockingQueue<Object> getQueue(RubyBasicObject queue) {
        assert queue.getDynamicObject().getShape().hasProperty(QUEUE_IDENTIFIER);
        return (BlockingQueue<Object>) QUEUE_PROPERTY.get(queue.getDynamicObject(), true);
    }

    @CoreMethod(names = { "push", "<<", "enq" }, required = 1)
    public abstract static class PushNode extends CoreMethodArrayArgumentsNode {

        public PushNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public RubyBasicObject push(RubyBasicObject self, final Object value) {
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

            getContext().getThreadManager().runUntilResult(new BlockingActionWithoutGlobalLock<Boolean>() {
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
