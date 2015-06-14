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
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.object.*;
import com.oracle.truffle.api.source.SourceSection;
import org.jruby.truffle.nodes.objects.Allocator;
import org.jruby.truffle.runtime.NotProvided;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.control.RaiseException;
import org.jruby.truffle.runtime.core.RubyBasicObject;
import org.jruby.truffle.runtime.core.RubyClass;
import org.jruby.truffle.runtime.object.BasicObjectType;
import org.jruby.truffle.runtime.subsystems.ThreadManager.BlockingActionWithoutGlobalLock;

import java.util.EnumSet;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

@CoreClass(name = "ConditionVariable")
public abstract class ConditionVariableNodes {

    private static class ConditionVariableType extends BasicObjectType {
    }

    public static final ConditionVariableType CONDITION_VARIABLE_TYPE = new ConditionVariableType();

    private static final HiddenKey ASSOCIATED_MUTEX_IDENTIFIER = new HiddenKey("associated_mutex");
    private static final Property ASSOCIATED_MUTEX_PROPERTY;
    private static final HiddenKey CONDITION_IDENTIFIER = new HiddenKey("condition");
    private static final Property CONDITION_PROPERTY;
    private static final DynamicObjectFactory CONDITION_VARIABLE_FACTORY;

    static {
        Shape.Allocator allocator = RubyBasicObject.LAYOUT.createAllocator();
        ASSOCIATED_MUTEX_PROPERTY = Property.create(ASSOCIATED_MUTEX_IDENTIFIER,
                allocator.locationForType(AtomicReference.class, EnumSet.of(LocationModifier.Final, LocationModifier.NonNull)), 0);
        CONDITION_PROPERTY = Property.create(CONDITION_IDENTIFIER,
                allocator.locationForType(AtomicReference.class, EnumSet.of(LocationModifier.Final, LocationModifier.NonNull)), 0);
        Shape shape = RubyBasicObject.LAYOUT.createShape(CONDITION_VARIABLE_TYPE)
                .addProperty(ASSOCIATED_MUTEX_PROPERTY)
                .addProperty(CONDITION_PROPERTY);
        CONDITION_VARIABLE_FACTORY = shape.createFactory();
    }

    public static class ConditionVariableAllocator implements Allocator {
        @Override
        public RubyBasicObject allocate(RubyContext context, RubyClass rubyClass, Node currentNode) {
            return new RubyBasicObject(rubyClass, CONDITION_VARIABLE_FACTORY.newInstance(new AtomicReference<RubyBasicObject>(), new AtomicReference<Condition>()));
        }
    }

    @SuppressWarnings("unchecked")
    protected static AtomicReference<RubyBasicObject> getAssociatedMutex(RubyBasicObject mutex) {
        assert mutex.getDynamicObject().getShape().hasProperty(ASSOCIATED_MUTEX_IDENTIFIER);
        return (AtomicReference<RubyBasicObject>) ASSOCIATED_MUTEX_PROPERTY.get(mutex.getDynamicObject(), true);
    }

    @SuppressWarnings("unchecked")
    protected static AtomicReference<Condition> getCondition(RubyBasicObject conditionVariable) {
        assert conditionVariable.getDynamicObject().getShape().hasProperty(CONDITION_IDENTIFIER);
        return (AtomicReference<Condition>) CONDITION_PROPERTY.get(conditionVariable.getDynamicObject(), true);
    }

    @CoreMethod(names = "broadcast")
    public abstract static class BroadcastNode extends UnaryCoreMethodNode {

        public BroadcastNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public RubyBasicObject broadcast(RubyBasicObject conditionVariable) {
            final Condition condition = getCondition(conditionVariable).get();
            final RubyBasicObject associatedMutex = getAssociatedMutex(conditionVariable).get();

            if (condition == null) {
                return conditionVariable;
            }

            if (!MutexNodes.getLock(associatedMutex).isHeldByCurrentThread()) {
                CompilerDirectives.transferToInterpreter();
                throw new RaiseException(getContext().getCoreLibrary().threadError("Called ConditionVariable#broadcast without holding associated Mutex", this));
            }

            condition.signalAll();

            return conditionVariable;
        }

    }

    @CoreMethod(names = "signal")
    public abstract static class SignalNode extends UnaryCoreMethodNode {

        public SignalNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public RubyBasicObject signal(RubyBasicObject conditionVariable) {
            final Condition condition = getCondition(conditionVariable).get();
            final RubyBasicObject associatedMutex = getAssociatedMutex(conditionVariable).get();

            if (condition == null) {
                return conditionVariable;
            }

            if (!MutexNodes.getLock(associatedMutex).isHeldByCurrentThread()) {
                CompilerDirectives.transferToInterpreter();
                throw new RaiseException(getContext().getCoreLibrary().threadError("Called ConditionVariable#signal without holding associated Mutex", this));
            }

            condition.signal();

            return conditionVariable;
        }

    }

    @CoreMethod(names = "wait", required = 1, optional = 1)
    public abstract static class WaitNode extends CoreMethodArrayArgumentsNode {

        public WaitNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        private static interface WaitAction {
            void wait(Condition condition) throws InterruptedException;
        }

        @Specialization(guards = "isRubyMutex(mutex)")
        RubyBasicObject wait(RubyBasicObject conditionVariable, RubyBasicObject mutex, NotProvided timeout) {
            return wait(conditionVariable, mutex, nil());
        }

        @Specialization(guards = { "isRubyMutex(mutex)", "isNil(timeout)" })
        RubyBasicObject wait(RubyBasicObject conditionVariable, RubyBasicObject mutex, RubyBasicObject timeout) {
            return waitOn(conditionVariable, mutex, new WaitAction() {
                @Override
                public void wait(Condition condition) throws InterruptedException {
                    condition.await();
                }
            });
        }

        @Specialization(guards = "isRubyMutex(mutex)")
        RubyBasicObject wait(RubyBasicObject conditionVariable, RubyBasicObject mutex, final int timeout) {
            return wait(conditionVariable, mutex, (double) timeout);
        }

        @Specialization(guards = "isRubyMutex(mutex)")
        RubyBasicObject wait(RubyBasicObject conditionVariable, RubyBasicObject mutex, final double timeout) {
            final long timeoutInNanos = ((long) (timeout * 1_000_000_000));

            return waitOn(conditionVariable, mutex, new WaitAction() {
                private long remaining = timeoutInNanos;

                @Override
                public void wait(Condition condition) throws InterruptedException {
                    while (remaining > 0) {
                        remaining = condition.awaitNanos(remaining);
                    }
                }
            });
        }

        private RubyBasicObject waitOn(RubyBasicObject conditionVariable, RubyBasicObject mutex, final WaitAction waitAction) {
            final AtomicReference<RubyBasicObject> associatedMutexReference = getAssociatedMutex(conditionVariable);
            final AtomicReference<Condition> conditionReference = getCondition(conditionVariable);

            final Condition condition;
            if (associatedMutexReference.compareAndSet(null, mutex)) {
                final ReentrantLock lock = MutexNodes.getLock(mutex);
                condition = lock.newCondition();
                conditionReference.set(condition);
            } else if (associatedMutexReference.get() == mutex) {
                condition = conditionReference.get();
            } else {
                CompilerDirectives.transferToInterpreter();
                throw new RaiseException(getContext().getCoreLibrary().threadError("Attempt to associate a ConditionVariable which already has a Mutex", this));
            }

            if (!MutexNodes.getLock(associatedMutexReference.get()).isHeldByCurrentThread()) {
                CompilerDirectives.transferToInterpreter();
                throw new RaiseException(getContext().getCoreLibrary().threadError("Called ConditionVariable#wait without holding associated Mutex", this));
            }

            getContext().getThreadManager().runUntilResult(new BlockingActionWithoutGlobalLock<Boolean>() {
                @Override
                public Boolean block() throws InterruptedException {
                    waitAction.wait(condition);
                    return SUCCESS;
                }
            });

            return conditionVariable;
        }

    }

}
