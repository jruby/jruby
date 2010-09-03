package org.jruby.util.io;

import java.io.IOException;
import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.SoftReference;
import java.nio.channels.Selector;
import java.util.ArrayList;
import java.util.List;

public class SelectorPool {
    private final List<SoftReference<Selector>> pool = new ArrayList();
    private final ReferenceQueue queue = new ReferenceQueue();
    
    public Selector get() throws IOException{
        return retrieveFromPool();
    }

    public void put(Selector selector) {
        returnToPool(selector);
    }

    private synchronized Selector retrieveFromPool() throws IOException {
        // scrub pool
        clean();

        Selector selector = null;

        // try to get from pool
        while (!pool.isEmpty() && selector == null) {
            Reference<Selector> ref = pool.remove(pool.size() - 1);
            selector = ref.get();
        }

        if (selector != null) return selector;

        return Selector.open();
    }

    private synchronized void returnToPool(Selector selector) {
        clean();
        pool.add(new SoftReference<Selector>(selector, queue));
    }

    private synchronized void clean() {
        Reference ref;
        while ((ref = queue.poll()) != null) {
            pool.remove((SoftReference)ref);
        }
    }
}
