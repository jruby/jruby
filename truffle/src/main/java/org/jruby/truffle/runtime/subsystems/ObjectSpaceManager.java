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
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.control.RaiseException;
import org.jruby.truffle.runtime.core.RubyBasicObject;
import org.jruby.truffle.runtime.core.RubyProc;
import org.jruby.truffle.runtime.core.RubyThread;
import org.jruby.truffle.runtime.subsystems.ThreadManager.BlockingActionWithoutGlobalLock;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.*;

/**
 * Supports the Ruby {@code ObjectSpace} module. Object IDs are lazily allocated {@code long}
 * values, mapped to objects with a weak hash map. Finalizers are implemented with weak references
 * and reference queues, and are run in a dedicated Ruby thread.
 */
public class ObjectSpaceManager {

    private static class FinalizerReference extends WeakReference<RubyBasicObject> {

        public List<RubyProc> finalizers = new LinkedList<>();

        public FinalizerReference(RubyBasicObject object, ReferenceQueue<? super RubyBasicObject> queue) {
            super(object, queue);
        }

        public void addFinalizer(RubyProc proc) {
            finalizers.add(proc);
        }

        public List<RubyProc> getFinalizers() {
            return finalizers;
        }

        public void clearFinalizers() {
            finalizers = new LinkedList<>();
        }

    }

    private final RubyContext context;

    private final Map<RubyBasicObject, FinalizerReference> finalizerReferences = new WeakHashMap<>();
    private final ReferenceQueue<RubyBasicObject> finalizerQueue = new ReferenceQueue<>();
    private RubyThread finalizerThread;

    public ObjectSpaceManager(RubyContext context) {
        this.context = context;
    }

    public synchronized void defineFinalizer(RubyBasicObject object, RubyProc proc) {
        // Record the finalizer against the object

        FinalizerReference finalizerReference = finalizerReferences.get(object);

        if (finalizerReference == null) {
            finalizerReference = new FinalizerReference(object, finalizerQueue);
            finalizerReferences.put(object, finalizerReference);
        }

        finalizerReference.addFinalizer(proc);

        // If there is no finalizer thread, start one

        if (finalizerThread == null) {
            // TODO(CS): should we be running this in a real Ruby thread?

            finalizerThread = new RubyThread(context.getCoreLibrary().getThreadClass(), context.getThreadManager());
            finalizerThread.initialize(context, null, "finalizer", new Runnable() {
                @Override
                public void run() {
                    runFinalizers();
                }
            });
        }
    }

    public synchronized void undefineFinalizer(RubyBasicObject object) {
        final FinalizerReference finalizerReference = finalizerReferences.get(object);

        if (finalizerReference != null) {
            finalizerReference.clearFinalizers();
        }
    }

    private void runFinalizers() {
        // Run in a loop

        while (true) {
            // Leave the global lock and wait on the finalizer queue

            FinalizerReference finalizerReference = context.getThreadManager().runUntilResult(new BlockingActionWithoutGlobalLock<FinalizerReference>() {
                @Override
                public FinalizerReference block() throws InterruptedException {
                    return (FinalizerReference) finalizerQueue.remove();
                }
            });

            runFinalizers(finalizerReference);
        }
    }

    private static void runFinalizers(FinalizerReference finalizerReference) {
        try {
            for (RubyProc proc : finalizerReference.getFinalizers()) {
                proc.rootCall();
            }
        } catch (RaiseException e) {
            // MRI seems to silently ignore exceptions in finalizers
        }
    }

    public static interface ObjectGraphVisitor {

        boolean visit(RubyBasicObject object);

    }

    @TruffleBoundary
    public Map<Long, RubyBasicObject> collectLiveObjects() {
        final Map<Long, RubyBasicObject> liveObjects = new HashMap<>();

        final ObjectGraphVisitor visitor = new ObjectGraphVisitor() {

            @Override
            public boolean visit(RubyBasicObject object) {
                return liveObjects.put(object.verySlowGetObjectID(), object) == null;
            }

        };

        context.getSafepointManager().pauseAllThreadsAndExecute(null, false, new SafepointAction() {

            @Override
            public void run(RubyThread currentThread, Node currentNode) {
                synchronized (liveObjects) {
                    currentThread.visitObjectGraph(visitor);
                    context.getCoreLibrary().getGlobalVariablesObject().visitObjectGraph(visitor);

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
            if (value instanceof RubyBasicObject) {
                ((RubyBasicObject) value).visitObjectGraph(visitor);
            }
        }
    }

}
