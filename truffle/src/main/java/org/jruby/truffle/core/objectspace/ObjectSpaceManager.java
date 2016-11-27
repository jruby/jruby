/*
 * Copyright (c) 2013, 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.core.objectspace;

import com.oracle.truffle.api.Assumption;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.TruffleOptions;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.utilities.CyclicAssumption;
import org.jruby.RubyGC;
import org.jruby.truffle.RubyContext;
import org.jruby.truffle.core.thread.ThreadManager;
import org.jruby.truffle.language.control.RaiseException;
import org.jruby.truffle.language.objects.ObjectIDOperations;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.atomic.AtomicLong;

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

    private final CyclicAssumption tracingAssumption = new CyclicAssumption("objspace-tracing");
    @CompilerDirectives.CompilationFinal private boolean isTracing = false;
    private int tracingAssumptionActivations = 0;
    private boolean tracingPaused = false;

    private final AtomicLong nextObjectID = new AtomicLong(ObjectIDOperations.FIRST_OBJECT_ID);

    public ObjectSpaceManager(RubyContext context) {
        this.context = context;
    }

    @CompilerDirectives.TruffleBoundary
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

            finalizerThread = ThreadManager.createRubyThread(context);
            ThreadManager.initialize(finalizerThread, context, null, "finalizer", () -> runFinalizers());
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

        if (TruffleOptions.AOT) {
            // ReferenceQueue#remove is not available with AOT.
            return;
        }

        while (true) {
            // Wait on the finalizer queue
            FinalizerReference finalizerReference = (FinalizerReference) context.getThreadManager().runUntilResult(null, () -> finalizerQueue.remove());

            runFinalizers(context, finalizerReference);
        }
    }

    private static void runFinalizers(RubyContext context, FinalizerReference finalizerReference) {
        try {
            for (Object callable : finalizerReference.getFinalizers()) {
                context.send(callable, "call", null);
            }
        } catch (RaiseException e) {
            // MRI seems to silently ignore exceptions in finalizers
        }
    }

    public List<DynamicObject> getFinalizerHandlers() {
        final List<DynamicObject> handlers = new ArrayList<>();

        for (FinalizerReference finalizer : finalizerReferences.values()) {
            for (Object handler : finalizer.getFinalizers()) {
                if (handler instanceof DynamicObject) {
                    handlers.add((DynamicObject) handler);
                }
            }
        }

        return handlers;
    }

    public void traceAllocationsStart() {
        tracingAssumptionActivations++;

        if (tracingAssumptionActivations == 1) {
            isTracing = true;
            tracingAssumption.invalidate();
        }
    }

    public void traceAllocationsStop() {
        tracingAssumptionActivations--;

        if (tracingAssumptionActivations == 0) {
            isTracing = false;
            tracingAssumption.invalidate();
        }
    }

    public void traceAllocation(DynamicObject object, DynamicObject classPath, DynamicObject methodId, DynamicObject sourcefile, int sourceline) {
        if (TruffleOptions.AOT) {
            throw new UnsupportedOperationException("Memory manager is not available with AOT.");
        }

        if (tracingPaused) {
            return;
        }

        tracingPaused = true;

        try {
            context.send(context.getCoreLibrary().getObjectSpaceModule(), "trace_allocation", null, object, classPath, methodId, sourcefile, sourceline, RubyGC.getCollectionCount());
        } finally {
            tracingPaused = false;
        }
    }

    public Assumption getTracingAssumption() {
        return tracingAssumption.getAssumption();
    }

    public boolean isTracing() {
        return isTracing;
    }

    public long getNextObjectID() {
        final long id = nextObjectID.getAndAdd(2);

        if (id < 0) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            throw new RuntimeException("Object IDs exhausted");
        }

        return id;
    }

}
