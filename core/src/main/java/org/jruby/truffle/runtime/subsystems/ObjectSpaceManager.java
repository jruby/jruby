/*
 * Copyright (c) 2013, 2014 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.runtime.subsystems;

import java.lang.ref.*;
import java.util.*;
import java.util.concurrent.*;

import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.frame.Frame;
import com.oracle.truffle.api.frame.FrameInstance;
import com.oracle.truffle.api.frame.FrameInstanceVisitor;
import com.oracle.truffle.api.frame.FrameSlot;
import org.jruby.runtime.Visibility;
import org.jruby.truffle.nodes.RubyNode;
import org.jruby.truffle.runtime.*;
import org.jruby.truffle.runtime.core.*;
import org.jruby.truffle.runtime.util.Consumer;

/**
 * Supports the Ruby {@code ObjectSpace} module. Object IDs are lazily allocated {@code long}
 * values, mapped to objects with a weak hash map. Finalizers are implemented with weak references
 * and reference queues, and are run in a dedicated Ruby thread (but not a dedicated Java thread).
 */
public class ObjectSpaceManager {

    private class FinalizerReference extends WeakReference<RubyBasicObject> {

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
    private Thread finalizerJavaThread;
    private boolean stop;
    private CountDownLatch finished = new CountDownLatch(1);

    public ObjectSpaceManager(RubyContext context) {
        this.context = context;
    }

    public void defineFinalizer(RubyBasicObject object, RubyProc proc) {
        RubyNode.notDesignedForCompilation();

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

            finalizerThread.initialize(context, null, new Runnable() {

                @Override
                public void run() {
                    runFinalizers();
                }

            });
        }
    }

    public void undefineFinalizer(RubyBasicObject object) {
        RubyNode.notDesignedForCompilation();

        final FinalizerReference finalizerReference = finalizerReferences.get(object);

        if (finalizerReference != null) {
            finalizerReference.clearFinalizers();
        }
    }

    private void runFinalizers() {
        // Run in a loop

        while (true) {
            // Is there a finalizer ready to immediately run?

            FinalizerReference finalizerReference = (FinalizerReference) finalizerQueue.poll();

            if (finalizerReference != null) {
                runFinalizers(finalizerReference);
                continue;
            }

            // Check if we've been asked to stop

            if (stop) {
                break;
            }

            // Leave the global lock and wait on the finalizer queue

            final RubyThread runningThread = context.getThreadManager().leaveGlobalLock();
            finalizerJavaThread = Thread.currentThread();

            try {
                finalizerReference = (FinalizerReference) finalizerQueue.remove();
            } catch (InterruptedException e) {
                continue;
            } finally {
                context.getThreadManager().enterGlobalLock(runningThread);
            }

            runFinalizers(finalizerReference);
        }

        finished.countDown();
    }

    private static void runFinalizers(FinalizerReference finalizerReference) {
        try {
            for (RubyProc proc : finalizerReference.getFinalizers()) {
                proc.rootCall();
            }
        } catch (Exception e) {
            // MRI seems to silently ignore exceptions in finalizers
        }
    }

    public void shutdown() {
        RubyNode.notDesignedForCompilation();

        context.getThreadManager().enterGlobalLock(finalizerThread);

        try {
            // Tell the finalizer thread to stop and wait for it to do so

            if (finalizerThread != null) {
                stop = true;

                if (finalizerJavaThread != null) {
                    finalizerJavaThread.interrupt();
                }

                context.getThreadManager().leaveGlobalLock();

                try {
                    finished.await();
                } catch (InterruptedException e) {
                } finally {
                    context.getThreadManager().enterGlobalLock(finalizerThread);
                }
            }

            // Run any finalizers for objects that are still live

            for (FinalizerReference finalizerReference : finalizerReferences.values()) {
                runFinalizers(finalizerReference);
            }
        } finally {
            context.getThreadManager().leaveGlobalLock();
        }
    }

    public static interface ObjectGraphVisitor {

        boolean visit(RubyBasicObject object);

    }

    private Map<Long, RubyBasicObject> liveObjects;
    private ObjectGraphVisitor visitor;

    public Map<Long, RubyBasicObject> collectLiveObjects() {
        RubyNode.notDesignedForCompilation();

        // TODO(CS): probably a race condition here if multiple threads try to collect at the same time

        liveObjects = new HashMap<>();

        visitor = new ObjectGraphVisitor() {

            @Override
            public boolean visit(RubyBasicObject object) {
                return liveObjects.put(object.getObjectID(), object) == null;
            }

        };

        context.getSafepointManager().pauseAllThreadsAndExecute(new Consumer<Boolean>() {

            @Override
            public void accept(Boolean isPausingThread) {
                synchronized (liveObjects) {
                    context.getCoreLibrary().getGlobalVariablesObject().visitObjectGraph(visitor);
                    context.getCoreLibrary().getMainObject().visitObjectGraph(visitor);
                    context.getCoreLibrary().getObjectClass().visitObjectGraph(visitor);
                    visitCallStack(visitor);
                }
            }

        });

        return Collections.unmodifiableMap(liveObjects);
    }

    public void visitCallStack(final ObjectGraphVisitor visitor) {
        visitFrameInstance(Truffle.getRuntime().getCurrentFrame(), visitor);

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
            if (!(value instanceof Visibility)) { // TODO(cs): Better condition for hidden local variables
                context.getCoreLibrary().box(value).visitObjectGraph(visitor);
            }
        }
    }

}
