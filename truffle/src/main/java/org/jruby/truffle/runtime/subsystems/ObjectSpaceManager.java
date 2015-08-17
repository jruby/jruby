/*
 * Copyright (c) 2013, 2015 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.runtime.subsystems;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.frame.Frame;
import com.oracle.truffle.api.frame.FrameInstance;
import com.oracle.truffle.api.frame.FrameInstanceVisitor;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.object.DynamicObject;
import org.jruby.truffle.nodes.core.BasicObjectNodes;
import org.jruby.truffle.nodes.core.ThreadNodes;
import org.jruby.truffle.runtime.DebugOperations;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.control.RaiseException;
import org.jruby.truffle.runtime.subsystems.ThreadManager.BlockingAction;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.*;

/**
 * Supports the Ruby {@code ObjectSpace} module. Object IDs are lazily allocated {@code long}
 * values, mapped to objects with a weak hash map. Finalizers are implemented with weak references
 * and reference queues, and are run in a dedicated Ruby thread.
 */
public class ObjectSpaceManager {

    private static class FinalizerReference extends WeakReference<DynamicObject> {

        public List<Object> finalizers = new LinkedList<>();

        public FinalizerReference(DynamicObject object, ReferenceQueue<? super DynamicObject> queue) {
            super(object, queue);
        }

        public void addFinalizer(Object callable) {
            finalizers.add(callable);
        }

        public List<Object> getFinalizers() {
            return finalizers;
        }

        public void clearFinalizers() {
            finalizers = new LinkedList<>();
        }

    }

    private final RubyContext context;

    private final Map<DynamicObject, FinalizerReference> finalizerReferences = new WeakHashMap<>();
    private final ReferenceQueue<DynamicObject> finalizerQueue = new ReferenceQueue<>();
    private DynamicObject finalizerThread;

    public ObjectSpaceManager(RubyContext context) {
        this.context = context;
    }

    public synchronized void defineFinalizer(DynamicObject object, Object callable) {
        // Record the finalizer against the object

        FinalizerReference finalizerReference = finalizerReferences.get(object);

        if (finalizerReference == null) {
            finalizerReference = new FinalizerReference(object, finalizerQueue);
            finalizerReferences.put(object, finalizerReference);
        }

        finalizerReference.addFinalizer(callable);

        // If there is no finalizer thread, start one

        if (finalizerThread == null) {
            // TODO(CS): should we be running this in a real Ruby thread?

            finalizerThread = ThreadNodes.createRubyThread(context.getCoreLibrary().getThreadClass(), context.getThreadManager());
            ThreadNodes.initialize(finalizerThread, context, null, "finalizer", new Runnable() {
                @Override
                public void run() {
                    runFinalizers();
                }
            });
        }
    }

    public synchronized void undefineFinalizer(DynamicObject object) {
        final FinalizerReference finalizerReference = finalizerReferences.get(object);

        if (finalizerReference != null) {
            finalizerReference.clearFinalizers();
        }
    }

    private void runFinalizers() {
        // Run in a loop

        while (true) {
            // Wait on the finalizer queue
            FinalizerReference finalizerReference = context.getThreadManager().runUntilResult(new BlockingAction<FinalizerReference>() {
                @Override
                public FinalizerReference block() throws InterruptedException {
                    return (FinalizerReference) finalizerQueue.remove();
                }
            });

            runFinalizers(context, finalizerReference);
        }
    }

    private static void runFinalizers(RubyContext context, FinalizerReference finalizerReference) {
        try {
            for (Object callable : finalizerReference.getFinalizers()) {
                DebugOperations.send(context, callable, "call", null);
            }
        } catch (RaiseException e) {
            // MRI seems to silently ignore exceptions in finalizers
        }
    }

    public static interface ObjectGraphVisitor {

        boolean visit(DynamicObject object);

    }

    @TruffleBoundary
    public Map<Long, DynamicObject> collectLiveObjects() {
        final Map<Long, DynamicObject> liveObjects = new HashMap<>();

        final ObjectGraphVisitor visitor = new ObjectGraphVisitor() {

            @Override
            public boolean visit(DynamicObject object) {
                return liveObjects.put(BasicObjectNodes.verySlowGetObjectID(object), object) == null;
            }

        };

        context.getSafepointManager().pauseAllThreadsAndExecute(null, false, new SafepointAction() {

            @Override
            public void run(DynamicObject currentThread, Node currentNode) {
                synchronized (liveObjects) {
                    BasicObjectNodes.visitObjectGraph(currentThread, visitor);
                    BasicObjectNodes.visitObjectGraph(context.getCoreLibrary().getGlobalVariablesObject(), visitor);

                    // Needs to be called from the corresponding Java thread or it will not use the correct call stack.
                    visitCallStack(visitor);
                }
            }

        });

        return Collections.unmodifiableMap(liveObjects);
    }

    private void visitCallStack(final ObjectGraphVisitor visitor) {
        FrameInstance currentFrame = Truffle.getRuntime().getCurrentFrame();
        if (currentFrame != null) {
            visitFrameInstance(currentFrame, visitor);
        }

        Truffle.getRuntime().iterateFrames(new FrameInstanceVisitor<Object>() {
            @Override
            public Void visitFrame(FrameInstance frameInstance) {
                visitFrameInstance(frameInstance, visitor);
                return null;
            }
        });
    }

    public void visitFrameInstance(FrameInstance frameInstance, ObjectGraphVisitor visitor) {
        visitFrame(frameInstance.getFrame(FrameInstance.FrameAccess.READ_ONLY, true), visitor);
    }

    public void visitFrame(Frame frame, ObjectGraphVisitor visitor) {
        if (frame == null) {
            return;
        }

        for (FrameSlot slot : frame.getFrameDescriptor().getSlots()) {
            Object value = frame.getValue(slot);
            if (value instanceof DynamicObject) {
                BasicObjectNodes.visitObjectGraph(((DynamicObject) value), visitor);
            }
        }
    }

}
