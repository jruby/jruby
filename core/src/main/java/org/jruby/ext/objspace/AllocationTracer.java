package org.jruby.ext.objspace;

import org.jruby.Ruby;
import org.jruby.RubyGC;
import org.jruby.anno.CRuby;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.backtrace.RubyStackTraceElement;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.WeakIdentityHashMap;

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.ref.SoftReference;
import java.util.List;
import java.util.Map;

/**
 * Where objects were allocated while ObjectSpace.trace_object_allocations was on. Ruby#startAllocationTracing
 * passes every object allocated with the runtime to {@link #record} until tracing stops.
 */
public class AllocationTracer {
    record Info(String path, int line, long generation) {}

    private final Map<IRubyObject, Info> allocations = new WeakIdentityHashMap();
    private final ThreadLocal<Boolean> recording = ThreadLocal.withInitial(() -> false);
    // The JVM may add or remove collectors while it runs; the list is read again for each tracing session
    private volatile List<GarbageCollectorMXBean> collectors;

    /**
     * Called by Ruby#startAllocationTracing when a tracing session starts.
     */
    public void startSession() {
        try {
            collectors = ManagementFactory.getGarbageCollectorMXBeans();
        } catch (Throwable t) {
            collectors = null;
        }
    }

    @CRuby(value = "newobj_i", file = "ext/objspace/object_tracing.c",
            note = "records path, line and generation only; Integers in long range and Floats never get here")
    public void record(Ruby runtime, IRubyObject object) {
        // a Java thread that is not running Ruby code is not adopted just to record its allocations
        SoftReference<ThreadContext> ref = runtime.getThreadService().get();
        ThreadContext context = ref == null ? null : ref.get();
        if (context == null) return;

        // walking the stack can allocate
        if (recording.get()) return;
        recording.set(true);
        try {
            // without a Ruby caller CRuby still records the object, with a nil path and line 0
            RubyStackTraceElement caller = context.getCallerElement();
            Info info = caller == null ?
                    new Info(null, 0, collectionCount()) :
                    new Info(caller.getFileName(), caller.getLineNumber(), collectionCount());
            synchronized (allocations) {
                allocations.put(object, info);
            }
        } finally {
            recording.set(false);
        }
    }

    // GC.count, -1 when the collectors cannot be read
    private long collectionCount() {
        List<GarbageCollectorMXBean> collectors = this.collectors;
        if (collectors == null) return -1;
        try {
            return RubyGC.getCollectionCount(collectors);
        } catch (Throwable t) {
            return -1;
        }
    }

    @CRuby(value = "lookup_allocation_info", file = "ext/objspace/object_tracing.c")
    Info get(IRubyObject object) {
        synchronized (allocations) {
            return allocations.get(object);
        }
    }

    void clear() {
        synchronized (allocations) {
            allocations.clear();
        }
    }
}
