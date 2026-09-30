package org.jruby.util.collections;

import org.junit.Test;

import java.util.Map;

import static org.junit.Assert.assertSame;

/**
 * Test for {@link WeakValuedMap}
 */
public class WeakValuedMapTest {

    // keeps the backing map so the test can enqueue a stored reference itself, standing in for
    // the collector clearing it after a replacement was put
    static class CapturingWeakValuedMap extends WeakValuedMap<Integer, Object> {
        Map<Integer, WeakValuedMap.KeyedReference<Integer, Object>> backing;

        @Override
        protected Map<Integer, WeakValuedMap.KeyedReference<Integer, Object>> newMap() {
            return backing = super.newMap();
        }
    }

    @Test
    public void testReplacedValueSurvivesItsPredecessorDying() {
        CapturingWeakValuedMap map = new CapturingWeakValuedMap();
        Object first = new Object();
        Object second = new Object();

        map.put(1, first);
        WeakValuedMap.KeyedReference<Integer, Object> firstRef = map.backing.get(1);
        map.put(1, second);
        firstRef.enqueue();

        assertSame("the value put after its predecessor died must stay", second, map.get(1));
    }
}
