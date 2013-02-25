/***** BEGIN LICENSE BLOCK *****
 * Version: EPL 1.0/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Eclipse Public
 * License Version 1.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.eclipse.org/legal/epl-v10.html
 *
 * Software distributed under the License is distributed on an "AS
 * IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * rights and limitations under the License.
 *
 * Copyright (C) 2009 Wayne Meissner
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

package org.jruby.cext;

import java.lang.ref.Reference;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.jruby.RubyBasicObject;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.threading.DaemonThreadFactory;
import org.jruby.util.SoftReferenceReaper;
import org.jruby.util.WeakIdentityHashMap;
import org.jruby.util.WeakReferenceReaper;

/**
 * The cext {@link GC} keeps track of native handles and associates them with their corresponding Java objects
 * to avoid garbage-collection while either is in use. It will remove unused references when a thread exits native code
 * or the VM runs out of memory.
 */
public class GC {

    private static final Map<Object, Handle> nativeHandles = new WeakIdentityHashMap();
    private static final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor(new DaemonThreadFactory());
    private static volatile Reference<Object> reaper = null;
    private static Runnable gcTask;
    private static volatile Future<?> gcFuture;
    private static final AtomicInteger gcDisable = new AtomicInteger();

    /**
     * This is an upcall from the C++ stub to mark objects that are only strongly
     * reachable from a C VALUE instance.
     *
     * @param obj The object to mark
     */

    public static final void mark(IRubyObject obj) {
    }

    public static final void trigger() {
        if (gcFuture == null || gcFuture.isDone()) {
            gcFuture = executor.submit(gcTask);
        }
    }

    static void init(final Native n) {
        gcTask = new Runnable() {
            public void run() {
                if (gcDisable.get() == 0) {
                    GIL.acquire();
                    try {
                        n.gc();
                    } finally {
                        GIL.releaseNoCleanup();
                    }
                }
            }
        };
    }

    static final Handle lookup(IRubyObject obj) {
        return obj instanceof RubyBasicObject 
                ? (Handle) ((RubyBasicObject) obj).getNativeHandle()
                : nativeHandles.get(obj);
    }

    /**
     * Called from Handle.valueOf
     * @param obj
     */
    static final void register(IRubyObject obj, Handle h) {
        if (obj instanceof RubyBasicObject) {
            ((RubyBasicObject) obj).setNativeHandle(h);
        } else {
            nativeHandles.put(obj, h);
        }

        Cleaner.register(h);
    }

    static final void cleanup() {
        //
        // Trigger a cleanup when the VM runs out of memory and starts clearing
        // soft references.
        //
        if (reaper == null) {
            reaper = new WeakReferenceReaper<Object>(new Object()) {
                public void run() {
                    reaper = null;
                    GC.trigger();
                }
            };
        }
    }

    static void disable() {
        gcDisable.incrementAndGet();
    }

    static void enable() {
        if (gcDisable.decrementAndGet() == 0) {
            cleanup();
        }
    }
}
