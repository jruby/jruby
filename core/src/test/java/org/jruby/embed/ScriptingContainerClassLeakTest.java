package org.jruby.embed;

import org.jruby.Ruby;
import org.junit.Test;

import java.lang.management.ManagementFactory;
import java.lang.ref.WeakReference;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Regression test for https://github.com/jruby/jruby/issues/9092
 *
 * Leak mechanism:
 *   Thread (GC root) → ThreadLocalMap → Entry.value = SoftRef&lt;ThreadContext&gt;
 *                                                         → ThreadContext → Ruby → JRubyClassLoader → ~98 classes
 *
 * ThreadService extends ThreadLocal and stores a SoftReference&lt;ThreadContext&gt; per thread.
 * SoftReferences are only cleared by the JVM under heap pressure, so in a well-resourced
 * server System.gc() will NOT collect them.  After the fix, teardown() explicitly calls
 * SoftReference.clear() on every tracked ref, making the Ruby runtime weakly reachable
 * and therefore collected by a normal System.gc() call.
 */
public class ScriptingContainerClassLeakTest {

    /**
     * Primary regression test for GH-9092.
     *
     * After the container is terminated (engine disposed), the Ruby runtime must
     * be eligible for collection by a normal GC — not just when the JVM is under
     * memory pressure.
     *
     * Without the fix: the calling thread's ThreadLocalMap retains
     *   Entry.value = SoftRef&lt;ThreadContext&gt; → ThreadContext → Ruby
     * so Ruby remains <em>softly reachable</em> and survives System.gc().
     *
     * With the fix: teardown() calls SoftReference.clear() on every tracked ref,
     * breaking the chain; Ruby becomes only weakly reachable and is collected.
     */
    @Test
    public void runtimeIsGCdAfterTeardown() throws InterruptedException {
        // Create engine and dispose inside a helper method so that the
        // ScriptingContainer is guaranteed out-of-scope when we check GC.
        WeakReference<Ruby> ref = createEvalAndClose();
        forceGC();
        assertNull(
            "Ruby runtime was not collected after terminate() (GH-9092). " +
            "The calling thread's ThreadLocalMap is holding a SoftReference to " +
            "the runtime's ThreadContext, keeping it alive across GC cycles.",
            ref.get()
        );
    }

    @Test
    public void runtimeIsGCdAfterWorkerThreadTeardown() throws InterruptedException {
        AtomicReference<WeakReference<Ruby>> refHolder = new AtomicReference<>();

        Thread thread = new Thread(() -> {
            ScriptingContainer sc = new ScriptingContainer(LocalContextScope.SINGLETHREAD);
            Ruby runtime = sc.getProvider().getRuntime();
            refHolder.set(new WeakReference<>(runtime));
            sc.terminate();
            Ruby.clearGlobalRuntime();
        });

        thread.start();
        thread.join();

        WeakReference<Ruby> ref = refHolder.get();
        forceGC();

        assertNull(
            "Ruby runtime created and terminated on a worker thread was not collected after terminate() (GH-9092).",
            ref.get()
        );
    }

    /**
     * Create a container, eval something (so the calling thread's ThreadContext
     * is registered via ThreadService.set()), then dispose it.  Returning only a
     * WeakReference ensures the container itself is out-of-scope at the call site.
     */
    private static WeakReference<Ruby> createEvalAndClose() {
        ScriptingContainer sc = new ScriptingContainer(LocalContextScope.SINGLETHREAD);
        sc.runScriptlet("nil");   // triggers ThreadService.set() → SoftRef tracked
        WeakReference<Ruby> ref = new WeakReference<>(sc.getProvider().getRuntime());
        sc.terminate();
        Ruby.clearGlobalRuntime();
        return ref;
        // sc and all strong references it owns go out of scope here
    }

    /**
     * Secondary check: loaded-class count must not grow linearly across cycles.
     * This catches any classloader leak regardless of the specific mechanism.
     */
    @Test
    public void classCountDoesNotGrowUnboundedly() throws InterruptedException {
        // Warmup
        for (int i = 0; i < 3; i++) createAndTearDown();
        forceGC();

        long classesBefore = ManagementFactory.getClassLoadingMXBean().getLoadedClassCount();

        for (int i = 0; i < 10; i++) createAndTearDown();
        forceGC();

        long classesAfter = ManagementFactory.getClassLoadingMXBean().getLoadedClassCount();
        long growth = classesAfter - classesBefore;

        // Generous constant budget (lazy JDK internals etc.), but not per-cycle growth.
        // Pre-fix behaviour was ~98 new classes per cycle * 10 = ~980.
        assertTrue(
            "Loaded class count grew by " + growth + " across 10 ScriptingContainer " +
            "cycles (max allowed: 200). Classloader leak detected (see GH-9092).",
            growth <= 200
        );
    }

    // -------------------------------------------------------------------------

    private static void createAndTearDown() {
        ScriptingContainer sc = new ScriptingContainer(LocalContextScope.SINGLETHREAD);
        try {
            sc.runScriptlet("1 + 1");
        } finally {
            sc.terminate();
            Ruby.clearGlobalRuntime();
        }
    }

    @SuppressWarnings({"deprecation", "removal"})
    private static void forceGC() throws InterruptedException {
        // ScriptingContainer has finalize(), so two GC passes are needed:
        // 1st: detects sc is unreachable → enqueues for finalization
        // System.runFinalization(): runs sc.finalize() → sc now truly collectable
        // 2nd: collects sc + LocalContext → Ruby loses its strong-ref path
        // 3rd+: Ruby (with fix: weakly reachable) is collected
        for (int i = 0; i < 5; i++) {
            System.gc();
            System.runFinalization();
            Thread.sleep(100);
        }
    }
}
