/*
 * Copyright (C) 2009 Wayne Meissner
 *
 * This file is part of jruby-cext.
 *
 * This code is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License version 3 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 3 for more details.
 *
 * You should have received a copy of the GNU General Public License
 * version 3 along with this work.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.jruby.cext;

import java.lang.ref.Reference;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import org.jruby.RubyBasicObject;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.threading.DaemonThreadFactory;
import org.jruby.util.ReferenceReaper;
import org.jruby.util.WeakIdentityHashMap;


public class GC {
    private static final String NATIVE_REF_KEY = "cext-ref";

    @SuppressWarnings(value="unchecked")
    private static final Map<Object, Handle> nonRubyRefs = new WeakIdentityHashMap();
    private static final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor(new DaemonThreadFactory());
    private static volatile Reference<Object> reaper = null;
    private static Runnable gcTask;
    private static volatile Future<?> gcFuture;

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
                GIL.acquire();
                try {
                    n.gc();
                    Object obj;
                    while ((obj = n.pollGC()) != null) {
                        if (obj instanceof RubyBasicObject) {
                            ((RubyBasicObject) obj).fastSetInternalVariable(NATIVE_REF_KEY, null);
                        } else {
                            nonRubyRefs.remove(obj);
                        }
                    }
                } finally {
                    GIL.releaseNoCleanup();
                }
            }
        };
    }

    static final Handle lookup(IRubyObject obj) {
        return obj instanceof RubyBasicObject
                ? (Handle) ((RubyBasicObject) obj).fastGetInternalVariable(NATIVE_REF_KEY)
                : nonRubyRefs.get(obj);
    }

    /**
     * Called from Handle.valueOf
     * @param obj
     */
    static final void register(IRubyObject obj, Handle h) {
        if (obj instanceof RubyBasicObject) {
            ((RubyBasicObject) obj).fastSetInternalVariable(NATIVE_REF_KEY, h);

        } else {
            nonRubyRefs.put(obj, h);
        }
    }

    static final void cleanup() {
        //
        // Trigger a cleanup when the VM runs out of memory and starts clearing
        // soft references.
        //
        if (reaper == null) {
            reaper = new ReferenceReaper.Soft<Object>(new Object()) {
                public void run() {
                    reaper = null;
                    GC.trigger();
                }
            };
        }
    }
}
