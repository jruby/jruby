/*
 * Copyright (c) 2013, 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.language.objects;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.frame.Frame;
import com.oracle.truffle.api.frame.FrameInstance;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.Property;
import org.jruby.truffle.Layouts;
import org.jruby.truffle.RubyContext;
import org.jruby.truffle.core.hash.Entry;
import org.jruby.truffle.language.arguments.RubyArguments;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Deque;
import java.util.HashSet;
import java.util.Set;

public abstract class ObjectGraph {

    @TruffleBoundary
    public static Set<DynamicObject> stopAndGetAllObjects(Node currentNode, final RubyContext context) {
        final Set<DynamicObject> visited = new HashSet<>();

        final Thread stoppingThread = Thread.currentThread();

        context.getSafepointManager().pauseAllThreadsAndExecute(currentNode, false, (thread, currentNode1) -> {
            synchronized (visited) {
                final Deque<DynamicObject> stack = new ArrayDeque<>();

                // Thread.current
                stack.add(thread);
                // Fiber.current
                stack.add(Layouts.THREAD.getFiberManager(thread).getCurrentFiber());

                if (Thread.currentThread() == stoppingThread) {
                    visitContextRoots(context, stack);
                }

                Truffle.getRuntime().iterateFrames(frameInstance -> {
                    stack.addAll(getObjectsInFrame(frameInstance.getFrame(
                            FrameInstance.FrameAccess.READ_ONLY, true)));
                    return null;
                });

                while (!stack.isEmpty()) {
                    final DynamicObject object = stack.pop();

                    if (visited.add(object)) {
                        stack.addAll(ObjectGraph.getAdjacentObjects(object));
                    }
                }
            }
        });

        return visited;
    }

    @TruffleBoundary
    public static Set<DynamicObject> stopAndGetRootObjects(Node currentNode, final RubyContext context) {
        final Set<DynamicObject> objects = new HashSet<>();

        final Thread stoppingThread = Thread.currentThread();

        context.getSafepointManager().pauseAllThreadsAndExecute(currentNode, false, (thread, currentNode1) -> {
            objects.add(thread);

            if (Thread.currentThread() == stoppingThread) {
                visitContextRoots(context, objects);
            }
        });

        return objects;
    }

    public static void visitContextRoots(RubyContext context, Collection<DynamicObject> stack) {
        // We do not want to expose the global object
        stack.addAll(context.getCoreLibrary().getGlobalVariables().dynamicObjectValues());
        stack.addAll(context.getAtExitManager().getHandlers());
        stack.addAll(context.getObjectSpaceManager().getFinalizerHandlers());
    }

    public static Set<DynamicObject> getAdjacentObjects(DynamicObject object) {
        final Set<DynamicObject> reachable = new HashSet<>();

        if (Layouts.BASIC_OBJECT.isBasicObject(object)) {
            reachable.add(Layouts.BASIC_OBJECT.getLogicalClass(object));
            reachable.add(Layouts.BASIC_OBJECT.getMetaClass(object));
        }

        for (Property property : object.getShape().getPropertyListInternal(false)) {
            final Object propertyValue = property.get(object, object.getShape());

            if (propertyValue instanceof DynamicObject) {
                reachable.add((DynamicObject) propertyValue);
            } else if (propertyValue instanceof Entry[]) {
                for (Entry bucket : (Entry[]) propertyValue) {
                    while (bucket != null) {
                        if (bucket.getKey() instanceof DynamicObject) {
                            reachable.add((DynamicObject) bucket.getKey());
                        }

                        if (bucket.getValue() instanceof DynamicObject) {
                            reachable.add((DynamicObject) bucket.getValue());
                        }

                        bucket = bucket.getNextInLookup();
                    }
                }
            } else if (propertyValue instanceof Object[]) {
                for (Object element : (Object[]) propertyValue) {
                    if (element instanceof DynamicObject) {
                        reachable.add((DynamicObject) element);
                    }
                }
            } else if (propertyValue instanceof Collection<?>) {
                for (Object element : ((Collection<?>) propertyValue)) {
                    if (element instanceof DynamicObject) {
                        reachable.add((DynamicObject) element);
                    }
                }
            } else if (propertyValue instanceof Frame) {
                reachable.addAll(getObjectsInFrame((Frame) propertyValue));
            } else if (propertyValue instanceof ObjectGraphNode) {
                reachable.addAll(((ObjectGraphNode) propertyValue).getAdjacentObjects());
            }
        }

        return reachable;
    }

    public static Set<DynamicObject> getObjectsInFrame(Frame frame) {
        final Set<DynamicObject> objects = new HashSet<>();

        final Frame lexicalParentFrame = RubyArguments.tryGetDeclarationFrame(frame);
        if (lexicalParentFrame != null) {
            objects.addAll(getObjectsInFrame(lexicalParentFrame));
        }

        final Object self = RubyArguments.tryGetSelf(frame);
        if (self instanceof DynamicObject) {
            objects.add((DynamicObject) self);
        }

        final DynamicObject block = RubyArguments.tryGetBlock(frame);
        if (block != null) {
            objects.add(block);
        }

        // Other frame arguments are either only internal or user arguments which appear in slots.

        for (FrameSlot slot : frame.getFrameDescriptor().getSlots()) {
            final Object slotValue = frame.getValue(slot);

            if (slotValue instanceof DynamicObject) {
                objects.add((DynamicObject) slotValue);
            }
        }

        return objects;
    }

}
