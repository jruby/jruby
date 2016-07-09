package org.jruby.ir.targets;

import java.util.HashSet;
import java.util.Set;

/**
 * Created by headius on 7/8/16.
 */
public class SiteTracker {
    private final Set<Integer> seenTypes = new HashSet<Integer>();
    private volatile int clearCount = 0;

    public synchronized boolean hasSeenType(int typeCode) {
        return seenTypes.contains(typeCode);
    }

    public synchronized void addType(int typeCode) {
        seenTypes.add(typeCode);
    }

    public synchronized int seenTypesCount() {
        return seenTypes.size();
    }

    public synchronized void clearTypes() {
        seenTypes.clear();
        clearCount++;
    }

    public int clearCount() {
        return clearCount;
    }
}
