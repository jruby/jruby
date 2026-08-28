package org.jruby.util.collections;

import org.junit.Test;

import static java.lang.Runtime.getRuntime;
import static java.util.concurrent.CompletableFuture.anyOf;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

/**
 * Test for {@link HashMapInt}
 */
public class HashMapIntTest {

    @Test
    public void testContainsKey() throws Throwable {
        HashMapInt<String> map = new HashMapInt<>();
        map.put("present", 1);

        assertTrue("Should contain 'present'", map.containsKey("present"));
        assertFalse("Should not contain 'absent'", map.containsKey("absent"));

        // with identity mode
        map = new HashMapInt<>(1, true);

        String key1 = new String();
        map.put(key1, 1);

        // Generate a duplicate key concurrently
        CompletableFuture[] list = new CompletableFuture[getRuntime().availableProcessors() * 2];
        Arrays.setAll(list, i -> CompletableFuture.supplyAsync(() -> findDuplicate(key1)).orTimeout(5, SECONDS));
        String key2;
        try {
            key2 = (String) anyOf(list).get();
        } catch (ExecutionException e) {
            // could not find a duplicate key within timeout, skip test
            return;
        }

        assertTrue("Should contain key1", map.containsKey(key1));
        assertFalse("Should not contain duplicate but idempotent key2", map.containsKey(key2));
    }

    private static Object findDuplicate(Object key1) {
        while (true) {
            String key = new String();
            if (System.identityHashCode(key1) == System.identityHashCode(key)) return key;
        }
    }
}