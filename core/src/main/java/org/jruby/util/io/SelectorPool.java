/*
 ***** BEGIN LICENSE BLOCK *****
 * Version: EPL 2.0/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Eclipse Public
 * License Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.eclipse.org/legal/epl-v20.html
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
 * use your version of this file under the terms of the EPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the EPL, the GPL or the LGPL.
 ***** END LICENSE BLOCK *****/

package org.jruby.util.io;

import java.io.IOException;
import java.nio.channels.ClosedSelectorException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;

import java.nio.channels.spi.SelectorProvider;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * This is a simple implementation of a hard-referenced java.nio.channels.Selector
 * pool. It is intended to allow us to reuse a small pool of selectors rather
 * than creating them new for each use (which causes problem for Windows and
 * its socket-per-selector impl) or saving them per-thread (which causes
 * problems when there are many not-quite-dead threads in flight.
 *
 * The selectors are kept open in the pool and you should call {@link #cleanup()}
 * for releasing selectors.
 *
 * @author headius
 */
public class SelectorPool {
    private final Map<SelectorProvider, Queue<Selector>> pool = new ConcurrentHashMap<>();

    /**
     * Get a selector from the pool (or create a new one). Selectors come from
     * the default selector provider on the current JVM.
     *
     * @return a java.nio.channels.Selector
     * @throws IOException if there's a problem opening a new selector
     */
    public Selector get() throws IOException{
        return retrieveFromPool(SelectorProvider.provider());
    }

    /**
     * Get a selector from the pool (or create a new one). Selectors come from
     * the given provider.
     *
     * @return a java.nio.channels.Selector
     * @throws IOException if there's a problem opening a new selector
     */
    public Selector get(SelectorProvider provider) throws IOException{
        return retrieveFromPool(provider);
    }

    /**
     * Put a selector back into the pool.
     *
     * @param selector the selector to put back
     */
    public void put(Selector selector) {
        try {
            for (SelectionKey key : selector.keys()) {
                if (key != null) {
                    key.cancel();
                }
            }

            try {
                selector.selectNow();
            } catch (Exception e) {
                //ignore
            }

            returnToPool(selector);
        } catch (ClosedSelectorException cse) {
            // ignore; this one is not usable and we'll just create a new one the next time
        }
    }
    
    /**
     * Clean up a pool.
     * 
     * All selectors in a pool are closed and the pool gets empty.
     * 
     */
    public void cleanup() {
        pool.forEach(SelectorPool::clearProviderPool);
        pool.clear();
    }

    private static void clearProviderPool(SelectorProvider provider, Queue<Selector> providerPool) {
        while (!providerPool.isEmpty()) {
            Selector selector;
            try {
                selector = providerPool.remove();
            } catch (NoSuchElementException nsme) {
                // empty, done
                break;
            }

            try {
                selector.close();
            } catch (IOException ioe) {
                // ignore IOException at termination.
            }
        }
    }

    private Selector retrieveFromPool(SelectorProvider provider) throws IOException {
        Queue<Selector> providerPool = pool.get(provider);
        if (providerPool != null && !providerPool.isEmpty()) {
            try {
                return providerPool.remove();
            } catch (NoSuchElementException nsme) {
                // someone drained it before us, just fall back on creating a new one below
            }
        }

        // otherwise just return a new one
        return SelectorFactory.openWithRetryFrom(null, provider);
    }

    private void returnToPool(Selector selector) {
        if (selector.isOpen()) {
            SelectorProvider provider = selector.provider();
            Queue<Selector> providerPool = pool.computeIfAbsent(provider, p -> new ConcurrentLinkedQueue<>());
            providerPool.add(selector);
        }
    }
}
