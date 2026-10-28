package org.jruby.util.collections;

import org.junit.Test;

import java.lang.ref.WeakReference;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.Adler32;
import java.util.zip.CRC32;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

public class StableClassValueTest {

    /**
     * The computed values are only referenced from the classes they were computed for, and must not be collected and
     * computed again.
     */
    @Test
    public void keepsValuesNothingElseReferences() throws InterruptedException {
        var computed = new AtomicInteger();
        var classValue = new StableClassValue<>(cls -> {
            computed.incrementAndGet();
            return new Object();
        });

        var value = new WeakReference<>(classValue.get(Adler32.class));
        var garbage = new WeakReference<>(new Object());
        forceGC(garbage);

        assertNull("GC did not run", garbage.get());
        assertNotNull("value was collected", value.get());
        assertSame(value.get(), classValue.get(Adler32.class));
        assertEquals(1, computed.get());
    }

    /**
     * A JDK class is never unloaded, so the value it holds for a StableClassValue must not reference the class loader
     * that loaded StableClassValue once clear() has been called. Here that class loader is a disposable one, like the
     * class loader of an embedded JRuby.
     */
    @Test
    public void clearReleasesTheClassLoader() throws Exception {
        WeakReference<ClassLoader> loader = computeAndClearInDisposableLoader();
        forceGC(loader);

        assertNull("class loader was not collected after clear()", loader.get());
    }

    private static WeakReference<ClassLoader> computeAndClearInDisposableLoader() throws Exception {
        URL[] classpath = {
            StableClassValue.class.getProtectionDomain().getCodeSource().getLocation(),
            NewObject.class.getProtectionDomain().getCodeSource().getLocation()
        };
        try (var loader = new URLClassLoader(classpath, null)) {
            Class<?> calculatorType = loader.loadClass(ClassValueCalculator.class.getName());
            Object calculator = loader.loadClass(NewObject.class.getName()).getDeclaredConstructor().newInstance();

            Class<?> stableClassValue = loader.loadClass(StableClassValue.class.getName());
            Constructor<?> constructor = stableClassValue.getConstructor(calculatorType);
            Method get = stableClassValue.getMethod("get", Class.class);
            Method clear = stableClassValue.getMethod("clear");
            constructor.setAccessible(true);
            get.setAccessible(true);
            clear.setAccessible(true);

            Object classValue = constructor.newInstance(calculator);
            assertNotNull(get.invoke(classValue, CRC32.class));
            clear.invoke(classValue);

            return new WeakReference<>(loader);
        }
    }

    public static class NewObject implements ClassValueCalculator<Object> {
        @Override
        public Object computeValue(Class<?> cls) {
            return new Object();
        }
    }

    private static void forceGC(WeakReference<?> ref) throws InterruptedException {
        for (int i = 0; i < 10 && ref.get() != null; i++) {
            System.gc();
            Thread.sleep(100);
        }
    }
}
