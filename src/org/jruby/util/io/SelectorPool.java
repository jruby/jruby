/*
 ***** BEGIN LICENSE BLOCK *****
 * Version: CPL 1.0/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Common Public
 * License Version 1.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.eclipse.org/legal/cpl-v10.html
 *
 * Software distributed under the License is distributed on an "AS
 * IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * rights and limitations under the License.
 *
 * Copyright (C) 2010 Charles O Nutter <headius@headius.com>
 *
 * Alternatively, the contents of this file may be used under the terms of
 * either of the GNU General Public License Version 2 or later (the "GPL"),
 * or the GNU Lesser General Public License Version 2.1 or later (the "LGPL"),
 * in which case the provisions of the GPL or the LGPL are applicable instead
 * of those above. If you wish to allow use of your version of this file only
 * under the terms of either the GPL or the LGPL, and not to allow others to
 * use your version of this file under the terms of the CPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the CPL, the GPL or the LGPL.
 ***** END LICENSE BLOCK *****/

package org.jruby.util.io;

import java.io.IOException;
import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.SoftReference;
import java.nio.channels.Selector;
import java.util.ArrayList;
import java.util.List;

import java.nio.channels.spi.SelectorProvider;

/**
 * This is a simple implementation of a soft-referenced java.nio.channels.Selector
 * pool. It is intended to allow us to reuse a small pool of selectors rather
 * than creating them new for each use (which causes problem for Windows and
 * its socket-per-selector impl) or saving them per-thread (which causes
 * problems when there are many not-quite-dead threads in flight.
 *
 * The selectors are kept in soft references, so that if there's memory
 * pressure and they are not in use, they'll get dereferenced and eventually
 * close and finalize. Weak references would be too transient, and there's no
 * reason to keep them open in hard references forever.
 *
 * @author headius
 */
public class SelectorPool {
    private final List<SoftReference<Selector>> pool = new ArrayList();
    private final ReferenceQueue queue = new ReferenceQueue();

    /**
     * Get a selector from the pool (or create a new one). Selectors come from
     * the default selector provider on the current JVM.
     *
     * @return a java.nio.channels.Selector
     * @throws IOException if there's a problem opening a new selector
     */
    public synchronized Selector get() throws IOException{
        return retrieveFromPool();
    }

    /**
     * Put a selector back into the pool.
     *
     * @param selector the selector to put back
     */
    public synchronized void put(Selector selector) {
        returnToPool(selector);
    }

    private Selector retrieveFromPool() throws IOException {
        // scrub pool
        clean();

        Selector selector = null;

        // try to get from pool
        while (!pool.isEmpty() && selector == null) {
            Reference<Selector> ref = pool.remove(pool.size() - 1);
            selector = ref.get();
        }

        if (selector != null) return selector;

        return SelectorFactory.openWithRetryFrom(null, SelectorProvider.provider());
    }

    private void returnToPool(Selector selector) {
        clean();
        pool.add(new SoftReference<Selector>(selector, queue));
    }

    private void clean() {
        Reference ref;
        while ((ref = queue.poll()) != null) {
            pool.remove((SoftReference)ref);
        }
    }
}
