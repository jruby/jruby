package org.jruby.ir.targets;

import org.jruby.util.collections.IntHashMap;

/**
 * Created by headius on 7/8/16.
 */
public class SiteTracker {

    private final IntHashMap<?> seenTypes = new IntHashMap<>(4); // Set<int>
    private volatile int clearCount = 0;

    public synchronized boolean hasSeenType(int typeCode) {
        return seenTypes.containsKey(typeCode);
    }

    public synchronized void addType(int typeCode) {
        seenTypes.put(typeCode, null);
    }

    public synchronized int seenTypesCount() {
        return seenTypes.size();
    }

    public synchronized void clearTypes() {
        clearCount++;
        seenTypes.clear();
    }

    public int clearCount() {
        return clearCount;
    }

    @Override
    public synchronized String toString() {
        return getClass().getName() + '@' + Integer.toHexString(System.identityHashCode(this))
                + this.seenTypes + "clearCount=" + clearCount;
    }
}
